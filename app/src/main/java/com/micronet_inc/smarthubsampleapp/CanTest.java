package com.micronet_inc.smarthubsampleapp;

import static java.lang.Thread.sleep;
import static java.util.Arrays.copyOfRange;

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
import com.micronet.canbus.Info;
import com.micronet.canbus.J1708Frame;
import com.micronet.canbus.J1708Interface;
import com.micronet.canbus.J1708Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CanTest {

    private static CanTest instance = null;
    private final static String TAG = "CanTest";

    protected CanTest() {
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

    class TpConnectionFrame {
        int pgn;
        int messageSizeInBytes;
        int expectedPacketsNum; //Consider the connection to be closed if this is set to zero
        int maxPacketsPerBurst;
        int sourceAdd; // This would refer to the remote address that sourced the connection
        int destinationAdd; // us (or the global address)
        long timeoutElapsedms; // time that this connection will expire in elapsed ms
        byte[] data;
    }

    private CanbusInterface canbusInterface1;
    private CanbusInterface canbusInterface2;
    private J1708Interface j1708Interface;
    private CanbusSocket canbusSocket1;
    private CanbusSocket canbusSocket2;
    private J1708Socket j1708Socket;// = new J1708Socket();
    CanbusHardwareFilter[] canbusFilter;
    CanbusFlowControl[] canbusFlowControls;

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final String STD = "STD";
    private static final String EXT = "EXT";
    private static final String STD_R = "STD_R";
    private static final String EXT_R = "EXT_R";
    private String txCanMessage="";

    public StringBuilder can1Data = new StringBuilder(1000);
    public StringBuilder can2Data = new StringBuilder(1000);
    public StringBuilder j1708Data = new StringBuilder(1000);

    //1939 Parameter Group Numbers
    public static final int J1939_ENGINE_CONTROLLER2 = 0x00F003;
    public static final int J1939_ENGINE_CONTROLLER1 = 0x00F004;
    public static final int J1939_PGN_GEAR = 0x00F005; // ECM2
    public static final int J1939_PGN_ODOMETER_LOW = 0x00FEE0;
    public static final int J1939_PGN_ODOMETER_HIGH = 0x00FEC1;
    public static final int J1939_PGN_ENGINE_HOURS_REVOLUTIONS = 0x00FEE5;
    public static final int J1939_PGN_FUEL_CONSUMPTION = 0x00FEE9;
    public static final int J1939_PGN_VIN_NUMBER = 0x00FEEC;
    public static final int J1939_ENGINE_TEMPERATURE_1 = 0x00FEEE;
    public static final int J1939_PGN_PARKING = 0x00FEF1;
    public static final int J1939_FUEL_ECONOMY = 0x00FEF2;
    public static final int J1939_PGN_DASH_DISP = 0x00FEFC;

    private static final int J1939_PDU_FORMAT_REQUEST = 0xEA;
    public static final int ourSourceAddress =0x00;
    public static final int globalAddress =0xFF;

    //Connection Management
    private static final int J1939_PDU_FORMAT_MANAGE_CONN = 0xEC;  // Manage a connection
    private static final int J1939_PDU_FORMAT_DATA_CONN = 0xEB;// Transfer data in a connection

    //Types of Manage connection messages
    public static final int J1939_TP_CM_RTS = 16; // Request To Send
    public static final int J1939_TP_CM_CTS = 17; // Clear To Send
    public static final int J1939_TP_CM_EOM = 19; // End of message ACK
    public static final int J1939_TP_CM_ABORT = 255;
    public static final int J1939_TP_CM_BAM = 32; // Broadcast Announce Message

    static final int J1939_TP_ALLOWED_PGNS[] = new int[] {J1939_PGN_VIN_NUMBER};
    public static final int MAX_TP_FRAMES_PER_BURST = 1; // maximum number of frames we will accept in each burst
    public static final int TP_TIMEOUT_MS = 1250;

    TpConnectionFrame connections[] = new TpConnectionFrame[5]; // list of open connections

    final int pgn_mask = 0b000_0_0_11111111_11111111_00000000;
    final int priority_mask = 0b111_0_0_00000000_00000000_00000000;
    final int src_addr_mask = 0b000_0_0_00000000_00000000_11111111;
    final int pdu_format_mask = 0b000_0_0_11111111_00000000_00000000;
    final int pdu_specific_mask = 0b000_0_0_00000000_11111111_00000000;

    int canMessageIdPort1;
    byte[] canMessageDataPort1;
    boolean usersDataPort1 =false;
    CanbusFrameType canMessageTypePort1;

    int canMessageIdPort2;
    byte[] canMessageDataPort2;
    boolean usersDataPort2 =false;
    CanbusFrameType canMessageTypePort2;

    private int j1939IntervalDelay = 500; // ms
    private int j1708IntervalDelay = 500; // ms

    private Thread j1939Port1ReaderThread = null;
    private Thread j1939Port1SendThread = null;

    private Thread j1939Port2ReaderThread = null;
    private Thread j1939Port2SendThread = null;

    private Thread j1708ReaderThread = null;
    private Thread j1708SendThread = null;


    private J1939Port1Reader j1939Port1Reader = null;
    private J1939Port2Reader j1939Port2Reader = null;
    private J1708Reader j1708Reader = null;

    private volatile boolean blockOnReadPort1 = false;
    private volatile boolean blockOnReadJ1708 = false;
    private final int READ_TIMEOUT = 500; // readPort1 timeout (in milliseconds)

    private int baudrate;
    private boolean removeCan1;
    private boolean removeCan2;
    private boolean removeJ1708=false;
    private boolean silentMode;
    private int portNumber;
    private boolean termination;
    private volatile boolean autoSendJ1939Port1;
    private volatile boolean autoSendJ1939Port2;
    private volatile boolean autoSendJ1708;
    private boolean enableFilters = false;
    private boolean enableFlowControl = false;
    private boolean isCan1InterfaceOpen = false;
    private boolean isCan2InterfaceOpen = false;
    private boolean isJ1708InterfaceOpen = false;
    private boolean discardInBuffer;

    public static String txtRequestVin ="None";
    public static String txtRequestEngineHours= "-1";
    public static int txtGetLowOdometer = -1;
    public static int txtGetHighOdometer= -1;
    public static int txtGetVehicleSpeed= -1;
    public static int txtTransmissionGear = -1;

    public boolean isDiscardInBuffer() {
        return discardInBuffer;
    }

    public void setDiscardInBuffer(boolean discardInBuffer) {
        this.discardInBuffer = discardInBuffer;
    }

    public boolean isPort1SocketOpen() {
        // there's actually no api call to check status of canbus socket but
        // this app will open the socket as soon as object is initialized.
        // also socket doesn't actually close1939Port1 even with call to QBridgeCanbusSocket.close1939Port1()
        return canbusSocket1 != null;
    }

    public boolean isj1708SocketOpen() {
        // there's actually no api call to check status of j1708 socket but
        // this app will open the socket as soon as object is initialized.
        // also socket doesn't actually close1939Port1 even with call to QBridgeCanbusSocket.close1939Port1()
        return j1708Socket != null;
    }

    public boolean isPort2SocketOpen() {
        // there's actually no api call to check status of canbus socket but
        // this app will open the socket as soon as object is initialized.
        // also socket doesn't actually close1939Port1 even with call to QBridgeCanbusSocket.close1939Port1()
        return canbusSocket2 != null;
    }

    public boolean isCan1InterfaceOpen() {
        return isCan1InterfaceOpen;
    }

    public boolean isCan2InterfaceOpen() {
        return isCan2InterfaceOpen;
    }

    public boolean isJ1708InterfaceOpen() {return isJ1708InterfaceOpen;}

    public int getBaudrate() {
        return baudrate;
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

    public boolean getRemove1708InterfaceState() {
        return removeJ1708;
    }

    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
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

    public void setRemoveJ1708InterfaceState(boolean removej1708) {
        this.removeJ1708 = removej1708;
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

    public String getVersion() {return Info.VERSION;}

    public String requestVinNumber(int portNumber, int toAddress){
        requestPgn(portNumber,J1939_PGN_VIN_NUMBER, toAddress, CanbusFrameType.EXTENDED);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return txtRequestVin;
    }

    public String requestEngineHours(int portNumber, int toAddress){
        requestPgn(portNumber, J1939_PGN_ENGINE_HOURS_REVOLUTIONS, toAddress, CanbusFrameType.EXTENDED);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return txtRequestEngineHours;
    }

    public int getTxtOdometer(){
        return txtGetLowOdometer;
    }

    public int getTxtVehicleSpeed(){
        return txtGetVehicleSpeed;
    }

    public int getTxtTransmissionGear(){
        return txtTransmissionGear;
    }

    public int CreateCanInterface1(boolean silentMode, int baudrate, boolean termination, int port) {
        this.silentMode = silentMode;
        this.baudrate = baudrate;
        this.termination=termination;
        this.portNumber=port;

        if (canbusInterface1 == null) {
            canbusInterface1 = new CanbusInterface();
            canbusFilter = setFilters();
            canbusFlowControls = setFlowControlMessages();
            try {
                canbusInterface1.create(silentMode, baudrate, termination, canbusFilter, 2, canbusFlowControls);
                //canbusInterface1.create(silentMode,baudrate,termination,canbusFilter,2);
            } catch (CanbusException e) {
                Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode());
                e.printStackTrace();
                return -1;
            }
        }

        if (canbusSocket1 == null) {
            canbusSocket1 = canbusInterface1.createSocketCAN1();
            canbusSocket1.openCan1();
        }
        if (discardInBuffer) {
            canbusSocket1.discardInBuffer();
        }
        isCan1InterfaceOpen = true;
        startPort1Threads();
        for (int i=0; i< 5; i++ )
            connections[i] = new TpConnectionFrame();
        return 0;
    }

    public int CreateCanInterface2(boolean silentMode, int baudrate, boolean termination, int port) {
        this.silentMode = silentMode;
        this.baudrate = baudrate;
        this.termination=termination;
        this.portNumber=port;

        if (canbusInterface2 == null  ) {
            canbusInterface2 = new CanbusInterface();
            canbusFilter=setFilters();
            canbusFlowControls=setFlowControlMessages();
            try {
                canbusInterface2.create(silentMode,baudrate,termination,canbusFilter,3,canbusFlowControls);
            } catch (CanbusException e) {
                Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode() );
                e.printStackTrace();
                return -1;
            }
        }
        if (canbusSocket2 == null) {
            canbusSocket2 = canbusInterface2.createSocketCAN2();
            canbusSocket2.openCan2();
        }
        if (discardInBuffer) {
            canbusSocket2.discardInBuffer();
        }
        isCan2InterfaceOpen = true;
        startPort2Threads();
        return 0;
    }

    public int create1708Interface(){
        Log.d(TAG, "Creating a 1708 Interface - Java");
        if(j1708Interface == null){
            j1708Interface = new J1708Interface();
            j1708Interface.createJ1708();
        }

        if(j1708Socket == null){
            //j1708Socket = J1708Socket();
            j1708Socket = j1708Interface.createSocketJ1708();
            j1708Socket.openJ1708();
        }
        isJ1708InterfaceOpen = true;
        startJ1708Threads();
        return 0;
    }

    public void silentMode(boolean silentMode) {
        this.silentMode = silentMode;
    }

    public CanbusHardwareFilter[] setFilters() {
        enableFilters = true;
        ArrayList<CanbusHardwareFilter> filterList = new ArrayList<CanbusHardwareFilter>();
        CanbusHardwareFilter[] filters;
        // Up to 24 filters
        int[] ids = new int[]{0x18FEE000, 0x1CECFF00 , 0x1CEBFF00, 0x18FEE500 , 0x18FEF100, J1939_ENGINE_CONTROLLER2 << 8, J1939_ENGINE_CONTROLLER1 << 8 , J1939_PGN_DASH_DISP << 8, J1939_PGN_GEAR << 8, J1939_ENGINE_TEMPERATURE_1 << 8 , J1939_FUEL_ECONOMY << 8};
        int[] mask = {0x1FFFFFFF, 0x1FFF00FF, 0x1FFF00FF, 0x1FFFFFFF, 0x1FFFFFFF, 0x00FFFF00, 0x00FFFF00, 0x00FFFF00, 0x00FFFF00, 0x00FFFF00, 0x00FFFF00};
        int[] type={CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,
                CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED,
                CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED, CanbusHardwareFilter.EXTENDED};
        filterList.add(new CanbusHardwareFilter(ids,mask, type));
        filters = filterList.toArray(new CanbusHardwareFilter[0]);
        return filters;
    }

    public CanbusFlowControl[] setFlowControlMessages(){

        enableFlowControl = true;

        CanbusFlowControl[] flowControlMessages = new CanbusFlowControl[8];

        byte[] data1=new byte[]{0x10,0x34,0x56,0x78,0x1f,0x2f,0x3f,0x4f};

        flowControlMessages[0] = new CanbusFlowControl(0x18FEE000,0x18FEE018, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[1] = new CanbusFlowControl(0x1CECFF00,0x1CECFF1C, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[2] = new CanbusFlowControl(0x18FEE300,0x18FEE318, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[3] = new CanbusFlowControl(0x18FEE400,0x18FEE418, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[4] = new CanbusFlowControl(0x18FEE500,0x18FEE518, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[5] = new CanbusFlowControl(0x1CECEE00,0x1CECEE1C, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[6] = new CanbusFlowControl(0x1CECCC00,0x1CECCC00, CanbusFlowControl.EXTENDED,8,data1);
        flowControlMessages[7] = new CanbusFlowControl(0x1CECAA00,0x1CECAA00, CanbusFlowControl.EXTENDED,8,data1);
        return flowControlMessages;
    }

    public void clearFilters() {
        // re-init the interface to clear filters
        enableFilters = false;
        CreateCanInterface1(silentMode, baudrate, termination, portNumber);
    }

    public void requestPgn(int portNumber, int requestedPgn, int toAddress, CanbusFrameType type){
        int priority=6;
        int request_pgn = setFrameId(priority, J1939_PDU_FORMAT_REQUEST, toAddress);

        CanbusFrameType requestFrameType=type;

        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(requestedPgn);
        byte[] J1939data = copyOfRange(b.array(), 0, 3);

        if(portNumber == 2){
            if(canbusSocket1 != null) {
                canbusSocket1.write1939Port1(new CanbusFramePort1(request_pgn, J1939data,requestFrameType));
                Log.d(TAG, "Request frame written to Port 1!");
            }
            try {
                sleep(j1939IntervalDelay);
            }
            catch (InterruptedException e) {
            }
        }
        else if(portNumber == 3){
            if(canbusSocket2 != null) {
                canbusSocket2.write1939Port2(new CanbusFramePort2(request_pgn, J1939data,requestFrameType));
                Log.d(TAG, "Request Frame written to Port 2!");
            }
            try {
                sleep(j1939IntervalDelay);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public void sendCTS(int portNumber, int toAddress, int pgn, int maxPackets, int firstPacket) {

        int frameId, priority=7;
        byte[] dataBytes = new byte[8];
        CanbusFrameType requestFrameType= CanbusFrameType.EXTENDED;

        byte[] requestedPgn = getConnectionPgn(pgn);

        frameId= setFrameId(priority,J1939_PDU_FORMAT_MANAGE_CONN,toAddress);

        dataBytes[0]= J1939_TP_CM_CTS;
        dataBytes[1]= (byte) maxPackets;
        dataBytes[2]= (byte) firstPacket;
        dataBytes[3]= (byte) 0xFF;
        dataBytes[4]= (byte) 0xFF;
        dataBytes[5]= requestedPgn[0];
        dataBytes[6]= requestedPgn[1];
        dataBytes[7]= requestedPgn[2];

        Log.d(TAG, "######## Sending CTS ########");

        if(portNumber==2){
            if(canbusSocket1 != null) {
                canbusSocket1.write1939Port1(new CanbusFramePort1(frameId, dataBytes,requestFrameType));
                Log.d(TAG, "######## Sent CTS ######## Max Packets=" + maxPackets + " First Packet="+ firstPacket);
            }
            try {
                sleep(j1939IntervalDelay);
            }
            catch (InterruptedException e) {
            }
        }
        else if(portNumber==3){
            if(canbusSocket2 != null) {
                canbusSocket2.write1939Port2(new CanbusFramePort2(frameId, dataBytes,requestFrameType));
            }
            try {
                sleep(j1939IntervalDelay);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public void sendConnectEOM(int portNumber, int toAddress, int pgn, int totalBytes, int totalPackets) {

        int frameId, priority=7;
        byte[] dataBytes = new byte[8];
        CanbusFrameType requestFrameType= CanbusFrameType.EXTENDED;

        Log.d(TAG, "######## Sending EOM ########");

        byte[] requestedPgn = getConnectionPgn(pgn);

        frameId= setFrameId(priority,J1939_PDU_FORMAT_MANAGE_CONN, toAddress);

        dataBytes[0]= J1939_TP_CM_EOM;
        dataBytes[1]= (byte) (totalBytes & 0xFF);
        dataBytes[2]= (byte) ((totalBytes >> 8) & 0xFF);
        dataBytes[3]= (byte) totalPackets;
        dataBytes[4]= (byte) 0xFF;
        dataBytes[5]= requestedPgn[0];
        dataBytes[6]= requestedPgn[1];
        dataBytes[7]= requestedPgn[2];

        if(portNumber==2){
            if(canbusSocket1 != null) {
                canbusSocket1.write1939Port1(new CanbusFramePort1(frameId, dataBytes,requestFrameType));
                Log.d(TAG, "######## Sent EOM ########");
            }
            try {
                sleep(j1939IntervalDelay);
            }
            catch (InterruptedException e) {
            }
        }
        else if(portNumber==3){
            if(canbusSocket2 != null) {
                canbusSocket2.write1939Port2(new CanbusFramePort2(frameId, dataBytes,requestFrameType));
            }
            try {
                sleep(j1939IntervalDelay);
            }
            catch (InterruptedException e) {
            }
        }

    }

    public int setFrameId(int priority, int pduFormat, int pduSpecific){
        int frameId=0;
        frameId=priority;
        frameId <<= 1;
        frameId |= 0; // Reserved
        frameId <<= 1;
        frameId |= 0; // Data Page
        frameId <<= 8;
        frameId |= pduFormat;
        frameId <<= 8;
        frameId |= pduSpecific;  // or protocol-specific for some formats
        frameId <<= 8;
        frameId |= ourSourceAddress ;
        return frameId;
    }

    public byte[] getConnectionPgn(int pgn){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(pgn);
        byte[] requestedPgn = copyOfRange(b.array(), 0, 3);
        return requestedPgn;
    }

    public void discardInBuffer() {
        canbusSocket1.discardInBuffer();
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
        }

        //j1939Port2ReaderThread.setPriority(Thread.NORM_PRIORITY + 3);
        j1939Port2ReaderThread.start();
    }

    private void startJ1708Threads() {

        if (j1708Reader == null) {
            j1708Reader = new J1708Reader();
        }

        j1708Reader.clearValues();


        if (j1708ReaderThread == null || j1708ReaderThread.getState() != Thread.State.NEW) {
            j1708ReaderThread = new Thread(j1708Reader);
        }
        j1708ReaderThread.start();
    }

    public void closeCan1Interface() {
        if (canbusInterface1 != null) {
            canbusInterface1.removeCAN1();
            canbusInterface1 = null;
        }
        isCan1InterfaceOpen = false;
    }


    public void closeCan2Interface() {
        if (canbusInterface2 != null) {
            canbusInterface2.removeCAN2();
            canbusInterface2 = null;
        }
        isCan2InterfaceOpen = false;
    }

    public int closeJ1708Interface() {
        int code=-1;
        if (j1708Interface != null) {
            code = j1708Interface.removeJ1708();
            j1708Interface = null;
            isJ1708InterfaceOpen = false;
            return code;
        }
        isJ1708InterfaceOpen = false;
        return code;
    }


    public void closeCan1Socket() {
        if (isPort1SocketOpen()) {
            canbusSocket1.close1939Port1();
            canbusSocket1 = null;

            if (j1939Port1ReaderThread != null && j1939Port1ReaderThread.isAlive()) {
                j1939Port1ReaderThread.interrupt();
                try {
                    j1939Port1ReaderThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void closeCan2Socket() {
        if (isPort2SocketOpen()) {
            canbusSocket2.close1939Port2();
            canbusSocket2 = null;

            if (j1939Port2ReaderThread != null && j1939Port2ReaderThread.isAlive()) {
                j1939Port2ReaderThread.interrupt();
                try {
                    j1939Port2ReaderThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void closeJ1708Socket() {
        if (isj1708SocketOpen()) {
            j1708Socket.close1708Port();
            j1708Socket = null;

            if (j1708ReaderThread != null && j1708ReaderThread.isAlive()) {
                j1708ReaderThread.interrupt();
                try {
                    j1708ReaderThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /// J1939 Canbus Reader
    public int getPort1CanbusFrameCount() {
        if (j1939Port1Reader == null)
            return 0;
        return j1939Port1Reader.getCanbusFrameCount();
    }

    public int getPort1CanbusByteCount() {
        if (j1939Port1Reader == null)
            return 0;
        return j1939Port1Reader.getCanbusByteCount();
    }

    public int getPort1CanbusRollovers() {

        return j1939Port1Reader.getRollovers();
    }

    public int getPort1CanbusMaxdiff() {
        return j1939Port1Reader.getMaxdiff();
    }

    public int getPort2CanbusFrameCount() {return j1939Port2Reader.getCanbusFrameCount();}

    public int getPort2CanbusByteCount() {
        return j1939Port2Reader.getCanbusByteCount();
    }

    public int getPort2CanbusRollovers() {
        return j1939Port2Reader.getRollovers();
    }

    public int getPort2CanbusMaxdiff() {
        return j1939Port2Reader.getMaxdiff();
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

    public boolean isAutoSendJ1708() {
        return autoSendJ1708;
    }

    public void setAutoSendJ1708(boolean autoSendJ1708) {
        this.autoSendJ1708 = autoSendJ1708;
    }

    public void setBlockOnReadPort1(boolean blockOnReadPort1) {
        this.blockOnReadPort1 = blockOnReadPort1;
    }




    /*
    * Received a connection management request [Response for requested PGN]
    * */
    public void rxManageConnection(int portNum, int sAddress, int dAddress, byte[] data){

        /*
        * [Bytes 1 and 2] = Number of bytes
        * [Byte 3] = Total packets
        * [Byte 4] = Max packets per CTS , FF = no limit
        * [Byte 5] = PGN (Little Endian)
        * */

        int connectionId=0, pgn=0, i=0;
        boolean sendResponse= false;

        byte connectionPgn[] = Arrays.copyOfRange(data, 5,9);
        ByteBuffer bb = ByteBuffer.wrap(connectionPgn);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        pgn = bb.getInt() & 0xFFFFFFFF;
        Log.d(TAG, "rxManageConnection - Pgn="+Integer.toHexString(pgn));

        if ((data[0] != (byte)J1939_TP_CM_RTS ) && (data[0] != (byte) J1939_TP_CM_BAM)) {
            return;
        }

        for (i =0 ; i < J1939_TP_ALLOWED_PGNS.length; i++) {
            if (J1939_TP_ALLOWED_PGNS[i] == pgn) break;
        }

        if ((dAddress == ourSourceAddress) && (data[0] == (byte) J1939_TP_CM_RTS)) {
            sendResponse = true;
        }

        connectionId = findOpenConnection(sAddress, dAddress);
        if (connectionId < 0) {
            // No open connections, find an available ID for the new connection
            for (connectionId=0 ; connectionId < 5; connectionId ++) {
                if (isConnectionIdAvailable(i)) break;
            }
        }

        //Accepting a connection
        TpConnectionFrame connection = connections[connectionId];
        connection.pgn = pgn;
        connection.messageSizeInBytes = ((((int) data[2]) & 0xFF) << 8) | (data[1] & 0xFF);
        connection.expectedPacketsNum = ((int) data[3]) & 0xFF;
        connection.maxPacketsPerBurst = ((((int) data[4]) & 0xFF) > MAX_TP_FRAMES_PER_BURST ? MAX_TP_FRAMES_PER_BURST : ((int) data[4]) & 0xFF);
        connection.sourceAdd = sAddress;
        connection.destinationAdd = dAddress;
        connection.timeoutElapsedms = SystemClock.elapsedRealtime() + TP_TIMEOUT_MS;
        connection.data = new byte[connection.messageSizeInBytes]; // created an array
        Arrays.fill(connection.data, (byte) 0xFF);
        Log.v (TAG, "accept <-- " + String.format("%02X to %02X (%04X)", connection.sourceAdd, connection.destinationAdd, connection.pgn) + " expect " + connection.expectedPacketsNum + " packets " + connection.messageSizeInBytes + " bytes");

        if(sendResponse){
            sendCTS(portNum,dAddress, pgn, connection.maxPacketsPerBurst, 1);
        }
    }

    public void rxConnectionData(int portNumber, int sAddress, int dAddress, byte[] data){

        // [Byte 0] - Sequence Number
        // [Byte 1-7] - Data bytes

        boolean sendResponse = false;
        int startingByte;
        int connectionID = findOpenConnection(sAddress, dAddress);

        if (connectionID < 0) {
            //No Open connections
            return;
        }

        // open connection found, update the time-out
        connections[connectionID].timeoutElapsedms = SystemClock.elapsedRealtime() + TP_TIMEOUT_MS + 2000;

        if(connections[connectionID].destinationAdd == ourSourceAddress){
            sendResponse = true;
        }

        if (data[0] == 0) {
            //The sequence number HAS TO BE > 0
            Log.e(TAG, "Bad Sequence number on TP DATA packet = " + ((int) data[0]));
            return;
        }

        // copy the data from this frame to the data array
        startingByte = (((int)data[0]-1) & 0xFF) * 7;
        for (int i=0; i < 7; i++) { // there are always 7 bytes in the frame
            if ((startingByte+i) < connections[connectionID].messageSizeInBytes)
                connections[connectionID].data[startingByte+i] = data[1+i];
        }

        // Have we received all the data ? If so then send EOM, otherwise send CTS
        if ((((int) data[0]) & 0xFF) == connections[connectionID].expectedPacketsNum){
            // last packet
            // Do whatever we want to do with the data
            processConnectionData(connectionID);
            Log.d(TAG, "Yooohooo! Processed data!");

            // Tell the remote to close connection. (Do this after processing so it doesn't open a new one and overwrite)
            int pgn = connections[connectionID].pgn;
            int expected_bytes = connections[connectionID].messageSizeInBytes;
            int expected_packets = connections[connectionID].expectedPacketsNum;

            removeConnection(connectionID);


            if (sendResponse) {
                sendConnectEOM(portNumber, sAddress, pgn, expected_bytes, expected_packets);
            }
        }
        else {
            // more packets to come
            if (sendResponse) {
                sendCTS(portNumber, sAddress,
                        connections[connectionID].pgn,
                        connections[connectionID].maxPacketsPerBurst,
                        ((((int) data[0]) & 0xFF) + 1) // current packet number
                );
            }
        }
    }

    int findOpenConnection(int source_address, int destination_address) {
        for (int i=0 ; i<5; i++) {
            if ((connections[i].sourceAdd == source_address) && (connections[i].destinationAdd == destination_address) && (connections[i].expectedPacketsNum != 0))  {
                return i; // index of the open connection
            }
        }
        // If there is no open connection, then connection ID= -1;
        return -1;
    }

    boolean isConnectionIdAvailable(int connectionID) {
        return connections[connectionID].expectedPacketsNum == 0;
    }

    void removeConnection(int connectionID) {
        connections[connectionID].expectedPacketsNum = 0;
        Log.d(TAG, "###### Removed Connection! Connection ID=" + connectionID+ " ########");

    }

    /*
    * Process connection data that is received
    * */
    void processConnectionData(int connectionID) {
        int pgn, pduFormat=0, pduSpecific=0;
        pgn=connections[connectionID].pgn;
        pduFormat=pgn << 8;
        pduSpecific = pgn & 0xFF;
        parsePgn(connections[connectionID].pgn,pduFormat, pduSpecific, connections[connectionID].sourceAdd, connections[connectionID].data, connections[connectionID].messageSizeInBytes);
        Log.v(TAG, "data <-- " + String.format("%02X to %02X (%04X)", connections[connectionID].sourceAdd, connections[connectionID].destinationAdd,connections[connectionID].pgn));
    }


    public void parsePgn(int pgn, int pduFormat, int pduSpecific, int srcAddr, byte[] data, int data_length){

        int totalSecs=0;
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        switch (pgn){

            case J1939_PGN_ODOMETER_LOW:
                // Vehicle Distance
                // Bytes [1-4] = Trip Distance
                // Bytes [5-8] = Total Vehicle Distance [Odometer]
                // Trip Distance range: 0 to 526,385,151.9 km [0.125 km/bit]
                // Total Vehicle Distance range: 0 to 526,385,151.9 km [0.125 km/bit]

                ByteBuffer odometerValue = ByteBuffer.wrap(data, 4, 4);
                odometerValue.order(ByteOrder.LITTLE_ENDIAN);
                int odometer = (int) ((odometerValue.getInt() & 0xffffffff)*0.125);
                txtGetLowOdometer = odometer;
                Log.d(TAG, "Odometer: " + odometer + " km");
                break;

            case J1939_PGN_ODOMETER_HIGH:
                // Vehicle Distance
                // Bytes [1-4] = Trip Distance
                // Bytes [5-8] = Total Vehicle Distance [Odometer]
                // Trip Distance range: 0 to 21,055,406 km [5 m/bit] [Unit = m]
                // Total Vehicle Distance range: 0 to 21,055,406 km [5 m/bit] [Unit = m]

                ByteBuffer highOdometerValue = ByteBuffer.wrap(data, 4, 4);
                highOdometerValue.order(ByteOrder.LITTLE_ENDIAN);
                int highOdometer = (int) (((highOdometerValue.getInt() & 0xffffffff)*5)/0.000621371);
                txtGetHighOdometer = highOdometer;
                Log.d(TAG, "Odometer: " + highOdometer + " miles");
                break;

            case J1939_PGN_VIN_NUMBER:
                // The ECU uses Transport Protocol to transmit the VIN

                String s = new String(data, 0 , data_length);
                txtRequestVin=s;
                Log.d(TAG, "VIN number: "+ s);
                break;

            case J1939_PGN_ENGINE_HOURS_REVOLUTIONS:
                // Engine Hours, Revolutions
                // Bytes [1-4] = Total Engine Hours
                // Bytes [5-8] = Total Engine Revolutions]
                // Total engine hours range: 0 to 210554060.75h [0.05h/bit].
                // Total engine revolutions range: 0 to 4211081215000r [1000r/bit]

                ByteBuffer engineTotalHours=ByteBuffer.wrap(data, 0,4);
                engineTotalHours.order(ByteOrder.LITTLE_ENDIAN);
                int engineHours = engineTotalHours.getInt() & 0xffffffff;
                int totalEngineHours= (int) (engineHours*0.05);
                totalSecs=totalEngineHours;
                hours = totalSecs / 3600;
                minutes = (totalSecs % 3600) / 60;
                seconds = totalSecs % 60;
                txtRequestEngineHours= "Calculated value: " +engineHours + " Time - "+ hours + " hours " + minutes +" mins " + seconds + " seconds";
                Log.d(TAG, "Engine Hours, Revolutions: " + txtRequestEngineHours);
                break;

            case J1939_PGN_FUEL_CONSUMPTION:
                //Fuel Consumption
                // Bytes 1-4 = Trip fuel
                // Bytes 5-8 = Total fuel
                // 0.5 L per bit (Liquid Fuel)
                ByteBuffer totalFuelConsumption = ByteBuffer.wrap(data, 4, 4);
                totalFuelConsumption.order(ByteOrder.LITTLE_ENDIAN);
                int fuelConsumption = totalFuelConsumption.getInt() & 0xffffffff;
                int calculatedFuelConsumption= (int) (fuelConsumption*0.5);
                Log.d(TAG, "Fuel Consumption: "  + calculatedFuelConsumption + " L");
                break;

            case J1939_PGN_GEAR:
                // ETC2 message
                // Byte 1 : selected gear
                // Byte 2-3: Actual gear ratio
                // byte 4: Current gear
                // byte 5-6 transmission requested range
                // byte 7-8 transmission actual range
                // Gear:
                //      251 = Park
                //      125 = Neutral
                //      > 125 is forward gears
                //      < 125 is reverse gears

                ByteBuffer transmissionGear = ByteBuffer.wrap(data, 3, 1);
                int gear = transmissionGear.get() & 0xff;
                txtTransmissionGear = gear;
                Log.d(TAG, "Transmission Gear: " + gear);
                break;

            case J1939_PGN_PARKING:
                // Cruise Control / Vehicle Speed
                // Bytes [ 2-3 ] = Wheel Based Speed
                // Wheel-Based Vehicle Speed range: 0 to 250.996km/h [1/256km/h/bit]

                ByteBuffer vehicleSpeed = ByteBuffer.wrap(data, 1, 2);
                vehicleSpeed.order(ByteOrder.LITTLE_ENDIAN);
                int wheelBasedVehicleSpeed = (vehicleSpeed.getShort() & 0xffff)* (1/256);
                txtGetVehicleSpeed = wheelBasedVehicleSpeed;
                Log.d(TAG, "Vehicle Speed: " + wheelBasedVehicleSpeed + " km/h");
                break;

            case J1939_ENGINE_CONTROLLER1:
                // ELECTRONIC ENGINE CONTROLLER #1: EEC1
                // Byte [1] (4bits) = Status EEC1 [SPN 899]
                // Byte [2] = Driver's demand engine - percent torque [SPN 512]
                // Byte [3] = Actual Engine-Percent Torque [SPN 513]
                // Byte [4-5] = Engine Speed [SPN 190]
                // Byte [6-8] = Not defined [0xFF}
                // Actual engine - percent torque range: -125% to 125% [1%/bit]
                // Engine speed: 0 to 8031.875rpm 0.125rpm/bit
                // All other positions that aren't defined above are not implemented yet!

                ByteBuffer engineSpeed = ByteBuffer.wrap(data, 3, 2);
                engineSpeed.order(ByteOrder.LITTLE_ENDIAN);
                int engine = (int) ((engineSpeed.getShort() & 0xffff)*0.125);
                Log.d(TAG, "Engine Speed: " + engine + " rpm");
                break;

            case J1939_ENGINE_CONTROLLER2:
                // ELECTRONIC ENGINE CONTROLLER #2: EEC2
                // Byte [1.1] (2bits) - Accelerator Pedal 1 Low Idle Switch
                // Byte [1.3] (2bits) - Accelerator Pedal Kickdown Switch
                // Byte [1.5] (2bits) - Road Speed Limit Status
                // Byte [1.7] (2bits) - Accelerator Pedal 2 Low Idle Switch
                // Byte [2] - Accelerator Pedal Position
                // Byte [3] - Engine Percent Load at Current Speed
                // Byte [4] - Remote Accelerator Pedal Position
                // Byte [5] - Accelerator Pedal Position 2
                // Byte [6.1] - Vehicle Acceleration Rate Limit Status
                // Accelerator Pedal Position 1: 0% to 100% [0.4%/bit]
                // Engine Percent Load at Current Speed: 0% to 125% [1%/bit]
                // All other positions that aren't defined above are not implemented yet!

                ByteBuffer throttlePos = ByteBuffer.wrap(data, 1, 1);
                int throttle = (throttlePos.get()) >> 6;
                Log.d(TAG, "Throttle Position (Accelerator Pedal Low Idle): " + throttle + " [States: 0-Off, 1-On, 2 -Error, 3- Unavailable]");
                break;


            case J1939_PGN_DASH_DISP:
                // Dash Display
                // Byte [1] = Washer Fluid Level
                // Byte [2] = Fuel level 1
                // Byte [3] = Engine Fuel Filter Differential Pressure
                // Byte [4] = Engine oil filter differential pressure
                // Byte [5-6] = Cargo ambient temperature
                // Byte [7] = Fuel 2 level
                // Washer Fluid Level: 0 to 100 % [0.4 %/bit]
                // Fuel Level 1: 0 to 100 % [0.4 %/bit]
                // Fuel Level 2: 0 to 100 % 0.4 %/bit.
                // All other positions that aren't defined above are not implemented yet!

                ByteBuffer fuelLevel = ByteBuffer.wrap(data, 1, 1);
                int level = (int) ((fuelLevel.get() & 0xffffffff)*0.4);
                Log.d(TAG, "Fuel Level: " + level + " %");
                break;


            case J1939_ENGINE_TEMPERATURE_1:
                //Engine Temperature 1
                // Byte [1] = Engine Coolant Temperature
                // Byte [2] = Engine Fuel Temperature 1
                // Byte [3-4] = Engine Oil Temperature 1
                // Byte [5-6] = Engine Turbocharger Oil
                // Byte [7] =  Engine Intercooler Temperature
                // Byte [8] = Engine Intercooler Thermostat Opening
                // Engine Coolant Temperature: -40 to 210°C [1°C/bit]
                // Engine Fuel Temperature 1: -40 to 210°C [1°C/bit]
                // Engine Oil Temperature 1: -273 to 1735°C [0.03125°C/bit]
                // All other positions that aren't defined above are not implemented yet!

                ByteBuffer engineCoolantTemp = ByteBuffer.wrap(data, 0, 1);
                int coolantTemp = (engineCoolantTemp.get() & 0xffffffff);
                Log.d(TAG, "Engine Coolant Temp: " + coolantTemp + " deg C");
                break;
        }
    }

    private class J1939Port1Reader implements Runnable {
        private volatile int canbusFrameCount = 0;
        private volatile int canbusByteCount = 0;
        private volatile int rollovers = 0;
        private volatile int maxdiff = 0;

        public int getCanbusFrameCount() {
            return canbusFrameCount;
        }

        public int getCanbusByteCount() {
            return canbusByteCount;
        }

        public int getRollovers() {
            return rollovers;
        }

        public int getMaxdiff() {
            return maxdiff;
        }

        public void clearValues() {
            canbusByteCount = 0;
            canbusFrameCount = 0;
            rollovers = 0;
            maxdiff = 0;
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
                CanbusFramePort1 canbusFrame1 = null;
                try {
                    if (blockOnReadPort1) {
                        canbusFrame1 = canbusSocket1.readPort1();
                    } else {
                        canbusFrame1 = canbusSocket1.readPort1(READ_TIMEOUT);
                    }
                    if (canbusFrame1 != null)
                    {
                        long time = SystemClock.elapsedRealtime();
                        int pgn = ((canbusFrame1.getId() & pgn_mask)  >> 8);
                        int sourceAdr=((canbusFrame1.getId() & src_addr_mask)  >> 8);
                        int pdu_format= (((canbusFrame1.getId() & pdu_format_mask) >> 16));
                        int pdu_specific=((canbusFrame1.getId() & pdu_specific_mask)  >> 8);
                        byte[] dataBytes=canbusFrame1.getData();

                        String canFrameType="";
                        if(canbusFrame1.getType() == CanbusFrameType.STANDARD){
                            canFrameType=STD;
                        }
                        else if(canbusFrame1.getType() == CanbusFrameType.EXTENDED){
                            canFrameType=EXT;
                        }
                        else if (canbusFrame1.getType() == CanbusFrameType.STANDARD_REMOTE){
                            canFrameType=STD_R;
                        }
                        else if(canbusFrame1.getType() == CanbusFrameType.EXTENDED_REMOTE){
                            canFrameType=EXT_R;
                        }

                        // done to prevent adding too much text to UI at once
                        if (can1Data.length() < 500) {
                            // avoiding string.format for performance
                            can1Data.append(time);
                            can1Data.append(",");
                            can1Data.append(Integer.toHexString(canbusFrame1.getId()));
                            can1Data.append(",");
                            can1Data.append(canFrameType);
                            can1Data.append(",");
                            can1Data.append(Integer.toHexString(pgn));
                            can1Data.append(",[");
                            can1Data.append(bytesToHex(canbusFrame1.getData()));
                            can1Data.append("] (");
                            /*can1Data.append(new String(canbusFrame1.getData()));*/
                            can1Data.append("),");
                            can1Data.append(canbusFrame1.getData().length);
                            can1Data.append("\n");
                        }

//                        if(pdu_format < 237){
//
//                            switch(pdu_format){
//
//                                case J1939_PDU_FORMAT_MANAGE_CONN:
//                                    //Rxd connection request for requested PGN
//                                    Log.d(TAG, "Check me on Port 1 ! ******** Manage Connection RXD ******** ");
//                                    rxManageConnection(2, sourceAdr, pdu_specific, dataBytes);
//                                    break;
//
//                                case J1939_PDU_FORMAT_DATA_CONN:
//                                    // Data Transfer for the requested PGN
//                                    rxConnectionData(2, sourceAdr, pdu_specific, dataBytes);
//                                    Log.d(TAG, "Check me on Port 2 ! ******** Data Transfer RXD ******** ");
//                                    break;
//                            }
//                        }
//                        else parsePgn(pgn, pdu_format, pdu_specific, sourceAdr, dataBytes, 8);

                        ++canbusFrameCount;
                        canbusByteCount += canbusFrame1.getData().length;
                    }
                    else {
                       // Log.d(TAG, "Read timeout");
                    }
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
        private volatile int rollovers = 0;
        private volatile int maxdiff = 0;

        public int getCanbusFrameCount() {
            return canbusFrameCount;
        }

        public int getCanbusByteCount() {
            return canbusByteCount;
        }

        public int getRollovers() {
            return rollovers;
        }

        public int getMaxdiff() {
            return maxdiff;
        }

        public void clearValues() {
            canbusByteCount = 0;
            canbusFrameCount = 0;
            rollovers = 0;
            maxdiff = 0;
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
            /*final int mask = 0b000_0_0_11111111_11111111_00000000;*/
            final int mask = 0b000_1_1_11111111_11111111_00000000; //Included Reserved an DP
            //final int mask =0x03FFFF00;

            while (true) {
                CanbusFramePort2 canbusFrame2 = null;
                try {
                    if (blockOnReadPort1) {
                        canbusFrame2 = canbusSocket2.readPort2();
                    } else {
                        canbusFrame2 = canbusSocket2.readPort2(READ_TIMEOUT);
                    }
                    if (canbusFrame2 != null) {
                        long time = SystemClock.elapsedRealtime();
                        int pgn = ((canbusFrame2.getId() & pgn_mask)  >> 8);
                        int sourceAdr=((canbusFrame2.getId() & src_addr_mask)  >> 8);
                        int pdu_format= (((canbusFrame2.getId() & pdu_format_mask) >> 16));
                        int pdu_specific=((canbusFrame2.getId() & pdu_specific_mask)  >> 8);
                        byte[] dataBytes=canbusFrame2.getData();
                        String canFrameType="";

                        if(canbusFrame2.getType() == CanbusFrameType.STANDARD){
                            canFrameType=STD;
                        }
                        else if(canbusFrame2.getType() == CanbusFrameType.EXTENDED){
                            canFrameType=EXT;
                        }
                        else if (canbusFrame2.getType() == CanbusFrameType.STANDARD_REMOTE){
                            canFrameType=STD_R;
                        }
                        else if(canbusFrame2.getType() == CanbusFrameType.EXTENDED_REMOTE){
                            canFrameType=EXT_R;
                        }
                        // done to prevent adding too much text to UI at once
                        if (can2Data.length() < 500) {
                            // avoiding string.format for performance
                            can2Data.append(time);
                            can2Data.append(",");
                            can2Data.append(Integer.toHexString(canbusFrame2.getId()));
                            can2Data.append(",");
                            can2Data.append(canFrameType);
                            can2Data.append(",");
                            can2Data.append(Integer.toHexString(pgn));
                            can2Data.append(",[");
                            can2Data.append(bytesToHex(canbusFrame2.getData()));
                            can2Data.append("] (");
                            //can2Data.append(new String(canbusFrame2.getData()));
                            can2Data.append("),");
                            can2Data.append(canbusFrame2.getData().length);
                            can2Data.append("\n");
                        }

                        if(pdu_format < 237){

                            switch(pdu_format){

                                case J1939_PDU_FORMAT_MANAGE_CONN:
                                    //Rxd connection request for requested PGN
                                    Log.d(TAG, "Check me on Port 2! ******** Manage Connection RXD ******** ");
                                    rxManageConnection(3, sourceAdr, pdu_specific, dataBytes);
                                    break;

                                case J1939_PDU_FORMAT_DATA_CONN:
                                    // Data Transfer for the requested PGN
                                    rxConnectionData(3, sourceAdr, pdu_specific, dataBytes);
                                    Log.d(TAG, "Check me on Port 2 ! ******** Data Transfer RXD ******** ");
                                    break;
                            }
                        }
                        else parsePgn(pgn, pdu_format, pdu_specific, sourceAdr, dataBytes, 8);

                        ++canbusFrameCount;
                        canbusByteCount += canbusFrame2.getData().length;
                    } else {
                        //Log.d(TAG, "Read timeout for Port 2");
                    }
                } catch (NullPointerException ex) {
                    // socket is null
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
    }

    public int getJ1708IntervalDelay() {
        return j1708IntervalDelay;
    }

    public void setJ1708IntervalDelay(int j1708IntervalDelay) {
        this.j1708IntervalDelay = j1708IntervalDelay;
    }

    /* Convert a byte array to a hex friendly string */
    public static String bytesToHex(byte[] bytes) {
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
        if (j1939Port1SendThread == null || !j1939Port1SendThread.isAlive()) {
            j1939Port1SendThread = new Thread(sendJ1939Port1Runnable);
            j1939Port1SendThread.start();
        }
    }

    public void sendJ1939Port1(boolean userData, String messageType, String messageId, String messageData){
        usersDataPort1 =userData;
        canMessageDataPort1 = messageData.getBytes();
        canMessageIdPort1 = Integer.parseInt(messageId);

        if (messageType.toString() == "T") {
            canMessageTypePort1 = CanbusFrameType.EXTENDED;
        }
        else if (messageType.toString() == "t") {
            canMessageTypePort1 = CanbusFrameType.STANDARD;
        }
        else if (messageType == "R") {
            canMessageTypePort1 = CanbusFrameType.EXTENDED_REMOTE;
        }
        else if (messageType == "r") {
            canMessageTypePort1 = CanbusFrameType.STANDARD_REMOTE;
        }

        if (j1939Port1SendThread == null || !j1939Port1SendThread.isAlive()) {
            j1939Port1SendThread = new Thread(sendJ1939Port1Runnablle);
            j1939Port1SendThread.start();
        }
    }

    private Runnable sendJ1939Port1Runnable = new Runnable() {
        @Override
        public void run() {
            CanbusFrameType MessageType;
            int MessageId;
            byte[] MessageData;
            do {

                MessageType= CanbusFrameType.EXTENDED;
                MessageId=J1939_FUEL_ECONOMY;
                int data = 0;
                ByteBuffer dbuf = ByteBuffer.allocate(8);
                dbuf.order(ByteOrder.LITTLE_ENDIAN);
                dbuf.putInt(data++);
                byte[] a = dbuf.array();
                a[0] = 0x12;
                a[1] = 0x34;
                a[2] = 0x45;
                a[3] = 0x67;
                a[4] = 0x1F;
                a[5] = 0x2F;
                a[6] = 0x3F;
                a[7] = 0x4F;
                MessageData=a;

                if(canbusSocket1 != null) {
                    canbusSocket1.write1939Port1(new CanbusFramePort1(MessageId, MessageData,MessageType));
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                }
            } while (autoSendJ1939Port1);
        }
    };

    private Runnable sendJ1939Port1Runnablle = new Runnable() {
        @Override
        public void run() {
            do {
                if(canbusSocket1 != null) {
                    canbusSocket1.write1939Port1(new CanbusFramePort1(canMessageIdPort1, canMessageDataPort1, canMessageTypePort1));
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                }
            } while (autoSendJ1939Port1);
        }
    };

    public void sendJ1939Port2() {
        if (j1939Port2SendThread == null || !j1939Port2SendThread.isAlive()) {
            j1939Port2SendThread = new Thread(sendJ1939Port2Runnable);
            j1939Port2SendThread.start();
        }
    }

    public void sendJ1939Port2(boolean userData, String messageType, String messageId, String messageData){
        usersDataPort2 =userData;
        canMessageDataPort2 = messageData.getBytes();
        canMessageIdPort2 = Integer.parseInt(messageId);

        if (messageType.toString() == "T") {
            canMessageTypePort2 = CanbusFrameType.EXTENDED;
        }
        else if (messageType.toString() == "t") {
            canMessageTypePort2 = CanbusFrameType.STANDARD;
        }
        else if (messageType == "R") {
            canMessageTypePort2 = CanbusFrameType.EXTENDED_REMOTE;
        }
        else if (messageType == "r") {
            canMessageTypePort2 = CanbusFrameType.STANDARD_REMOTE;
        }

        if (j1939Port2SendThread == null || !j1939Port2SendThread.isAlive()) {
            j1939Port2SendThread = new Thread(sendJ1939Port2Runnablle);
            j1939Port2SendThread.start();
        }
    }

    private Runnable sendJ1939Port2Runnable = new Runnable() {
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
                ByteBuffer dbuf = ByteBuffer.allocate(8);
                dbuf.order(ByteOrder.LITTLE_ENDIAN);
                dbuf.putInt(data++);
                byte[] a = dbuf.array();
                a[0] = 0x12;
                a[1] = 0x34;
                a[2] = 0x45;
                a[3] = 0x67;
                a[4] = 0x1F;
                a[5] = 0x2F;
                a[6] = 0x3F;
                a[7] = 0x4F;
                MessageData=a;
                if(canbusSocket2 != null) {
                    canbusSocket2.write1939Port2(new CanbusFramePort2(MessageId, MessageData,MessageType));
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                }
            } while (autoSendJ1939Port2);
        }
    };

    private Runnable sendJ1939Port2Runnablle = new Runnable() {
        @Override
        public void run() {
            do {
                if(canbusSocket2 != null) {
                    canbusSocket2.write1939Port2(new CanbusFramePort2(canMessageIdPort2, canMessageDataPort2, canMessageTypePort2));
                }
                try {
                    sleep(j1939IntervalDelay);
                } catch (InterruptedException e) {
                }
            } while (autoSendJ1939Port2);
        }
    };
    /// End J1939 methods

    
    /// J1708 Reader Thread
    public int getJ1708FrameCount() {
        return j1708Reader.getJ1708FrameCount();
    }

    public int getJ1708ByteCount() {
        return j1708Reader.getJ1708ByteCount();
    }

    private class J1708Reader implements Runnable {
        private volatile int j1708FrameCount = 0;
        private volatile int j1708ByteCount = 0;

        public int getJ1708FrameCount() {
            return j1708FrameCount;
        }

        public int getJ1708ByteCount() {
            return j1708ByteCount;
        }

        public void clearValues() {
            j1708FrameCount = 0;
            j1708ByteCount = 0;
        }


        @Override
        public void run() {
            while (true) {
                J1708Frame j1708Frame = null;
                try {
                    if (blockOnReadJ1708) {
                        j1708Frame = j1708Socket.readJ1708Port();
                    } else {
                        j1708Frame = j1708Socket.readJ1708Port(READ_TIMEOUT);
                    }

                    if (j1708Frame != null) {
                        long time = SystemClock.elapsedRealtime();
                        if (j1708Data.length() < 500) {
                            j1708Data.append(time);
                            j1708Data.append(", ");
                            j1708Data.append(Integer.toHexString(j1708Frame.getId()));
                            j1708Data.append(", [");
                            j1708Data.append(bytesToHex(j1708Frame.getData()));
                            j1708Data.append("], ");
                            j1708Data.append(j1708Frame.getData().length);
                            j1708Data.append("\n");

                        }
                        ++j1708FrameCount;
                        j1708ByteCount += j1708Frame.getData().length;
                    }
                    else {
                        Log.d(TAG, "Read timeout for J1708");
                    }
                } catch (NullPointerException ex){
                    // socket is null
                    return;
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void sendJ1708() {
            if (j1708SendThread == null || !j1708SendThread.isAlive()) {
                j1708SendThread = new Thread(sendJ1708Runnable);
                j1708SendThread.start();
            }
    }

    private Runnable sendJ1708Runnable = new Runnable() {
        @Override
        public void run() {
            int data = 0;
            do {
              /*  ByteBuffer dbuf = ByteBuffer.allocate(8);
                dbuf.order(ByteOrder.LITTLE_ENDIAN);
                dbuf.putInt(data++);
                byte[] a = dbuf.array();*/
               byte[] a = new byte[5];
                a[0] = 0x6A;
                a[1] = 0x31;
                a[2] = 0x37;
                a[3] = 0x30;
                a[4] = 0x38;
                J1708Frame frame = new J1708Frame(0x31, a);
                try {
                    if (j1708Socket != null) {
                        j1708Socket.writeJ1708Port(frame);
                    }
                    Thread.sleep(j1708IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1708);
        }
    };
    /// End J1708 functions
}
