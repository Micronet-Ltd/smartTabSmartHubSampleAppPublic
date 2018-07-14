package com.micronet_inc.smarthubsampleapp;

import static java.lang.Thread.sleep;

import android.os.SystemClock;
import android.util.Log;
import com.micronet.canbus.CanbusException;
import com.micronet.canbus.CanbusFlowControl;
import com.micronet.canbus.CanbusFramePort1;
import com.micronet.canbus.CanbusFramePort2;
import com.micronet.canbus.CanbusFrameType;
import com.micronet.canbus.CanbusHardwareFilter;
import com.micronet.canbus.CanbusInterface;
import com.micronet.canbus.CanbusSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class CanTest {

    private static CanTest instance = null;
    private final static String TAG = "CanTest";

    private CanbusInterface canBusInterface1;
    private CanbusInterface canBusInterface2;
    private CanbusSocket canBusSocket1;
    private CanbusSocket canBusSocket2;
    private CanbusHardwareFilter[] canBusFilter;
    private CanbusFlowControl[] canBusFlowControls;

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final String STD = "STD";
    private static final String EXT = "EXT";
    private static final String STD_R = "STD_R";
    private static final String EXT_R = "EXT_R";

    public final StringBuilder can1Data = new StringBuilder(1000);
    public final StringBuilder can2Data = new StringBuilder(1000);

    private final int pgn_mask = 0b000_0_0_11111111_11111111_00000000;

    private int canMessageIdPort1;
    private byte[] canMessageDataPort1;
    private CanbusFrameType canMessageTypePort1;

    private int canMessageIdPort2;
    private byte[] canMessageDataPort2;
    private CanbusFrameType canMessageTypePort2;

    private int j1939IntervalDelay = 500; // ms

    private Thread j1939Port1ReaderThread = null;
    private Thread j1939Port1WriterThread = null;

    private Thread j1939Port2ReaderThread = null;
    private Thread j1939Port2WriterThread = null;

    private J1939Port1Reader j1939Port1Reader = null;
    private J1939Port2Reader j1939Port2Reader = null;
    private J1939Port1Writer j1939Port1Writer = null;
    private J1939Port2Writer j1939Port2Writer = null;

    //J1939 Parameter Group Numbers
    private static final int J1939_ENGINE_CONTROLLER2 = 0x00F003;
    private static final int J1939_ENGINE_CONTROLLER1 = 0x00F004;
    private static final int J1939_PGN_GEAR = 0x00F005; // ECM2
    private static final int J1939_PGN_ODOMETER_LOW = 0x00FEE0;
    private static final int J1939_PGN_ODOMETER_HIGH = 0x00FEC1;
    private static final int J1939_PGN_ENGINE_HOURS_REVOLUTIONS = 0x00FEE5;
    private static final int J1939_PGN_FUEL_CONSUMPTION = 0x00FEE9;
    private static final int J1939_PGN_VIN_NUMBER = 0x00FEEC;
    private static final int J1939_ENGINE_TEMPERATURE_1 = 0x00FEEE;
    private static final int J1939_PGN_PARKING = 0x00FEF1;
    private static final int J1939_FUEL_ECONOMY = 0x00FEF2;
    private static final int J1939_PGN_DASH_DISP = 0x00FEFC;


    private volatile boolean blockOnReadPort1 = false;
    private final int READ_TIMEOUT = 500; // readPort1 timeout (in milliseconds)

    private int baudRate;
    private boolean removeCan1;
    private boolean removeCan2;
    private boolean silentMode;
    private int portNumber;
    private boolean termination;
    private volatile boolean autoSendJ1939Port1;
    private volatile boolean autoSendJ1939Port2;
    private boolean enableFilters = false;
    private boolean enableFlowControl = false;
    private boolean isCan1InterfaceOpen = false;
    private boolean isCan2InterfaceOpen = false;
    private boolean discardInBuffer;

    private CanTest() {
        //Mandatory constructor
    }

    // Lazy Initialization (If required then only)
    public static CanTest getInstance() {
        if (instance == null) {
            // Thread Safe. Might be costly operation in some case
            synchronized (CanTest.class) {
                if (instance == null) {
                    instance = new CanTest();
                }
            }
        }
        return instance;
    }

    public boolean isDiscardInBuffer() {
        return discardInBuffer;
    }

    public void setDiscardInBuffer(boolean discardInBuffer) {
        this.discardInBuffer = discardInBuffer;
    }

    public boolean isPort1SocketOpen() {
        // there's actually no api call to check status of canBus socket but
        // this app will open the socket as soon as object is initialized.
        // also socket doesn't actually close1939Port1 even with call to QBridgeCanbusSocket.close1939Port1()
        return canBusSocket1 != null;
    }

    public boolean isPort2SocketOpen() {
        // there's actually no api call to check status of canbus socket but
        // this app will open the socket as soon as object is initialized.
        // also socket doesn't actually close1939Port1 even with call to QBridgeCanbusSocket.close1939Port1()
        return canBusSocket2 != null;
    }

    public boolean isCan1InterfaceOpen() {
        return isCan1InterfaceOpen;
    }

    public boolean isCan2InterfaceOpen() {
        return isCan2InterfaceOpen;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public boolean isSilentChecked() {
        return silentMode;
    }

    public boolean getRemoveCan1InterfaceState() {
        return removeCan1;
    }

    public boolean getRemoveCan2InterfaceState() {
        return removeCan2;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public void setSilentMode(boolean isSilent) {
        this.silentMode = isSilent;
    }

    public void setRemoveCan1InterfaceState(boolean removeCan1) {
        this.removeCan1 = removeCan1;
    }

    public void setRemoveCan2InterfaceState(boolean removeCan2) {
        this.removeCan2 = removeCan2;
    }

    public boolean getTermination() {
        return termination;
    }

    public void setTermination(boolean term) {
         termination = term ;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int port) {this.portNumber = port;}

    public int CreateCanInterface1(boolean silentMode, int baudRate, boolean termination, int port, boolean enableFilters, boolean enableFlowControl) {
        this.silentMode = silentMode;
        this.baudRate = baudRate;
        this.termination=termination;
        this.portNumber=port;
		this.enableFilters = enableFilters;
        this.enableFlowControl = enableFlowControl;

        if (canBusInterface1 == null) {
            canBusInterface1 = new CanbusInterface();
            canBusFilter = setFilters();
            canBusFlowControls = setFlowControlMessages();
            try {
                canBusInterface1.create(silentMode, baudRate, termination, canBusFilter, 2, canBusFlowControls);
                //canBusInterface1.create(silentMode,baudRate,termination,canBusFilter,2);
            } catch (CanbusException e) {
                Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode());
                e.printStackTrace();
                return -1;
            }
        }

        if (canBusSocket1 == null) {
            canBusSocket1 = canBusInterface1.createSocketCAN1();
            canBusSocket1.openCan1();
        }
        if (discardInBuffer) {
            canBusSocket1.discardInBuffer();
        }
        isCan1InterfaceOpen = true;
        startPort1Threads();
        return 0;
    }

    public int CreateCanInterface2(boolean silentMode, int baudRate, boolean termination, int port, boolean enableFilters, boolean enableFlowControl) {
        this.silentMode = silentMode;
        this.baudRate = baudRate;
        this.termination=termination;
        this.portNumber=port;
		this.enableFilters = enableFilters;
        this.enableFlowControl = enableFlowControl;

        if (canBusInterface2 == null  ) {
            canBusInterface2 = new CanbusInterface();
            canBusFilter =setFilters();
            canBusFlowControls =setFlowControlMessages();
            try {
                canBusInterface2.create(silentMode,baudRate,termination, canBusFilter,3, canBusFlowControls);
            } catch (CanbusException e) {
                Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode() );
                e.printStackTrace();
                return -1;
            }
        }
        if (canBusSocket2 == null) {
            canBusSocket2 = canBusInterface2.createSocketCAN2();
            canBusSocket2.openCan2();
        }
        if (discardInBuffer) {
            canBusSocket2.discardInBuffer();
        }
        isCan2InterfaceOpen = true;
        startPort2Threads();
        return 0;
    }

    public void silentMode(boolean silentMode) {
        this.silentMode = silentMode;
    }
    public void setFiltersEnabled(boolean enableFilters){
        this.enableFilters = enableFilters;
    }

    private CanbusHardwareFilter[] setFilters() {

        ArrayList<CanbusHardwareFilter> filterList = new ArrayList<>();
        CanbusHardwareFilter[] filters;
        int[] ids;
        int[] mask;
        int[] type;

        if (enableFilters) {
            // Up to 24 filters

            ids = new int[]{0x18FEE000, 0x1CECFF00, 0x1CEBFF00, 0x18FEE500, 0x18FEF100,
                J1939_ENGINE_CONTROLLER2 << 8, J1939_ENGINE_CONTROLLER1 << 8,
                J1939_PGN_DASH_DISP << 8, J1939_PGN_GEAR << 8, J1939_ENGINE_TEMPERATURE_1 << 8,
                J1939_FUEL_ECONOMY << 8};
            mask = new int[]{0x1FFFFFFF, 0x1FFF00FF, 0x1FFF00FF, 0x1FFFFFFF, 0x1FFFFFFF, 0x00FFFF00,
                0x00FFFF00, 0x00FFFF00, 0x00FFFF00, 0x00FFFF00, 0x00FFFF00};
            type = new int[]{CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,


                CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,
                CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,
                CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,
                CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,
                CanbusHardwareFilter.EXTENDED};
        }
        else{
            // Setting up two filters: one standard and one extended to allow all messages.
            ids = new int[]{0x00000000, 0x00000000};
            mask = new int[]{0x00000000, 0x00000000};
            type = new int[]{CanbusHardwareFilter.STANDARD, CanbusHardwareFilter.EXTENDED};
        }
        filterList.add(new CanbusHardwareFilter(ids,mask, type));
        filters = filterList.toArray(new CanbusHardwareFilter[0]);
        return filters;
    }

    public void setFlowControlEnabled(boolean enableFlowControl){
        this.enableFlowControl = enableFlowControl;
    }

    private CanbusFlowControl[] setFlowControlMessages(){
        if (enableFlowControl){
            CanbusFlowControl[] flowControlMessages = new CanbusFlowControl[8];

            byte[] data1=new byte[]{0x10,0x34,0x56,0x78,0x1f,0x2f,0x3f,0x4f};

            flowControlMessages[0] = new CanbusFlowControl(0x18FEE000,0x18FEE018,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[1] = new CanbusFlowControl(0x1CECFF00,0x1CECFF1C,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[2] = new CanbusFlowControl(0x18FEE300,0x18FEE318,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[3] = new CanbusFlowControl(0x18FEE400,0x18FEE418,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[4] = new CanbusFlowControl(0x18FEE500,0x18FEE518,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[5] = new CanbusFlowControl(0x1CECEE00,0x1CECEE1C,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[6] = new CanbusFlowControl(0x1CECCC00,0x1CECCC00,CanbusFlowControl.EXTENDED,8,data1);
            flowControlMessages[7] = new CanbusFlowControl(0x1CECAA00,0x1CECAA00,CanbusFlowControl.EXTENDED,8,data1);
            return flowControlMessages;
        }
        else{
            return null;
        }
    }
    public void clearFilters() {
        // re-init the interface to clear filters
        CreateCanInterface1(silentMode, baudRate, termination, portNumber, false, enableFlowControl);
    }

    public void discardInBuffer() {
        canBusSocket1.discardInBuffer();
    }

    private void startPort1Threads() {
        if (j1939Port1Reader == null) {
            j1939Port1Reader = new J1939Port1Reader();
        }

        j1939Port1Reader.clearValues();

        if (j1939Port1ReaderThread == null || j1939Port1ReaderThread.getState() != Thread.State.NEW) {
            j1939Port1ReaderThread = new Thread(j1939Port1Reader);
        }

        j1939Port1ReaderThread.setPriority(Thread.NORM_PRIORITY + 3);
        j1939Port1ReaderThread.start();
    }

    private void startPort2Threads(){

        //For CAN2_TTY
        if (j1939Port2Reader == null) {
            j1939Port2Reader = new J1939Port2Reader();
        }

        j1939Port2Reader.clearValues();

        if (j1939Port2ReaderThread == null || j1939Port2ReaderThread.getState() != Thread.State.NEW) {
            j1939Port2ReaderThread = new Thread(j1939Port2Reader);
            //j1939Port2ReaderThread.setPriority(Thread.NORM_PRIORITY + 3);
            j1939Port2ReaderThread.start();
        }
    }

    public void closeCan1Interface() {
        if (canBusInterface1 != null) {
            canBusInterface1.removeCAN1();
            canBusInterface1 = null;
        }
        isCan1InterfaceOpen = false;
    }


    public void closeCan2Interface() {
        if (canBusInterface2 != null) {
            canBusInterface2.removeCAN2();
            canBusInterface2 = null;
        }
        isCan2InterfaceOpen = false;
    }

    public void closeCan1Socket() {
        if (isPort1SocketOpen()) {
            canBusSocket1.close1939Port1();
            canBusSocket1 = null;

            if (j1939Port1ReaderThread != null && j1939Port1ReaderThread.isAlive()) {
                j1939Port1ReaderThread.interrupt();
                try {
                    j1939Port1ReaderThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1939Port1WriterThread != null && j1939Port1WriterThread.isAlive()) {
                j1939Port1WriterThread.interrupt();
                try {
                    j1939Port1WriterThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1939Port1Writer != null){
                j1939Port1Writer.clearCounts();
            }
        }

    }

    public void closeCan2Socket() {
        if (isPort2SocketOpen()) {
            canBusSocket2.close1939Port2();
            canBusSocket2 = null;

            if (j1939Port2ReaderThread != null && j1939Port2ReaderThread.isAlive()) {
                j1939Port2ReaderThread.interrupt();
                try {
                    j1939Port2ReaderThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1939Port2WriterThread != null && j1939Port2WriterThread.isAlive()) {
                j1939Port2WriterThread.interrupt();
                try {
                    j1939Port2WriterThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1939Port2Writer != null){
                j1939Port2Writer.clearCounts();
            }
        }
    }

    /// J1939 Canbus Reader
    public int getPort1CanbusRxFrameCount() {
        if (j1939Port1Reader == null)
            return 0;
        return j1939Port1Reader.getCanbusFrameCount();
    }

    public int getPort1CanbusRxByteCount() {
        if (j1939Port1Reader == null)
            return 0;
        return j1939Port1Reader.getCanbusByteCount();
    }

    public int getPort2CanbusRxFrameCount() {return j1939Port2Reader.getCanbusFrameCount();}

    public int getPort2CanbusRxByteCount() {
        return j1939Port2Reader.getCanbusByteCount();
    }

    public int getPort1CanbusTxFrameCount(){
        if (j1939Port1Writer == null){
            return 0;
        }
        return j1939Port1Writer.getFrameCount();
    }

    public int getPort1CanbusTxByteCount(){
        if (j1939Port1Writer == null){
            return 0;
        }
        return j1939Port1Writer.getByteCount();
    }

    public int getPort2CanbusTxFrameCount(){
        if (j1939Port2Writer == null){
            return 0;
        }
        return j1939Port2Writer.getFrameCount();
    }

    public int getPort2CanbusTxByteCount(){
        if (j1939Port2Writer == null){
            return 0;
        }
        return j1939Port2Writer.getByteCount();
    }

    public boolean isAutoSendJ1939Port1() {
        return autoSendJ1939Port1;
    }

    public void setAutoSendJ1939Port1(boolean autoSendJ1939Port1) {
        this.autoSendJ1939Port1 = autoSendJ1939Port1;
    }

    public boolean isAutoSendJ1939Port2() {
        return autoSendJ1939Port2;
    }

    public void setAutoSendJ1939Port2(boolean autoSendJ1939Port2) {
        this.autoSendJ1939Port2 = autoSendJ1939Port2;
    }

    private class J1939Port1Reader implements Runnable {
        private volatile int canbusFrameCount = 0;
        private volatile int canbusByteCount = 0;

        int getCanbusFrameCount() {
            return canbusFrameCount;
        }

        int getCanbusByteCount() {
            return canbusByteCount;
        }

        void clearValues() {
            canbusByteCount = 0;
            canbusFrameCount = 0;
        }

        @Override
        public void run() {
            // J1939 data unit:
            // 3 bit - priority
            // 1 bit - reserved
            // 1 bit - data page
            // 8 bit - PDU format
            // 8 bit - PDU specific
            // 8 bit - source address
            // For a PGN, we need to keep only PDU format + PDU specific, and mask out everything else

            while (true) {
                CanbusFramePort1 canBusFrame1;
                try {
                    if (blockOnReadPort1) {
                        canBusFrame1 = canBusSocket1.readPort1();
                    } else {
                        canBusFrame1 = canBusSocket1.readPort1(READ_TIMEOUT);
                    }
                    if (canBusFrame1 != null)
                    {
                        long time = SystemClock.elapsedRealtime();
                        int pgn = ((canBusFrame1.getId() & pgn_mask)  >> 8);
//                        int sourceAdr=((canBusFrame1.getId() & src_addr_mask)  >> 8);
//                        int pdu_format= (((canBusFrame1.getId() & pdu_format_mask) >> 16));
//                        int pdu_specific=((canBusFrame1.getId() & pdu_specific_mask)  >> 8);
//                        byte[] dataBytes=canBusFrame1.getData();

                        String canFrameType="";
                        if(canBusFrame1.getType() == CanbusFrameType.STANDARD){
                            canFrameType=STD;
                        }
                        else if(canBusFrame1.getType() == CanbusFrameType.EXTENDED){
                            canFrameType=EXT;
                        }
                        else if (canBusFrame1.getType() == CanbusFrameType.STANDARD_REMOTE){
                            canFrameType=STD_R;
                        }
                        else if(canBusFrame1.getType() == CanbusFrameType.EXTENDED_REMOTE){
                            canFrameType=EXT_R;
                        }

                        // done to prevent adding too much text to UI at once
                        if (can1Data.length() < 500) {
                            // avoiding string.format for performance
                            can1Data.append(time);
                            can1Data.append(",");
                            can1Data.append(Integer.toHexString(canBusFrame1.getId()));
                            can1Data.append(",");
                            can1Data.append(canFrameType);
                            can1Data.append(",");
                            can1Data.append(Integer.toHexString(pgn));
                            can1Data.append(",[");
                            can1Data.append(bytesToHex(canBusFrame1.getData()));
                            can1Data.append("] (");
                            can1Data.append("),");
                            can1Data.append(canBusFrame1.getData().length);
                            can1Data.append("\n");
                        }

                        canbusFrameCount++;
                        canbusByteCount += canBusFrame1.getData().length;
                    }
                    //else {
                       // Log.d(TAG, "Read timeout");
                    //}
                }catch (NullPointerException ex) {
                    // socket is null
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public int getJ1939IntervalDelay() {
        return j1939IntervalDelay;
    }

    public void setJ1939IntervalDelay(int j1939IntervalDelay) {
        this.j1939IntervalDelay = j1939IntervalDelay;
    }

    private class J1939Port2Reader implements Runnable {
        private volatile int canbusFrameCount = 0;
        private volatile int canbusByteCount = 0;

        int getCanbusFrameCount() {
            return canbusFrameCount;
        }

        int getCanbusByteCount() {
            return canbusByteCount;
        }

        void clearValues() {
            canbusByteCount = 0;
            canbusFrameCount = 0;
        }

        @Override
        public void run() {
            // J1939 data unit:
            // 3 bit - priority
            // 1 bit - reserved
            // 1 bit - data page
            // 8 bit - PDU format
            // 8 bit - PDU specific
            // 8 bit - source address
            // We need to keep only PDU format + PDU specific, and mask out everything else
            //final int mask = 0b000_0_0_11111111_11111111_00000000;
            //final int mask = 0b000_1_1_11111111_11111111_00000000; //Included Reserved an DP
            //final int mask =0x03FFFF00;

            while (true) {
                CanbusFramePort2 canBusFrame2;
                try {
                    if (blockOnReadPort1) {
                        canBusFrame2 = canBusSocket2.readPort2();
                    } else {
                        canBusFrame2 = canBusSocket2.readPort2(READ_TIMEOUT);
                    }
                    if (canBusFrame2 != null) {
                        long time = SystemClock.elapsedRealtime();
                        int pgn = ((canBusFrame2.getId() & pgn_mask)  >> 8);
//                        int sourceAdr=((canBusFrame2.getId() & src_addr_mask)  >> 8);
//                        int pdu_format= (((canBusFrame2.getId() & pdu_format_mask) >> 16));
//                        int pdu_specific=((canBusFrame2.getId() & pdu_specific_mask)  >> 8);
//                        byte[] dataBytes=canBusFrame2.getData();
                        String canFrameType="";

                        if(canBusFrame2.getType() == CanbusFrameType.STANDARD){
                            canFrameType=STD;
                        }
                        else if(canBusFrame2.getType() == CanbusFrameType.EXTENDED){
                            canFrameType=EXT;
                        }
                        else if (canBusFrame2.getType() == CanbusFrameType.STANDARD_REMOTE){
                            canFrameType=STD_R;
                        }
                        else if(canBusFrame2.getType() == CanbusFrameType.EXTENDED_REMOTE){
                            canFrameType=EXT_R;
                        }
                        // done to prevent adding too much text to UI at once
                        if (can2Data.length() < 500) {
                            // avoiding string.format for performance
                            can2Data.append(time);
                            can2Data.append(",");
                            can2Data.append(Integer.toHexString(canBusFrame2.getId()));
                            can2Data.append(",");
                            can2Data.append(canFrameType);
                            can2Data.append(",");
                            can2Data.append(Integer.toHexString(pgn));
                            can2Data.append(",[");
                            can2Data.append(bytesToHex(canBusFrame2.getData()));
                            can2Data.append("] (");
                            can2Data.append("),");
                            can2Data.append(canBusFrame2.getData().length);
                            can2Data.append("\n");
                        }

                        canbusFrameCount++;
                        canbusByteCount += canBusFrame2.getData().length;
                    }
                    //else {
                        //Log.d(TAG, "Read timeout for Port 2");
                    //}
                } catch (NullPointerException ex) {
                    // socket is null
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
    }

    // Convert a byte array to a hex friendly string
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
    public void sendJ1939Port1() {
        if (j1939Port1Writer == null){
            j1939Port1Writer = new J1939Port1Writer();
        }
        if (j1939Port1WriterThread == null || !j1939Port1WriterThread.isAlive()) {
            j1939Port1WriterThread = new Thread(j1939Port1Writer);
            j1939Port1WriterThread.start();
        }
    }

    public void sendJ1939Port1(String messageType, String messageId, String messageData){
        canMessageDataPort1 = messageData.getBytes();
        canMessageIdPort1 = Integer.parseInt(messageId);

        if (messageType.contentEquals("T")) {
            canMessageTypePort1 = CanbusFrameType.EXTENDED;
        }
        else if (messageType.contentEquals("t")) {
            canMessageTypePort1 = CanbusFrameType.STANDARD;
        }
        else if (messageType.contentEquals("R")) {
            canMessageTypePort1 = CanbusFrameType.EXTENDED_REMOTE;
        }
        else if (messageType.contentEquals("r")) {
            canMessageTypePort1 = CanbusFrameType.STANDARD_REMOTE;
        }

        if (j1939Port1WriterThread == null || !j1939Port1WriterThread.isAlive()) {
            j1939Port1WriterThread = new Thread(sendJ1939Port1Runnable2);
            j1939Port1WriterThread.start();
        }
    }

    private class J1939Port1Writer implements Runnable{
        private int sentFrameCount = 0;
        private int sentByteCount = 0;

        void clearCounts(){
            this.sentByteCount = 0;
            this.sentFrameCount = 0;
        }

        private int getFrameCount(){
            return sentFrameCount;
        }

        private int getByteCount(){
            return sentByteCount;
        }

        @Override
        public void run() {
            CanbusFrameType MessageType;
            int MessageId;
            byte[] MessageData;
            do {

                MessageType= CanbusFrameType.EXTENDED;
                MessageId=J1939_FUEL_ECONOMY;
                int data = 0;
                ByteBuffer dBuf = ByteBuffer.allocate(8);
                dBuf.order(ByteOrder.LITTLE_ENDIAN);
                dBuf.putInt(data);
                byte[] a = dBuf.array();
                a[0] = 0x12;
                a[1] = 0x34;
                a[2] = 0x45;
                a[3] = 0x67;
                a[4] = 0x1F;
                a[5] = 0x2F;
                a[6] = 0x3F;
                a[7] = 0x4F;
                MessageData=a;

                if(canBusSocket1 != null) {
                    CanbusFramePort1 canFrame = new CanbusFramePort1(MessageId, MessageData,MessageType);
                    canBusSocket1.write1939Port1(canFrame);
                    sentByteCount += canFrame.getData().length;
                    sentFrameCount++;
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port1);
        }
    }

    private Runnable sendJ1939Port1Runnable1 = new Runnable() {
        private int sentFrameCount = 0;
        private int sentByteCount = 0;

        public int getFrameCount(){
            return sentFrameCount;
        }

        public int getSentByteCount(){
            return sentByteCount;
        }

        @Override
        public void run() {
            CanbusFrameType MessageType;
            int MessageId;
            byte[] MessageData;
            do {

                MessageType= CanbusFrameType.EXTENDED;
                MessageId=J1939_FUEL_ECONOMY;
                int data = 0;
                ByteBuffer dBuf = ByteBuffer.allocate(8);
                dBuf.order(ByteOrder.LITTLE_ENDIAN);
                dBuf.putInt(data);
                byte[] a = dBuf.array();
                a[0] = 0x12;
                a[1] = 0x34;
                a[2] = 0x45;
                a[3] = 0x67;
                a[4] = 0x1F;
                a[5] = 0x2F;
                a[6] = 0x3F;
                a[7] = 0x4F;
                MessageData=a;

                if(canBusSocket1 != null) {
                    CanbusFramePort1 canFrame = new CanbusFramePort1(MessageId, MessageData,MessageType);
                    canBusSocket1.write1939Port1(canFrame);
                    sentByteCount += canFrame.getData().length;
                    sentFrameCount++;
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port1);
        }
    };

    private final Runnable sendJ1939Port1Runnable2 = new Runnable() {
        @Override
        public void run() {
            do {
                if(canBusSocket1 != null) {
                    canBusSocket1.write1939Port1(new CanbusFramePort1(canMessageIdPort1, canMessageDataPort1, canMessageTypePort1));
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port1);
        }
    };

    private class J1939Port2Writer implements Runnable{
        private int sentFrameCount = 0;
        private int sentByteCount = 0;

        void clearCounts(){
            this.sentByteCount = 0;
            this.sentFrameCount = 0;
        }

        private int getFrameCount(){
            return sentFrameCount;
        }

        private int getByteCount(){
            return sentByteCount;
        }

        @Override
        public void run() {
            CanbusFrameType MessageType;
            int MessageId;
            byte[] MessageData;
            do {

                MessageType= CanbusFrameType.EXTENDED;
                MessageId=J1939_FUEL_ECONOMY;
                int data = 0;
                ByteBuffer dBuf = ByteBuffer.allocate(8);
                dBuf.order(ByteOrder.LITTLE_ENDIAN);
                dBuf.putInt(data);
                byte[] a = dBuf.array();
                a[0] = 0x12;
                a[1] = 0x34;
                a[2] = 0x45;
                a[3] = 0x67;
                a[4] = 0x1F;
                a[5] = 0x2F;
                a[6] = 0x3F;
                a[7] = 0x4F;
                MessageData=a;

                if(canBusSocket2 != null) {
                    CanbusFramePort2 canFrame = new CanbusFramePort2(MessageId, MessageData,MessageType);
                    canBusSocket2.write1939Port2(canFrame);
                    sentByteCount += canFrame.getData().length;
                    sentFrameCount++;
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port2);
        }
    }


    public void sendJ1939Port2() {
        if (j1939Port2Writer == null){
            j1939Port2Writer = new J1939Port2Writer();
        }
        if (j1939Port2WriterThread == null || !j1939Port2WriterThread.isAlive()) {
            j1939Port2WriterThread = new Thread(j1939Port2Writer);
            j1939Port2WriterThread.start();
        }
    }

    public void sendJ1939Port2(String messageType, String messageId, String messageData){
        canMessageDataPort2 = messageData.getBytes();
        canMessageIdPort2 = Integer.parseInt(messageId);

        switch (messageType) {
            case "T":
                canMessageTypePort2 = CanbusFrameType.EXTENDED;
                break;
            case "t":
                canMessageTypePort2 = CanbusFrameType.STANDARD;
                break;
            case "R":
                canMessageTypePort2 = CanbusFrameType.EXTENDED_REMOTE;
                break;
            case "r":
                canMessageTypePort2 = CanbusFrameType.STANDARD_REMOTE;
                break;
        }

        if (j1939Port2WriterThread == null || !j1939Port2WriterThread.isAlive()) {
            j1939Port2WriterThread = new Thread(sendJ1939Port2Runnable2);
            j1939Port2WriterThread.start();
        }
    }

    private Runnable sendJ1939Port2Runnable1 = new Runnable() {
        private int sentFrameCount = 0;
        private int sentByteCount = 0;

        public int getFrameCount(){
            return sentFrameCount;
        }

        public int getSentByteCount(){
            return sentByteCount;
        }

        @Override
        public void run() {
            CanbusFrameType MessageType;
            int MessageId;
            byte[] MessageData;
            do {
                //To send a different type of frame change this to CanbusFrameType.EXTENDED
                MessageType= CanbusFrameType.EXTENDED;
                //A different ID Can be sent by changing the value here
                MessageId=0xF110;
                int data = 0;
                ByteBuffer dBuf = ByteBuffer.allocate(8);
                dBuf.order(ByteOrder.LITTLE_ENDIAN);
                dBuf.putInt(data);
                byte[] a = dBuf.array();
                a[0] = 0x12;
                a[1] = 0x34;
                a[2] = 0x45;
                a[3] = 0x67;
                a[4] = 0x1F;
                a[5] = 0x2F;
                a[6] = 0x3F;
                a[7] = 0x4F;
                MessageData=a;
                if(canBusSocket2 != null) {
                    CanbusFramePort2 canFrame = new CanbusFramePort2(MessageId, MessageData,MessageType);
                    canBusSocket2.write1939Port2(canFrame);
                    sentByteCount += canFrame.getData().length;
                    sentFrameCount++;
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port2);
        }
    };

    private final Runnable sendJ1939Port2Runnable2 = new Runnable() {
        @Override
        public void run() {
            do {
                if(canBusSocket2 != null) {
                    canBusSocket2.write1939Port2(new CanbusFramePort2(canMessageIdPort2, canMessageDataPort2, canMessageTypePort2));
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port2);
        }
    };
    /// End J1939 methods

}
