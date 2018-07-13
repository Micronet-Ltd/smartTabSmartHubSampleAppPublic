package com.micronet_inc.smarthubsampleapp.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.micronet_inc.smarthubsampleapp.BuildConfig;
import com.micronet_inc.smarthubsampleapp.R;

import micronet.hardware.MicronetHardware;
import micronet.hardware.exception.MicronetHardwareException;

public class AboutFragment extends Fragment {

    private static final String TAG = "SCAboutFragment";
    private View rootView;

    private IntentFilter dockFilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received dock event broadcast: " + intent
                    .getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1));
        }
    };

    public AboutFragment() {
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
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtAbout = rootView.findViewById(R.id.txtAppInfo);
        txtAbout.setText(String.format("Smarthub Sample App v %s\n" +
                "Copyright © 2018 Micronet Inc.\n", BuildConfig.VERSION_NAME));

        updateInfoText();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        Activity activity = getActivity();
        if (activity != null) {
            activity.registerReceiver(broadcastReceiver, dockFilter);
            activity.registerReceiver(mUsbReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Activity activity = getActivity();
        if (activity != null) {
            activity.unregisterReceiver(broadcastReceiver);
            activity.unregisterReceiver(mUsbReceiver);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "Intent: " + action);
                updateInfoText();
            } else { // ACTION_USB_DEVICE_DETACHED
                Log.d(TAG, "Intent: " + action);
                updateInfoText();
            }
        }
    };

    @SuppressLint("HardwareIds")
    public void updateInfoText() {
        MicronetHardware mh = MicronetHardware.getInstance();
        try {
            String mcuVersion = mh.getMcuVersion();
            String fpgaVersion = mh.getFpgaVersion();
            TextView txtDeviceInfo = rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("MCU Version: %s \n" +
                            "FPGA Version: %s\n" +
                            "Android OS Release: %s\n" +
                            "Android Build Number: %s\n" +
                            "Model: %s\n" +
                            "Serial: %s\n",
                    mcuVersion, fpgaVersion, Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL,
                    Build.SERIAL));
        } catch (MicronetHardwareException ex) {
            Log.e(TAG, ex.toString());
            TextView txtDeviceInfo = rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("Android OS Release: %s\n" +
                            "Android Build Number: %s\n" +
                            "Model %s\n" +
                            "Serial: %s\n",
                    Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
        }
        Log.d(TAG, "Updated text on info tab");
    }
}
