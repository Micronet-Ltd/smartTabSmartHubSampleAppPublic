package com.micronet.smarttabsmarthubsampleapp.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.micronet.smarttabsmarthubsampleapp.BuildConfig;
import com.micronet.smarttabsmarthubsampleapp.R;

import com.micronet.smarttabsmarthubsampleapp.receivers.DeviceStateReceiver;
import micronet.hardware.MicronetHardware;
import micronet.hardware.exception.MicronetHardwareException;

public class AboutFragment extends Fragment {

    private static final String TAG = "SHAboutFragment";
    private View rootView;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtAbout = rootView.findViewById(R.id.txtAppInfo);
        txtAbout.setText(String.format("SmartTab/SmartHub Sample App v %s\n" +
                "Copyright © 2018 Micronet Inc.\n", BuildConfig.VERSION_NAME));

        updateInfoText();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, DeviceStateReceiver.getLocalIntentFilter());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case DeviceStateReceiver.portsAttachedAction:
                        Handler handler = new Handler();
                        handler.postDelayed(updateTextRunnable, 2000);
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case DeviceStateReceiver.portsDetachedAction:
                        updateInfoText();
                        Log.d(TAG, "Ports detached event received");
                        break;
                }
            }
        }
    };

    Runnable updateTextRunnable = new Runnable() {
        @Override
        public void run() {
            updateInfoText();
        }
    };

    @SuppressLint("HardwareIds")
    public void updateInfoText() {
        MicronetHardware mh = MicronetHardware.getInstance();
        try {
            String mcuVersion = mh.getMcuVersion();
            String fpgaVersion = mh.getFpgaVersion();
            TextView txtDeviceInfo = rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("OS Version: %s \n" +
                            "MCU Version: %s\n" +
                            "FPGA Version: %s\n" +
                            "Android Build Version: %s\n" +
                            "Device Model: %s\n" +
                            "Serial: %s\n",
                    Build.DISPLAY, mcuVersion, fpgaVersion, Build.VERSION.RELEASE, Build.MODEL,
                    Build.SERIAL));
        } catch (MicronetHardwareException ex) {
            Log.e(TAG, ex.toString());
            TextView txtDeviceInfo = rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("OS Version: %s\n" +
                            "Android Build Version: %s\n" +
                            "Device Model: %s\n" +
                            "Serial: %s\n",
                    Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
        }
        Log.d(TAG, "Updated text on info tab");
    }
}
