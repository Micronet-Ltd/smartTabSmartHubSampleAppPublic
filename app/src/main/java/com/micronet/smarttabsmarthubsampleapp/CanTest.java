/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp;

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
import java.util.concurrent.Future;

public class CanTest {

    //private static CanTest instance = null;
    private final static String TAG = "CanTest";

    private int portNumber;
    public final static int CAN_PORT1 = 2;
    public final static int CAN_PORT2 = 3;
    private CanbusInterface canBusInterface;
    private CanbusSocket canbusSocket;
    private CanbusHardwareFilter[] canBusFilter;
    private CanbusFlowControl[] canBusFlowControls;

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final String STD = "STD";
    private static final String EXT = "EXT";
    private static final String STD_R = "STD_R";
    private static final String EXT_R = "EXT_R";

    public final StringBuilder canData = new StringBuilder(1000);

    private final int pgn_mask = 0b000_0_0_11111111_11111111_00000000;

    private int canMessageIdPort;
    private byte[] canMessageDataPort;
    private CanbusFrameType canMessageTypePort;

    private int j1939IntervalDelay = 500; // ms

    private Thread j1939PortReaderThread = null;
    private Thread j1939PortWriterThread = null;

    private J1939PortReader j1939PortReader = null;
    private J1939PortWriter j1939PortWriter = null;

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


    private volatile boolean blockOnReadPort = true;
    private final int READ_TIMEOUT = 100; // readPort1 timeout (in milliseconds)

    private int baudRate;
    private boolean removeCan;
    private boolean silentMode;
    private boolean termination;
    private volatile boolean autoSendJ1939Port;
    private boolean enableFilters = false;
    private boolean enableFlowControl = false;
    private boolean isCanInterfaceOpen = false;
    private boolean discardInBuffer;

    public CanTest(int portNumber) {
        //Mandatory constructor
        this.portNumber = portNumber;
    }

//    // Lazy Initialization (If required then only)
//    public static CanTest getInstance() {
//        if (instance == null) {
//            // Thread Safe. Might be costly operation in some case
//            synchronized (CanTest.class) {
//                if (instance == null) {
//                    instance = new CanTest();
//                }
//            }
//        }
//        return instance;
//    }

    public boolean isDiscardInBuffer() {
        return discardInBuffer;
    }

    public void setDiscardInBuffer(boolean discardInBuffer) {
        this.discardInBuffer = discardInBuffer;
    }

    public boolean isPortSocketOpen() {
        // there's actually no api call to check status of canBus socket but
        // this app will open the socket as soon as object is initialized.
        // also socket doesn't actually close1939Port1 even with call to QBridgeCanbusSocket.close1939Port1()
        return canbusSocket != null;
    }

    public boolean isCanInterfaceOpen() {
        return isCanInterfaceOpen;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public boolean isSilentChecked() {
        return silentMode;
    }

    public boolean getRemoveCan1InterfaceState() {
        return removeCan;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public void setSilentMode(boolean isSilent) {
        this.silentMode = isSilent;
    }

    public void setRemoveCanInterfaceState(boolean removeCan) {
        this.removeCan = removeCan;
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

    public int CreateCanInterface(boolean silentMode, int baudRate, boolean termination, int port, boolean enableFilters, boolean enableFlowControl) {
        this.silentMode = silentMode;
        this.baudRate = baudRate;
        this.termination=termination;
        this.portNumber=port;
        this.enableFilters = enableFilters;
        this.enableFlowControl = enableFlowControl;

        if (canBusInterface == null) {
            canBusInterface = new CanbusInterface();
            canBusFilter = setFilters();
            canBusFlowControls = setFlowControlMessages();
            try {
                if (portNumber == CAN_PORT1){
                    canBusInterface.create(silentMode, baudRate, termination, canBusFilter, 2, canBusFlowControls);
                    if (canbusSocket == null){
                        canbusSocket = canBusInterface.createSocketCAN1();
                        canbusSocket.openCan1();
                    }
                }
                else if (portNumber == CAN_PORT2){
                    canBusInterface.create(silentMode, baudRate, termination, canBusFilter, 3, canBusFlowControls);
                    if (canbusSocket == null){
                        canbusSocket = canBusInterface.createSocketCAN2();
                        canbusSocket.openCan2();
                    }
                }
            } catch (CanbusException e) {
                Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode());
                e.printStackTrace();
                return -1;
            }
        }

        if (discardInBuffer) {
            canbusSocket.discardInBuffer();
        }
        isCanInterfaceOpen = true;
        startPortThreads();
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
        CreateCanInterface(silentMode, baudRate, termination, portNumber, false, enableFlowControl);
    }

    public void discardInBuffer() {
        canbusSocket.discardInBuffer();
    }

    private void startPortThreads() {
        if (j1939PortReader == null) {
            j1939PortReader = new J1939PortReader();
        }

        j1939PortReader.clearValues();

        if (j1939PortReaderThread == null || j1939PortReaderThread.getState() != Thread.State.NEW) {
            j1939PortReaderThread = new Thread(j1939PortReader);
        }

        j1939PortReaderThread.setPriority(Thread.NORM_PRIORITY + 3);
        j1939PortReaderThread.start();
    }

    public void closeCanInterface() {
        if (canBusInterface != null) {
            if (portNumber == CAN_PORT1){
                canBusInterface.removeCAN1();

            }
            else if (portNumber == CAN_PORT2){
                canBusInterface.removeCAN2();
            }
            canBusInterface = null;
        }
        isCanInterfaceOpen = false;
    }

    public void closeCanSocket() {
        if (isPortSocketOpen()) {
            if (j1939PortReaderThread != null && j1939PortReaderThread.isAlive()) {
                j1939PortReaderThread.interrupt();
                try {
                    j1939PortReader.stopRunnable();
                    j1939PortReaderThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1939PortWriterThread != null && j1939PortWriterThread.isAlive()) {
                j1939PortWriterThread.interrupt();
                try {
                    j1939PortWriterThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1939PortWriter != null){
                j1939PortWriter.clearCounts();
            }

            if (portNumber == CAN_PORT1){
                canbusSocket.close1939Port1();
            }
            else if (portNumber == CAN_PORT2){
                canbusSocket.close1939Port2();
            }
            canbusSocket = null;
        }

    }

    /// J1939 Canbus Reader
    public int getPortCanbusRxFrameCount() {
        if (j1939PortReader == null)
            return 0;
        return j1939PortReader.getCanbusFrameCount();
    }

    public int getPortCanbusRxByteCount() {
        if (j1939PortReader == null)
            return 0;
        return j1939PortReader.getCanbusByteCount();
    }

    public int getPortCanbusTxFrameCount(){
        if (j1939PortWriter == null){
            return 0;
        }
        return j1939PortWriter.getFrameCount();
    }

    public int getPortCanbusTxByteCount(){
        if (j1939PortWriter == null){
            return 0;
        }
        return j1939PortWriter.getByteCount();
    }

    public boolean isAutoSendJ1939Port() {
        return autoSendJ1939Port;
    }

    public void setAutoSendJ1939Port(boolean autoSendJ1939Port) {
        this.autoSendJ1939Port = autoSendJ1939Port;
    }

    private class J1939PortReader implements Runnable {
        private volatile int canbusFrameCount = 0;
        private volatile int canbusByteCount = 0;
        private volatile boolean keepRunning = true;

        int getCanbusFrameCount() {
            return canbusFrameCount;
        }

        int getCanbusByteCount() {
            return canbusByteCount;
        }

        void clearValues() {
            canbusByteCount = 0;
            canbusFrameCount = 0;
            keepRunning = true;
        }

        void stopRunnable(){
            keepRunning = false;
        }

        private void getPort1Frames(){
            CanbusFramePort1 canBusFrame1;
            try {
                if (canbusSocket == null){
                    return;
                }

                if (blockOnReadPort) {
                    canBusFrame1 = canbusSocket.readPort1();
                } else {
                    canBusFrame1 = canbusSocket.readPort1(READ_TIMEOUT);
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
                    //if (canData.length() < 500) {
                        // avoiding string.format for performance
                        canData.append(time);
                        canData.append(",");
                        canData.append(Integer.toHexString(canBusFrame1.getId()));
                        canData.append(",");
                        canData.append(canFrameType);
                        canData.append(",");
                        canData.append(Integer.toHexString(pgn));
                        canData.append(",[");
                        canData.append(bytesToHex(canBusFrame1.getData()));
                        canData.append("],");
                        canData.append(canBusFrame1.getData().length);
                        canData.append("\n");
                    //}

                    canbusFrameCount++;
                    canbusByteCount += canBusFrame1.getData().length;
                }
                //else {
                // Log.d(TAG, "Read timeout");
                //}
            }catch (NullPointerException ex) {
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void getPort2Frames(){
            CanbusFramePort2 canBusFrame2;
            try {
                if (canbusSocket == null){
                    return;
                }

                if (blockOnReadPort) {
                    canBusFrame2 = canbusSocket.readPort2();
                } else {
                    canBusFrame2 = canbusSocket.readPort2(READ_TIMEOUT);
                }
                if (canBusFrame2 != null)
                {
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
                    //if (canData.length() < 500) {
                        // avoiding string.format for performance
                        canData.append(time);
                        canData.append(",");
                        canData.append(Integer.toHexString(canBusFrame2.getId()));
                        canData.append(",");
                        canData.append(canFrameType);
                        canData.append(",");
                        canData.append(Integer.toHexString(pgn));
                        canData.append(",[");
                        canData.append(bytesToHex(canBusFrame2.getData()));
                        canData.append("],");
                        canData.append(canBusFrame2.getData().length);
                        canData.append("\n");
                    //}

                    canbusFrameCount++;
                    canbusByteCount += canBusFrame2.getData().length;
                }
                //else {
                // Log.d(TAG, "Read timeout");
                //}
            }catch (NullPointerException ex) {
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            Log.d(TAG, "J1939PortReader started, portNumber="+ portNumber);
            while (keepRunning == true) {
                if (portNumber == CAN_PORT1){
                    getPort1Frames();
                }
                else if (portNumber == CAN_PORT2){
                    getPort2Frames();
                }
            }
            Log.d(TAG, "J1939PortReader exited, portNumber="+ portNumber);
        }
    }

    public int getJ1939IntervalDelay() {
        return j1939IntervalDelay;
    }

    public void setJ1939IntervalDelay(int j1939IntervalDelay) {
        this.j1939IntervalDelay = j1939IntervalDelay;
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
    public void sendJ1939Port() {
        if (j1939PortWriter == null){
            j1939PortWriter = new J1939PortWriter();
        }
        if (j1939PortWriterThread == null || !j1939PortWriterThread.isAlive()) {
            j1939PortWriterThread = new Thread(j1939PortWriter);
            j1939PortWriterThread.start();
        }
    }

    public void sendJ1939Port(String messageType, String messageId, String messageData){
        canMessageDataPort = messageData.getBytes();
        canMessageIdPort = Integer.parseInt(messageId);

        if (messageType.contentEquals("T")) {
            canMessageTypePort = CanbusFrameType.EXTENDED;
        }
        else if (messageType.contentEquals("t")) {
            canMessageTypePort = CanbusFrameType.STANDARD;
        }
        else if (messageType.contentEquals("R")) {
            canMessageTypePort = CanbusFrameType.EXTENDED_REMOTE;
        }
        else if (messageType.contentEquals("r")) {
            canMessageTypePort = CanbusFrameType.STANDARD_REMOTE;
        }

        if (j1939PortWriterThread == null || !j1939PortWriterThread.isAlive()) {
            j1939PortWriterThread = new Thread(sendJ1939PortRunnable2);
            j1939PortWriterThread.start();
        }
    }

    private class J1939PortWriter implements Runnable{
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

                if(canbusSocket != null) {
                    if (portNumber == CAN_PORT1){
                        CanbusFramePort1 canFrame = new CanbusFramePort1(MessageId, MessageData,MessageType);
                        canbusSocket.write1939Port1(canFrame);
                        sentByteCount += canFrame.getData().length;
                    }
                    else if (portNumber == CAN_PORT2){
                        CanbusFramePort2 canFrame = new CanbusFramePort2(MessageId, MessageData,MessageType);
                        canbusSocket.write1939Port2(canFrame);
                        sentByteCount += canFrame.getData().length;
                    }
                    sentFrameCount++;
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port);
        }
    }

    private final Runnable sendJ1939PortRunnable2 = new Runnable() {
        @Override
        public void run() {
            do {
                if(canbusSocket != null) {

                    if (portNumber == CAN_PORT1){
                        canbusSocket.write1939Port1(new CanbusFramePort1(canMessageIdPort, canMessageDataPort, canMessageTypePort));
                    }
                    else if (portNumber == CAN_PORT2){
                        canbusSocket.write1939Port2(new CanbusFramePort2(canMessageIdPort, canMessageDataPort, canMessageTypePort));
                    }
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1939Port);
        }
    };

    /// End J1939 methods

}
