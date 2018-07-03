package com.micronet_inc.smarthubsampleapp.fragments;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.micronet_inc.smarthubsampleapp.R;
import com.micronet_inc.smarthubsampleapp.adapters.GpiAdcTextAdapter;
import com.micronet_inc.smarthubsampleapp.receivers.DockStateReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * GPIO Fragment Class
 */
public class InputOutputsFragment extends Fragment {
    public static InputOutputsFragment sInstance = null;
    View rootView;
    private final String TAG = "SHInputOutputFragment";
    public static long POLLING_INTERVAL_MS = 3000;
    private GpiAdcTextAdapter gpiAdcTextAdapter;
    private Handler mHandler = null;
    public int mDockState = -1;

    private GridView gridview;

    private IntentFilter dockFilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
    private DockStateReceiver dockStateReceiver = new DockStateReceiver();

    private static int count = 0;

    public InputOutputsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_input_outputs_constraint, container, false);

        gpiAdcTextAdapter = new GpiAdcTextAdapter(getContext().getApplicationContext());

        gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(gpiAdcTextAdapter);

        final Button btnRefresh = (Button) rootView.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
                Toast.makeText(getContext().getApplicationContext(), "GPIs and ADCs refreshed", Toast.LENGTH_SHORT).show();
            }
        });

        final ToggleButton btnOutput1 = (ToggleButton) rootView.findViewById(R.id.toggleButtonOutput1);
        btnOutput1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if(btnOutput1.isChecked()){
//                    changeOutputState(1, true);
//                }else{
//                    changeOutputState(1, false);
//                }
            }
        });

        final ToggleButton btnOutput2 = (ToggleButton) rootView.findViewById(R.id.toggleButtonOutput2);
        btnOutput2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if(btnOutput2.isChecked()){
//                    changeOutputState(2, true);
//                }else{
//                    changeOutputState(2, false);
//                }
            }
        });

        final ToggleButton btnOutput3 = (ToggleButton) rootView.findViewById(R.id.toggleButtonOutput3);
        btnOutput3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if(btnOutput3.isChecked()){
//                    changeOutputState(3, true);
//                }else{
//                    changeOutputState(3, false);
//                }
            }
        });

        final ToggleButton btnOutput4 = (ToggleButton) rootView.findViewById(R.id.toggleButtonOutput4);
        btnOutput4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if(btnOutput4.isChecked()){
//                    changeOutputState(4, true);
//                }else{
//                    changeOutputState(4, false);
//                }
            }
        });

        Log.d(TAG, "GPIOs view created.");

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        sInstance = this;
        getActivity().registerReceiver(dockStateReceiver, dockFilter);

        displayCradleState();
        startPollingThread();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(dockStateReceiver);
        sInstance = null;
        super.onPause();
        Log.d(TAG, "onPause");

        // Stop the polling thread when the fragment is paused.
        mHandler.removeCallbacks(pollingThreadRunnable);
        Log.d(TAG, "Polling thread stopped.");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    private void startPollingThread() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        mHandler.post(pollingThreadRunnable);

        Log.d(TAG, "Polling thread started.");
    }

    final Runnable pollingThreadRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
                //Toast.makeText(getContext().getApplicationContext(), "GPIs and ADCs polled", Toast.LENGTH_SHORT).show();
            }catch (Exception ex){
                Log.e(TAG, ex.getMessage());
            }finally {
                mHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    /*
     * get the current docking state
     * from the last ACTION_DOCK_EVENT sticky intent
     */
    public int getCurrentDockState() {
        int currentDockState = -1;
        /*
         * Receiving the current docking state
         */
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent intent = getActivity().registerReceiver(null, ifilter);
        if (intent != null) {
            currentDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
        }
        return currentDockState;
    }

    public void displayCradleState() {
        String cradleStateMsg, ignitionStateMsg;
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

        TextView cradleStateTextview = (TextView) rootView.findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextview = (TextView) rootView.findViewById(R.id.textViewIgnitionState);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
    }

    public void changeOutputState(int i, boolean state){

        int gpioNum = 699+i;
        int gpioState = 0;

        if(state){
            gpioState = 1;
        }

        // If GPIO hasn't already been exported then export it
        File tempFile = new File("/sys/class/gpio/gpio"+gpioNum+"/value");
        if(!tempFile.exists()){
            // Export GPIO
            try {
                File file = new File("/sys/class/gpio/export");

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(String.valueOf(gpioNum).getBytes());

                fileOutputStream.flush();
                fileOutputStream.close();
                Log.d(TAG, "GPIO " + gpioNum + "Exported");
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }


        // Change the state of the GPIO
        try {
            // Check to see what id the app is. Currently to run se_dom_ex you have to be shell.
            java.lang.Process processID = Runtime.getRuntime().exec("id");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processID.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.d(TAG, line);
            }
            bufferedReader.close();

            // DOESN'T WORK
            // Tried this with and without quotes and it doesn't seem to work as it does in the shell.
            String commandsChmod[] = {"/system/bin/se_dom_ex", "\"chmod 666 /sys/class/gpio/gpio700/value\""};
            java.lang.Process processChmod = Runtime.getRuntime().exec(commandsChmod);
            BufferedReader bufferedReaderChmod = new BufferedReader(new InputStreamReader(processChmod.getInputStream()));
            while ((line = bufferedReaderChmod.readLine()) != null) {
                Log.e(TAG, line);
            }
            bufferedReaderChmod.close();

            // This command won't work for the same reason as above but also because echo doesn't seem to work
            // properly when I try to do it this way. For example, if I try echo "hello world" using this method it
            // doesn't work either.
            String commandsEcho[] = {"/system/bin/se_dom_ex", "echo " + gpioState + "> /sys/class/gpio/gpio700/value"};
            java.lang.Process processEcho = Runtime.getRuntime().exec(commandsEcho);
            BufferedReader bufferedReaderEcho = new BufferedReader(new InputStreamReader(processEcho.getInputStream()));
            while ((line = bufferedReaderEcho.readLine()) != null) {
                Log.e(TAG, line);
            }
            Log.e(TAG, "Output " + i + " set to state " + gpioState + ". GPIO Number: " + gpioNum);
            bufferedReaderEcho.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
