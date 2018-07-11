package com.micronet_inc.smarthubsampleapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.micronet_inc.smarthubsampleapp.R;
import com.micronet_inc.smarthubsampleapp.fragments.AboutFragment;
import com.micronet_inc.smarthubsampleapp.fragments.Can1OverviewFragment;
import com.micronet_inc.smarthubsampleapp.fragments.Can2OverviewFragment;
import com.micronet_inc.smarthubsampleapp.fragments.CanbusFramesFragment;
import com.micronet_inc.smarthubsampleapp.fragments.InputOutputsFragment;
import com.micronet_inc.smarthubsampleapp.fragments.J1708Fragment;
import com.micronet_inc.smarthubsampleapp.receivers.DeviceStateReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SmartHubSampleApp";

    private static boolean ttyPortsEnumerated = false;
    private static int dockState = -1;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private DeviceStateReceiver deviceStateReceiver = new DeviceStateReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Check if tty ports are available
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if(usbManager != null) {
            HashMap<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
            for (UsbDevice device : connectedDevices.values()) {
                Log.d(TAG, "Product Name: " + device.getProductName());

                // Check if tty ports are enumerated
                if (device.getProductId() == 773 && device.getVendorId() == 5538) {
                    ttyPortsEnumerated = true;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Receive any dock events or usb events
        IntentFilter filters = new IntentFilter();
        filters.addAction(Intent.ACTION_DOCK_EVENT);
        filters.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filters.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(deviceStateReceiver,filters);
    }

    public static synchronized boolean areTtyPortsAvailable(){
        return ttyPortsEnumerated;
    }

    public static synchronized void setTtyPortsState(boolean state){
        ttyPortsEnumerated = state;
    }

    public static synchronized int getDockState(){
        return dockState;
    }

    public static synchronized void setDockState(int state){
        dockState = state;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister receiver
        unregisterReceiver(deviceStateReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        Log.d(TAG, "Configuration changed: " + newConfig.toString());
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new InputOutputsFragment(), "GPIOs");
//        adapter.addFragment(new HWFragment(), "Misc");
        adapter.addFragment(new Can1OverviewFragment(), "Can1");
        adapter.addFragment(new Can2OverviewFragment(), "Can2");
        adapter.addFragment(new CanbusFramesFragment(), "CanFrames");
        adapter.addFragment(new J1708Fragment(), "J1708");
        adapter.addFragment(new AboutFragment(), "Info");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
