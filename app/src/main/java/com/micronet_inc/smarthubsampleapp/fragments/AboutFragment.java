package com.micronet_inc.smarthubsampleapp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.micronet_inc.smarthubsampleapp.BuildConfig;
import com.micronet_inc.smarthubsampleapp.R;

import micronet.hardware.MicronetHardware;
import micronet.hardware.exception.MicronetHardwareException;


public class AboutFragment extends Fragment {
    private static final String TAG = "SCAboutFragment";
    View rootView;

    private IntentFilter dockFilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received dock event broadcast: " + + intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1));

            Handler handler = new Handler();
            handler.postDelayed(runnable, 2000);
//            updateInfoText();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtAbout = (TextView)rootView.findViewById(R.id.txtAppInfo);
        txtAbout.setText(String.format("Smarthub Sample App v %s" +
                "\nCopyright © 2018 Micronet Inc.\n", BuildConfig.VERSION_NAME));

        MicronetHardware mh = MicronetHardware.getInstance();
        try{
            String mcuVersion = mh.getMcuVersion();
            String fpgaVersion = mh.getFpgaVersion();
            TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("MCU Version: %s \n" +
                            "FPGA Version: %s\n" +
                            "Android OS Release: %s\n" +
                            "Android Build Number: %s\n" +
                            "Model: %s\n" +
                            "Serial: %s\n",
                    mcuVersion, fpgaVersion, Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
        }catch (MicronetHardwareException ex){
            Log.e(TAG, ex.toString());
            TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("Android OS Release: %s\n" +
                            "Android Build Number: %s\n" +
                            "Model: %s\n" +
                            "Serial: %s\n",
                    Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(broadcastReceiver, dockFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(broadcastReceiver);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            MicronetHardware mh = MicronetHardware.getInstance();
            try{
                String mcuVersion = mh.getMcuVersion();
                String fpgaVersion = mh.getFpgaVersion();
                TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
                txtDeviceInfo.setText(String.format("MCU Version: %s \n" +
                                "FPGA Version: %s\n" +
                                "Android OS Release: %s\n" +
                                "Android Build Number: %s\n" +
                                "Model: %s\n" +
                                "Serial: %s\n",
                        mcuVersion, fpgaVersion, Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
            }catch (MicronetHardwareException ex){
                Log.e(TAG, ex.toString());
                TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
                txtDeviceInfo.setText(String.format("Android OS Release: %s\n" +
                                "Android Build Number: %s\n" +
                                "Model: %s\n" +
                                "Serial: %s\n",
                        Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
            }
            Log.d(TAG, "Updated text on info tab");
        }
    };

    public void updateInfoText(){
        MicronetHardware mh = MicronetHardware.getInstance();
        try{
            String mcuVersion = mh.getMcuVersion();
            String fpgaVersion = mh.getFpgaVersion();
            TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("MCU Version: %s \n" +
                            "FPGA Version: %s\n" +
                            "Android OS Release: %s\n" +
                            "Android Build Number: %s\n" +
                            "Model: %s\n" +
                            "Serial: %s\n",
                    mcuVersion, fpgaVersion, Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
        }catch (MicronetHardwareException ex){
            Log.e(TAG, ex.toString());
            TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("Android OS Release: %s\n" +
                            "Android Build Number: %s\n" +
                            "Model %s\n" +
                            "Serial: %s\n",
                    Build.DISPLAY, Build.VERSION.RELEASE, Build.MODEL, Build.SERIAL));
        }
        Log.d(TAG, "Updated text on info tab");
    }

    public void getVersions(){
        MicronetHardware mh = MicronetHardware.getInstance();
        try{
            String mcuVersion = mh.getMcuVersion();
            String fpgaVersion = mh.getFpgaVersion();
            TextView txtDeviceInfo = (TextView)rootView.findViewById(R.id.txtDeviceInfo);
            txtDeviceInfo.setText(String.format("MCU Version %s \nFPGA Version %s\n Kernel Version ----, Android OS Version ----",
                    mcuVersion, fpgaVersion ));
        }catch (MicronetHardwareException ex){
            Log.e(TAG, ex.toString());
        }
    }


}
