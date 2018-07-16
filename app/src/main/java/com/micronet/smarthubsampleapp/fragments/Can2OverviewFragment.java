package com.micronet.smarthubsampleapp.fragments;

import static java.lang.Thread.sleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.micronet.smarthubsampleapp.CanTest;
import com.micronet.smarthubsampleapp.R;
import java.util.Calendar;
import java.util.Date;

public class Can2OverviewFragment extends Fragment {

    private final String TAG = "Can2OverviewFragment";
    private View rootView;

    private Date LastCreated;
    private Date LastClosed;

    private final int BITRATE_250K = 250000;
    private final int BITRATE_500K = 500000;
    private boolean silentMode = false;
    private boolean termination = false;
    private boolean filtersEnabled = false;
    private boolean flowControlEnabled = false;
    private int baudRateSelected = BITRATE_250K;

    private Thread updateUIThread;

    private CanTest canTest;
    private TextView txtInterfaceClsTimeCan2;
    private TextView txtInterfaceOpenTimeCan2;
    private TextView txtCanTxSpeedCan2;
    private TextView txtCanBaudRateCan2;

    private TextView textViewFramesRx;
    private TextView textViewFramesTx;

    // Socket dependent UI
    private Button btnTransmitCan2;
    private ToggleButton swCycleTransmitJ1939Can2;
    private SeekBar seekBarJ1939SendCan2;

    //Interface dependent UI
    private ToggleButton toggleButtonTermCan2;
    private ToggleButton toggleButtonListenCan2;
    private RadioGroup baudRateCan2;
    private ToggleButton toggleButtonFilterSetCan2;
    private ToggleButton toggleButtonFlowControlCan2;

    private Button openCan2;
    private Button closeCan2;

    private ChangeBaudRateTask changeBaudRateTask;

    private int mDockState = -1;
    private boolean reopenCANOnTtyAttachEvent = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canTest = CanTest.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        //mDockState = null;
        Log.d(TAG, "onResume");

        IntentFilter filters = new IntentFilter();
        filters.addAction("com.micronet.smarthubsampleapp.dockevent");
        filters.addAction("com.micronet.smarthubsampleapp.portsattached");
        filters.addAction("com.micronet.smarthubsampleapp.portsdetached");

        Context context = getContext();
        if (context != null){
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, filters);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
        }
    }

    private void setStateSocketDependentUI() {
        boolean open = canTest.isPort2SocketOpen();
        btnTransmitCan2.setEnabled(open);
        swCycleTransmitJ1939Can2.setEnabled(open);
        seekBarJ1939SendCan2.setEnabled(open);
    }

    private void setDockStateDependentUI(){
        boolean uiElementEnabled = true;
        if (mDockState == Intent.EXTRA_DOCK_STATE_UNDOCKED){
            uiElementEnabled = false;
        }
        toggleButtonTermCan2.setEnabled(uiElementEnabled);
        toggleButtonListenCan2.setEnabled(uiElementEnabled);
        baudRateCan2.setEnabled(uiElementEnabled);
        toggleButtonFilterSetCan2.setEnabled(uiElementEnabled);
        toggleButtonFlowControlCan2.setEnabled(uiElementEnabled);
        openCan2.setEnabled(uiElementEnabled);
        closeCan2.setEnabled(uiElementEnabled);
    }

    private void updateInterfaceStatusUI(String status) {
        final TextView txtInterfaceStatus = rootView.findViewById(R.id.textCan2InterfaceStatus);
        if(status != null) {
            txtInterfaceStatus.setText(status);
            txtInterfaceStatus.setBackgroundColor(Color.YELLOW);
        } else if(canTest.isCan2InterfaceOpen()) {
            txtInterfaceStatus.setText(getString(R.string.open));
            txtInterfaceStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtInterfaceStatus.setText(getString(R.string.closed));
            txtInterfaceStatus.setBackgroundColor(Color.RED);
        }

        final TextView txtSocketStatus = rootView.findViewById(R.id.textCan2SocketStatus);
        if(status != null) {
            txtSocketStatus.setText(status);
            txtSocketStatus.setBackgroundColor(Color.YELLOW);
        } else if(canTest.isPort2SocketOpen()) {
            txtSocketStatus.setText(getString(R.string.open));
            txtSocketStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtSocketStatus.setText(getString(R.string.closed));
            txtSocketStatus.setBackgroundColor(Color.RED);
        }
    }

    private void updateInterfaceStatusUI() {
        updateInterfaceStatusUI(null);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    private void openCan2Interface(){
        canTest.setRemoveCan2InterfaceState(false);
        canTest.setBaudRate(baudRateSelected);
        canTest.setPortNumber(3);
        canTest.setSilentMode(silentMode);
        canTest.setTermination(termination);
        canTest.setRemoveCan2InterfaceState(false);
		canTest.setFiltersEnabled(filtersEnabled);
        canTest.setFlowControlEnabled(flowControlEnabled);
        executeChangeBaudRate();
    }

    private void closeCan2Interface(){
        canTest.setRemoveCan2InterfaceState(true);
        executeChangeBaudRate();
    }

    private void
    executeChangeBaudRate() {
        if (changeBaudRateTask == null || changeBaudRateTask.getStatus() != AsyncTask.Status.RUNNING) {
            changeBaudRateTask = new ChangeBaudRateTask( silentMode , baudRateSelected, termination, canTest.getPortNumber(), filtersEnabled, flowControlEnabled);
            changeBaudRateTask.execute();
        }
    }

    private void updateCountUI() {
        if (canTest != null){
            String s1 = canTest.getPort2CanbusRxFrameCount() + " Frames / " + canTest.getPort2CanbusRxByteCount() + " Bytes";
            swCycleTransmitJ1939Can2.setChecked(canTest.isAutoSendJ1939Port2());
            textViewFramesRx.setText(s1);
            if (canTest.getPort2CanbusRxFrameCount() == 0){
                textViewFramesRx.setBackgroundColor(Color.WHITE);
            }
            else{
                textViewFramesRx.setBackgroundColor(Color.GREEN);
            }

            String s2 = "Tx: " + canTest.getPort2CanbusTxFrameCount() + " Frames / " + canTest.getPort2CanbusTxByteCount() + " Bytes";
            textViewFramesTx.setText(s2);
            if (canTest.getPort2CanbusTxFrameCount() == 0) {
                textViewFramesTx.setBackgroundColor(Color.WHITE);
            }
            else{
                textViewFramesTx.setBackgroundColor(Color.GREEN);
            }
        }

    }

    private void updateBaudRateUI() {
        String baudRateDesc = getString(R.string._000k_desc);
        if (canTest.getBaudRate() == BITRATE_250K) {
            baudRateDesc = getString(R.string._250k_desc);
        } else if (canTest.getBaudRate() == BITRATE_500K) {
            baudRateDesc = getString(R.string._500k_desc);
        }
        txtCanBaudRateCan2.setText(baudRateDesc);
    }

    private void updateInterfaceTime() {
        String closedDate = " None ";
        String createdDate = " None ";
        if(LastClosed != null){
            closedDate = LastClosed.toString();
        }
        if(LastCreated != null){
            createdDate = LastCreated.toString();
        }

        txtInterfaceOpenTimeCan2.setText(createdDate);
        txtInterfaceClsTimeCan2.setText(closedDate);
    }


    private void startUpdateUIThread() {
        if (updateUIThread == null) {
            updateUIThread = new Thread(new Runnable() {
                @SuppressWarnings("InfiniteLoopStatement")
                @Override
                public void run() {
                    while (true) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCountUI();
                            }
                        });
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        if (!updateUIThread.isAlive()) {
            updateUIThread.start();
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView Start");
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_can2_overview, container, false);
        textViewFramesRx = rootView.findViewById(R.id.textViewCan2FramesRx);
        textViewFramesTx = rootView.findViewById(R.id.textViewCan2FramesTx);

        baudRateCan2 = rootView.findViewById(R.id.radioGrCan2BaudRates);
        toggleButtonListenCan2 = rootView.findViewById(R.id.toggleButtonCan2Listen);
        toggleButtonTermCan2 = rootView.findViewById(R.id.toggleButtonCan2Term);
        toggleButtonFilterSetCan2 = rootView.findViewById(R.id.toggleButtonCan2Filters);
        toggleButtonFlowControlCan2 = rootView.findViewById(R.id.toggleButtonCan2FlowControl);

        openCan2 = rootView.findViewById(R.id.buttonOpenCan2);
        closeCan2 = rootView.findViewById(R.id.buttonCloseCan2);
        txtInterfaceClsTimeCan2 = rootView.findViewById(R.id.textViewCan2ClosedTime);
        txtInterfaceOpenTimeCan2 = rootView.findViewById(R.id.textViewCan2CreatedTime);
        txtCanTxSpeedCan2 = rootView.findViewById(R.id.textViewCan2CurrTransmitInterval);
        txtCanBaudRateCan2 = rootView.findViewById(R.id.textViewCan2CurrBaudRate);

        btnTransmitCan2 = rootView.findViewById(R.id.btnCan2SendJ1939);
        seekBarJ1939SendCan2 = rootView.findViewById(R.id.seekBarCan2SendSpeed);
        swCycleTransmitJ1939Can2 = rootView.findViewById(R.id.swCan2CycleTransmitJ1939);

        seekBarJ1939SendCan2.setProgress(canTest.getJ1939IntervalDelay());
        btnTransmitCan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canTest.sendJ1939Port2();
            }
        });

        baudRateCan2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.radio250K:
                        baudRateSelected = BITRATE_250K;
                        break;
                    case R.id.radio500K:
                        baudRateSelected = BITRATE_500K;
                        break;
                }
            }
        });

        toggleButtonTermCan2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                termination = isChecked;
            }
        });


        toggleButtonListenCan2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                silentMode = isChecked;
            }
        });

        toggleButtonFilterSetCan2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filtersEnabled = isChecked;
            }
        });

        toggleButtonFlowControlCan2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flowControlEnabled = isChecked;
            }
        });
        openCan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCan2Interface();
            }
        });

        closeCan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeCan2Interface();
            }
        });

        swCycleTransmitJ1939Can2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canTest.setAutoSendJ1939Port2(swCycleTransmitJ1939Can2.isChecked());
                canTest.sendJ1939Port2();
            }
        });

        seekBarJ1939SendCan2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    canTest.setJ1939IntervalDelay(progress);
                    String progressStr = String.valueOf(progress) + "ms";
                    txtCanTxSpeedCan2.setText(progressStr);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //txtCanTxSpeedCan2.setText(canTest.getJ1939IntervalDelay() + "ms");
        updateBaudRateUI();
        updateInterfaceTime();
        updateInterfaceStatusUI();
        setStateSocketDependentUI();
        setDockStateDependentUI();
        Log.d(TAG, "onCreateView end");
        return rootView;
    }

    private class ChangeBaudRateTask extends AsyncTask<Void, String, Void> {

        final int baudRate;
        final boolean silent;
        final boolean termination;
        final int port;
		final boolean filtersEnabled;
        final boolean flowControlEnabled;

        ChangeBaudRateTask(boolean silent, int baudRate, boolean termination, int port, boolean filtersEnabled, boolean flowControlEnabled) {
            this.baudRate = baudRate;
            this.silent = silent;
            this.termination=termination;
            this.port=port;
            this.filtersEnabled = filtersEnabled;
            this.flowControlEnabled = flowControlEnabled;
        }

        @Override
        protected Void doInBackground(Void... params) {
            LastClosed = Calendar.getInstance().getTime();
            if(canTest.isCan2InterfaceOpen() || canTest.isPort2SocketOpen()) {
                publishProgress("Closing interface, please wait...");
                canTest.closeCan2Interface();
                publishProgress("Closing socket, please wait...");
                canTest.closeCan2Socket();
                return null;
            }

            publishProgress("Opening, please wait...");
            int ret = canTest.CreateCanInterface2(silent, baudRate, termination, port, filtersEnabled, flowControlEnabled);
            if (ret == 0) {
                LastCreated = Calendar.getInstance().getTime();
            }
            else{
                publishProgress("Closing interface, please wait...");
                canTest.closeCan2Interface();
                publishProgress("Closing socket, please wait...");
                canTest.closeCan2Socket();
                publishProgress("failed");
            }
            return null;
        }

        protected void onProgressUpdate(String... params) {
            updateInterfaceStatusUI(params[0]);
            setStateSocketDependentUI();
            setDockStateDependentUI();
        }

        protected void onPostExecute(Void result) {
            updateBaudRateUI();
            updateInterfaceTime();
            startUpdateUIThread();
            updateInterfaceStatusUI();
            setStateSocketDependentUI();
            setDockStateDependentUI();
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "com.micronet.smarthubsampleapp.dockevent":
                        mDockState = intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1);
                        updateCradleIgnState();
                        Log.d(TAG, "Dock event received: " + mDockState);
                        break;
                    case "com.micronet.smarthubsampleapp.portsattached":
                        if (reopenCANOnTtyAttachEvent){
                            Log.d(TAG, "Reopening CAN2 port since the tty port attach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "Reopening CAN2 port since the tty port attach event was received",
                                    Toast.LENGTH_SHORT).show();
                            openCan2Interface();
                            reopenCANOnTtyAttachEvent = false;
                        }
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case "com.micronet.smarthubsampleapp.portsdetached":
                        if (canTest.isCan2InterfaceOpen()){
                            Log.d(TAG, "closing CAN2 port since the tty port detach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "closing CAN2 port since the tty port detach event was received",
                                    Toast.LENGTH_SHORT).show();
                            closeCan2Interface();
                            reopenCANOnTtyAttachEvent = true;
                        }
                        Log.d(TAG, "Ports detached event received");
                        break;
                }
            }
        }
    };

    private void updateCradleIgnState(){
        String cradleStateMsg, ignitionStateMsg;
        Log.d(TAG, "updateCradleIgnState() mDockState:" + mDockState);
        switch (mDockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                //ignitionStateMsg = getString(R.string.ignition_off_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_on_state_text);
                break;
            default:
                /* this state indicates un-defined docking state */
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
        }

        TextView cradleStateTextView = rootView.findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextView = rootView.findViewById(R.id.textViewIgnitionState);
        cradleStateTextView.setText(cradleStateMsg);
        ignitionStateTextView.setText(ignitionStateMsg);
    }

}
