package com.micronet_inc.smarthubsampleapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.micronet_inc.smarthubsampleapp.R;

import org.w3c.dom.Text;

import java.util.Arrays;

import micronet.hardware.LED;
import micronet.hardware.MicronetHardware;
import micronet.hardware.exception.MicronetHardwareException;

/**
 * Micronet Hardware Fragment
 */
public class HWFragment extends Fragment {

    private final String TAG = "SHMicronetHardware";
    public static HWFragment sInstance = null;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_hw_constraint, container, false);

        final Button btnSetLedState = (Button) rootView.findViewById(R.id.btnSetLedState);
        btnSetLedState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicronetHardware micronetHardware = MicronetHardware.getInstance();

                EditText editTextBrightness = (EditText) rootView.findViewById(R.id.editTextBrightness);
                EditText editTextRed = (EditText) rootView.findViewById(R.id.editTextRed);
                EditText editTextGreen = (EditText) rootView.findViewById(R.id.editTextGreen);
                EditText editTextBlue = (EditText) rootView.findViewById(R.id.editTextBlue);

                int brightness = Integer.valueOf(editTextBrightness.getText().toString());
                int red = Integer.valueOf(editTextRed.getText().toString());
                int green = Integer.valueOf(editTextGreen.getText().toString());
                int blue = Integer.valueOf(editTextBlue.getText().toString());

                try {
                    micronetHardware.setLedStatus(2, brightness, Color.rgb(red, green, blue));
                } catch (MicronetHardwareException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });

        final Button btnSetRtcDatetime = (Button) rootView.findViewById(R.id.btnSetRtcDatetime);
        btnSetRtcDatetime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicronetHardware micronetHardware = MicronetHardware.getInstance();

                EditText editTextDate = (EditText) rootView.findViewById(R.id.editTextRtcDate);
                EditText editTextTime = (EditText) rootView.findViewById(R.id.editTextRtcTime);

                String strDate = editTextDate.getText().toString().replace('/','-');
                String strTime = editTextTime.getText().toString();

                String datetime = strDate + " " + strTime + ".00";

                Log.d(TAG, "Setting datetime to " + datetime);

                try {
                    micronetHardware.setRtcDateTime(datetime);
                } catch (MicronetHardwareException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });

        final Button btnSetPowerDown = (Button) rootView.findViewById(R.id.btnSetPowerDown);
        btnSetPowerDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicronetHardware micronetHardware = MicronetHardware.getInstance();

                EditText editTextSeconds = (EditText) rootView.findViewById(R.id.editTextSetPowerDown);

                try {
                    micronetHardware.SetDelayedPowerDownTime(Integer.valueOf(editTextSeconds.getText().toString()));
                } catch (MicronetHardwareException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        sInstance = this;

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

                Log.d(TAG, Arrays.toString(calReg) +", "+ rtcDatetime +", "+ batteryState +", "+ String.valueOf(powerUpReason) +", "+ "Brightness: " + Integer.toHexString(led.BRIGHTNESS).toUpperCase() +", Color: " + Integer.toHexString(Color.rgb(led.RED, led.GREEN, led.BLUE)).toUpperCase().substring(2));

                updateTextInformation(Arrays.toString(calReg), rtcDatetime, batteryState, String.valueOf(powerUpReason), "Brightness: " + Integer.toHexString(led.BRIGHTNESS).toUpperCase() +", Color: " + Integer.toHexString(Color.rgb(led.RED, led.GREEN, led.BLUE)).toUpperCase().substring(2));

            }catch (Exception ex){
                Log.e(TAG, ex.getMessage());
            }finally {
                mHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    public void updateTextInformation(String calReg, String datetime, String batteryState, String powerUpReason, String ledState){
        TextView tvRtcCalReg = (TextView) rootView.findViewById(R.id.textRtcCalibrationRegisters);
        TextView tvRtcDatetime = (TextView) rootView.findViewById(R.id.textRtcDatetime);
        TextView tvRtcBatteryState = (TextView) rootView.findViewById(R.id.textBatteryState);
        TextView tvPowerUpReason = (TextView) rootView.findViewById(R.id.textPowerUpReason);
        TextView tvLedState = (TextView) rootView.findViewById(R.id.textLedState);

        tvRtcCalReg.setText(calReg);
        tvRtcDatetime.setText(datetime);
        tvRtcBatteryState.setText(batteryState);
        tvPowerUpReason.setText(powerUpReason);
        tvLedState.setText(ledState);
    }
}
