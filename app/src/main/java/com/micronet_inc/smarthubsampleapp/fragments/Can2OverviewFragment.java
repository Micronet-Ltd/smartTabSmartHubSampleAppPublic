package com.micronet_inc.smarthubsampleapp.fragments;

import static java.lang.Thread.sleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.micronet_inc.smarthubsampleapp.CanTest;
import com.micronet_inc.smarthubsampleapp.R;
import java.util.Calendar;
import java.util.Date;

public class Can2OverviewFragment extends Fragment {

    private final String TAG = "Can2OverviewFragment";
    private Date LastCreated;
    private Date LastClosed;

    private int BITRATE_250K = 250000;
    private int BITRATE_500K = 500000;
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

    private TextView textViewFrames;

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
    private boolean reopenCANOnDockEvent = false;
    private IntentFilter dockFilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
    private DockStateReceiver dockStateReceiver = new DockStateReceiver();

    public Can2OverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canTest = CanTest.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        //mDockState = null;
        getActivity().registerReceiver(dockStateReceiver, dockFilter);
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(dockStateReceiver);
        super.onPause();
        Log.d(TAG, "onPause");
    }

    private void setStateSocketDependentUI() {
        boolean open = canTest.isPort2SocketOpen();
        btnTransmitCan2.setEnabled(open);
        swCycleTransmitJ1939Can2.setEnabled(open);
        seekBarJ1939SendCan2.setEnabled(open);
    }

    private void setStateInterfaceDependentUI() {
        boolean open = canTest.isCan2InterfaceOpen();
        //btnGetBaudrateCam.setEnabled(open);

    }

    private void updateInterfaceStatusUI(String status) {
        final TextView txtInterfaceStatus = getView().findViewById(R.id.textCan2InterfaceStatus);
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

        final TextView txtSocketStatus = getView().findViewById(R.id.textCan2SocketStatus);
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

        final View rootView = getView();

        textViewFrames = rootView.findViewById(R.id.textViewCan2Frames);

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
                    txtCanTxSpeedCan2.setText(progress + "ms");
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
        setStateInterfaceDependentUI();
        setStateSocketDependentUI();
    }

    private void openCan2Interface(){
        canTest.setRemoveCan2InterfaceState(false);
        canTest.setBaudrate(baudRateSelected);
        canTest.setPortNumber(3);
        canTest.setSilentMode(silentMode);
        canTest.setTermination(termination);
        canTest.setRemoveCan2InterfaceState(false);
		canTest.setFiltersEnabled(filtersEnabled);
        canTest.setFlowControlEnabled(flowControlEnabled);
        executeChangeBaudrate();
    }

    private void closeCan2Interface(){
        canTest.setRemoveCan2InterfaceState(true);
        executeChangeBaudrate();
    }

    private void
    executeChangeBaudrate() {
        if (changeBaudRateTask == null || changeBaudRateTask.getStatus() != AsyncTask.Status.RUNNING) {
            changeBaudRateTask = new ChangeBaudRateTask( silentMode , baudRateSelected, termination, canTest.getPortNumber(), filtersEnabled, flowControlEnabled);
            changeBaudRateTask.execute();
        }
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void updateCountUI() {
        if (canTest != null){
            String s = "J1939 Frames/Bytes: " + canTest.getPort2CanbusFrameCount() + "/" + canTest.getPort2CanbusByteCount();
            swCycleTransmitJ1939Can2.setChecked(canTest.isAutoSendJ1939Port2());
            textViewFrames.setText(s);
        }

    }

    private void updateBaudRateUI() {
        String baudrateDesc = getString(R.string._000k_desc);
        if (canTest.getBaudrate() == BITRATE_250K) {
            baudrateDesc = getString(R.string._250k_desc);
        } else if (canTest.getBaudrate() == BITRATE_500K) {
            baudrateDesc = getString(R.string._500k_desc);
        }
        txtCanBaudRateCan2.setText(baudrateDesc);
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

        if (updateUIThread != null && !updateUIThread.isAlive()) {
            updateUIThread.start();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_can2_overview, container, false);
    }

    private class ChangeBaudRateTask extends AsyncTask<Void, String, Void> {

        int baudrate;
        boolean silent;
        boolean termination;
        int port;
        boolean removeInterface;
		boolean filtersEnabled;
        boolean flowControlEnabled;

        public ChangeBaudRateTask(boolean silent,int baudrate,boolean termination, int port, boolean filtersEnabled, boolean flowControlEnabled) {
            this.baudrate = baudrate;
            this.silent = silent;
            this.termination=termination;
            this.port=port;
            this.removeInterface=canTest.getRemoveCan2InterfaceState();
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
            }
            if(removeInterface==true){
                return null;
            }

            publishProgress("Opening, please wait...");
            int ret = canTest.CreateCanInterface2(silent, baudrate, termination, port, filtersEnabled, flowControlEnabled);
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
            setStateInterfaceDependentUI();
        }

        protected void onPostExecute(Void result) {
            updateBaudRateUI();
            updateInterfaceTime();
            startUpdateUIThread();
            updateInterfaceStatusUI();
            setStateInterfaceDependentUI();
            setStateSocketDependentUI();
        }
    }

    private class DockStateReceiver extends BroadcastReceiver {
        private CanTest canTest;
        public final String TAG = getClass().getSimpleName();
        public DockStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
            try {
                updateCradleIgnState();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateCradleIgnState() throws InterruptedException {
        String cradleStateMsg, ignitionStateMsg;
        switch (mDockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                if (canTest.isCan2InterfaceOpen()){
                    Toast.makeText(getContext().getApplicationContext(), "closing Can2 port since device was undocked", Toast.LENGTH_SHORT).show();
                    closeCan2Interface();
                    reopenCANOnDockEvent = true;
                }
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                //ignitionStateMsg = getString(R.string.ignition_off_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                if (reopenCANOnDockEvent){
                    Toast.makeText(getContext().getApplicationContext(), "Reopening Can2 port since device was docked", Toast.LENGTH_SHORT).show();
                    sleep(4000);
                    openCan2Interface();
                    reopenCANOnDockEvent = false;
                }
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_on_state_text);
                if (reopenCANOnDockEvent){
                    Toast.makeText(getContext().getApplicationContext(), "Reopening Can2 port since device was docked", Toast.LENGTH_SHORT).show();
                    sleep(4000);
                    openCan2Interface();
                    reopenCANOnDockEvent = false;
                }
                break;
            default:
                /* this state indicates un-defined docking state */
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
        }

        TextView cradleStateTextview = (TextView) getView().findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextview = (TextView) getView().findViewById(R.id.textViewIgnitionState);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
    }


}
