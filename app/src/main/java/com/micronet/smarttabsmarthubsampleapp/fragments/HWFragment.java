/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.micronet.smarttabsmarthubsampleapp.R;

import java.util.Arrays;

import java.util.Objects;
import micronet.hardware.LED;
import micronet.hardware.MicronetHardware;

/**
 * Micronet Hardware Fragment
 */
public class HWFragment extends Fragment {

    private final String TAG = "SHMicronetHardware";
    public static long POLLING_INTERVAL_MS = 10000;
    private Handler mHandler = null;

    View rootView;

    public HWFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_hw, container, false);

        final Button btnSetLedState = rootView.findViewById(R.id.btnSetLedState);
        btnSetLedState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicronetHardware micronetHardware = MicronetHardware.getInstance();

                EditText editTextBrightness = rootView.findViewById(R.id.editTextBrightness);
                EditText editTextRed = rootView.findViewById(R.id.editTextRed);
                EditText editTextGreen = rootView.findViewById(R.id.editTextGreen);
                EditText editTextBlue = rootView.findViewById(R.id.editTextBlue);

                int brightness = Integer.valueOf(editTextBrightness.getText().toString());
                int red = Integer.valueOf(editTextRed.getText().toString());
                int green = Integer.valueOf(editTextGreen.getText().toString());
                int blue = Integer.valueOf(editTextBlue.getText().toString());

                if (brightness < 0 || brightness > 255 || red < 0 || red > 255 || green < 0
                        || green > 255 || blue < 0 || blue > 255) {
                    Toast.makeText(Objects.requireNonNull(getContext()).getApplicationContext(),
                            "Values must be between 0-255", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        micronetHardware.setLedStatus(2, brightness, Color.rgb(red, green, blue));
                        Toast.makeText(Objects.requireNonNull(getContext()).getApplicationContext(),
                                "Set LED State",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        });

        final Button btnSetRtcDatetime = rootView.findViewById(R.id.btnSetRtcDatetime);
        btnSetRtcDatetime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                MicronetHardware micronetHardware = MicronetHardware.getInstance();
//
//                EditText editTextDate = (EditText) rootView.findViewById(R.id.editTextRtcDate);
//                EditText editTextTime = (EditText) rootView.findViewById(R.id.editTextRtcTime);
//
//                String strDate = editTextDate.getText().toString().replace('/','-');
//                String strTime = editTextTime.getText().toString();
//
//                String datetime = strDate + " " + strTime + ".00";
//
//                Log.d(TAG, "Setting datetime to " + datetime);
//
//                try {
//                    micronetHardware.setRtcDateTime(datetime);
//                    Toast.makeText(getContext().getApplicationContext(), "Setting datetime to " + datetime, Toast.LENGTH_SHORT).show();
//                } catch (Exception e) {
//                    Log.e(TAG, e.toString());
//                }
            }
        });

        final Button btnSetPowerDown = rootView.findViewById(R.id.btnSetPowerDown);
        btnSetPowerDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicronetHardware micronetHardware = MicronetHardware.getInstance();

                EditText editTextSeconds = rootView.findViewById(R.id.editTextSetPowerDown);

                int seconds = Integer.valueOf(editTextSeconds.getText().toString());

                try {
                    micronetHardware.SetDelayedPowerDownTime(seconds);
                    Toast.makeText(Objects.requireNonNull(getContext()).getApplicationContext(),
                            "Shutting down device in " + seconds + " seconds", Toast.LENGTH_SHORT)
                            .show();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });

        final Button btnDate = rootView.findViewById(R.id.btnDate);
        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        final Button btnTime = rootView.findViewById(R.id.btnTime);
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        startPollingThread();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        // Stop the polling thread.
        mHandler.removeCallbacks(pollingThreadRunnable);
        Log.d(TAG, "Polling thread stopped.");
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
                MicronetHardware micronetHardware = MicronetHardware.getInstance();

                // Get information
                int[] calReg = micronetHardware.getRtcCalReg();
                String rtcDatetime = micronetHardware.getRtcDateTime();
                String batteryState = micronetHardware.checkRtcBattery();
                int powerUpReason = micronetHardware.getPowerUpIgnitionState();
                LED led = micronetHardware.getLedStatus(2);

                Log.d(TAG, Arrays.toString(calReg) + ", " + rtcDatetime + ", " + batteryState + ", "
                        + String.valueOf(powerUpReason) + ", " + "Brightness: " + Integer
                        .toHexString(led.BRIGHTNESS).toUpperCase() + ", Color: " + Integer
                        .toHexString(Color.rgb(led.RED, led.GREEN, led.BLUE)).toUpperCase()
                        .substring(2));

                updateTextInformation(Arrays.toString(calReg), rtcDatetime, batteryState,
                        String.valueOf(powerUpReason),
                        "Brightness: " + Integer.toHexString(led.BRIGHTNESS).toUpperCase()
                                + ", Color: " + Integer
                                .toHexString(Color.rgb(led.RED, led.GREEN, led.BLUE)).toUpperCase()
                                .substring(2));

            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            } finally {
                mHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    public void updateTextInformation(String calReg, String datetime, String batteryState,
            String powerUpReason, String ledState) {
        TextView tvRtcCalReg = rootView.findViewById(R.id.textRtcCalibrationRegisters);
        TextView tvRtcDatetime = rootView.findViewById(R.id.textRtcDatetime);
        TextView tvRtcBatteryState = rootView.findViewById(R.id.textBatteryState);
        TextView tvPowerUpReason = rootView.findViewById(R.id.textPowerUpReason);
        TextView tvLedState = rootView.findViewById(R.id.textLedState);

        tvRtcCalReg.setText(calReg);
        tvRtcDatetime.setText(datetime);
        tvRtcBatteryState.setText(batteryState);
        tvPowerUpReason.setText(powerUpReason);
        tvLedState.setText(ledState);
    }

    public void showDatePicker() {
        DatePickerFragment newFragment = new DatePickerFragment();
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            newFragment.show(getFragmentManager(), "Date Picker");
        }
    }
}
