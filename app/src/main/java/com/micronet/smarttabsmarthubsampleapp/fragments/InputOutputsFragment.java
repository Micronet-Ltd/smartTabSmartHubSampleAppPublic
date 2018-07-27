package com.micronet.smarttabsmarthubsampleapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.micronet.smarttabsmarthubsampleapp.R;
import com.micronet.smarttabsmarthubsampleapp.activities.MainActivity;
import com.micronet.smarttabsmarthubsampleapp.adapters.GpiAdcTextAdapter;

import com.micronet.smarttabsmarthubsampleapp.receivers.DeviceStateReceiver;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * GPIO Fragment Class
 */
public class InputOutputsFragment extends Fragment {

    private final String TAG = "InputOutputFragment";
    private static final long POLLING_INTERVAL_MS = 2000;

    private View rootView;
    private GpiAdcTextAdapter gpiAdcTextAdapter;
    private Handler handler = null;

    private int dockState = -1;

    private ToggleButton btnOutput0;
    private ToggleButton btnOutput1;
    private ToggleButton btnOutput2;
    private ToggleButton btnOutput3;

    public InputOutputsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_input_outputs, container, false);

        gpiAdcTextAdapter = new GpiAdcTextAdapter(getContext());

        GridView gridview = rootView.findViewById(R.id.gridview);
        gridview.setAdapter(gpiAdcTextAdapter);

        setUpButtons();

        Log.d(TAG, "GPIOs view created.");
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // Register for local broadcasts
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, DeviceStateReceiver.getLocalIntentFilter());
        }

        this.dockState = MainActivity.getDockState();

        // If the app begins in an undocked state then disable outputs.
        if(dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED){
            enableOutputButtons(false);
        }else{
            enableOutputButtons(true);
        }

        updateCradleIgnState();
        // Start polling thread
        startPollingThread();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        // Unregister for local broadcasts
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
        }

        // Stop the polling thread when the fragment is paused.
        if (handler != null) {
            handler.removeCallbacks(pollingThreadRunnable);
            Log.d(TAG, "Polling thread stopped.");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    private void setUpButtons() {
        final Button btnRefresh = rootView.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "GPIs and ADCs refreshed", Toast.LENGTH_SHORT).show();
            }
        });

        btnOutput0 = rootView.findViewById(R.id.toggleButtonOutput0);
        setUpOutputButton(btnOutput0, 0);

        btnOutput1 = rootView.findViewById(R.id.toggleButtonOutput1);
        setUpOutputButton(btnOutput1, 1);

        btnOutput2 = rootView.findViewById(R.id.toggleButtonOutput2);
        setUpOutputButton(btnOutput2, 2);

        btnOutput3 = rootView.findViewById(R.id.toggleButtonOutput3);
        setUpOutputButton(btnOutput3, 3);
    }

    private void setUpOutputButton(final ToggleButton toggleButton, final int output){
        toggleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(toggleButton.isChecked()){
                    changeOutputState(output, true);
                }else{
                    changeOutputState(output, false);
                }
            }
        });
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            // Handle local intent action
            if (action != null) {
                switch (action) {
                    case DeviceStateReceiver.dockAction:
                        dockState = intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1);
                        updateCradleIgnState();
                        Log.d(TAG, "Dock event received: " + dockState);
                        break;
                    case DeviceStateReceiver.portsAttachedAction:
                        handler.postDelayed(updateGpioRunnable, 2000);
                        enableOutputButtons(true);
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case DeviceStateReceiver.portsDetachedAction:
                        gpiAdcTextAdapter.populateGpisAdcs();
                        gpiAdcTextAdapter.notifyDataSetChanged();
                        enableOutputButtons(false);
                        Log.d(TAG, "Ports detached event received");
                        break;
                }
            }
        }
    };

    private void startPollingThread() {
        if (handler == null) {
            handler = new Handler();
        }
        handler.post(pollingThreadRunnable);

        Log.d(TAG, "Polling thread started.");
    }

    private final Runnable pollingThreadRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            } finally {
                // Post delayed runnable
                handler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    private final Runnable updateGpioRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    };

    private void updateCradleIgnState() {
        String cradleStateMsg, ignitionStateMsg;
        switch (dockState) {
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

        TextView cradleStateTextview = rootView.findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextview = rootView.findViewById(R.id.textViewIgnitionState);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
    }

    private void enableOutputButtons(boolean state){
        btnOutput0.setEnabled(state);
        btnOutput1.setEnabled(state);
        btnOutput2.setEnabled(state);
        btnOutput3.setEnabled(state);
    }

    private void changeOutputState(int i, boolean state) {
        int gpioNum = 700 + i;

        // If GPIO hasn't already been exported then export it
        exportGpio(gpioNum);

        // Change the state of the GPIO
        changeGpioState(gpioNum, state);
        Toast.makeText(getContext(), "Output " + i + " set " + (state ? "high": "low"), Toast.LENGTH_SHORT).show();
    }

    private void changeGpioState(int gpioNum, boolean state) {
        int gpioState = state ? 1: 0;

        try {
            String fileString = getContext().getFilesDir().getPath() + "/outputs.sh";
            Log.d(TAG, "File path is: " + fileString);

            File file = new File(fileString);
            if(file.exists()){
                boolean result = file.delete();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(fileString);
            fileOutputStream.write("#!system/bin/sh\n".getBytes());
            fileOutputStream.write(("echo " + gpioState + " > sys/class/gpio/gpio" + gpioNum + "/value\n").getBytes());
            fileOutputStream.write("echo $? > /data/data/com.micronet.smarttabsmarthubsampleapp/files/result.txt\n".getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();

            // Run shell script with op.se_dom_ex
            Runtime.getRuntime().exec(new String[]{"setprop", "op.se_dom_ex", fileString});
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void exportGpio(int gpioNum) {
        File tempFile = new File("/sys/class/gpio/gpio" + gpioNum + "/value");
        if (!tempFile.exists()) {
            // Export GPIO
            try {
                File file = new File("/sys/class/gpio/export");

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(String.valueOf(gpioNum).getBytes());

                fileOutputStream.flush();
                fileOutputStream.close();
                Log.d(TAG, "GPIO " + gpioNum + "Exported");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
