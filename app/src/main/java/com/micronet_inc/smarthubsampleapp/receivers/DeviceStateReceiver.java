package com.micronet_inc.smarthubsampleapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.micronet_inc.smarthubsampleapp.activities.MainActivity;

import java.util.HashMap;

public class DeviceStateReceiver extends BroadcastReceiver {

    public final String TAG = getClass().getSimpleName();

    public DeviceStateReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Reduce lag in activity by going async
        goAsync();

        // Get the action from intent
        String action = intent.getAction();

        // Handle action
        if (Intent.ACTION_DOCK_EVENT.equals(action)) {
            int dockState = intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1);
            MainActivity.setDockState(dockState);

            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            Intent localIntent = new Intent("com.micronet.smarthubsampleapp.dockevent");
            localIntent.putExtra(android.content.Intent.EXTRA_DOCK_STATE, dockState);
            localBroadcastManager.sendBroadcast(localIntent);

            Log.d(TAG, "Dock event received: " + dockState);
        } else { // USB Attach Detach Event
            Log.d(TAG, "USB event received: " + action);
            UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

            if (mUsbManager != null) {
                HashMap<String, UsbDevice> connectedDevices = mUsbManager.getDeviceList();

                boolean portsEnumerated = false;
                boolean portsEnumeratedBefore = MainActivity.areTtyPortsAvailable();

                for (UsbDevice device : connectedDevices.values()) {
                    Log.d(TAG, "Product Name: " + device.getProductName());

                    // Check if tty ports are enumerated
                    if (device.getProductId() == 773 && device.getVendorId() == 5538) {
                        Log.d(TAG, "Interface count: " + device.getInterfaceCount());
                        UsbInterface intf = device.getInterface(0);
                        Log.d(TAG, "Endpoint count: " + intf.getEndpointCount());
                        UsbEndpoint usbEndpoint = intf.getEndpoint(0);
                        if (usbEndpoint == null) {
                            Log.wtf(TAG, "Endpoint null");
                            break;
                        }

                        portsEnumerated = true;
                    }
                }

                // Handle state
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) && portsEnumerated) {
                    if (!portsEnumeratedBefore) {
                        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                        Intent localIntent = new Intent("com.micronet.smarthubsampleapp.portsattached");
                        localBroadcastManager.sendBroadcast(localIntent);

                        MainActivity.setTtyPortsState(true);
                        Log.d(TAG, "Attach event, ports enumerated.");
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) && !portsEnumerated) {
                    Log.d(TAG, "Attach event, ports not enumerated.");
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action) && !portsEnumerated) {
                    if (portsEnumeratedBefore) {
                        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                        Intent localIntent = new Intent("com.micronet.smarthubsampleapp.portsdetached");
                        localBroadcastManager.sendBroadcast(localIntent);

                        MainActivity.setTtyPortsState(false);
                        Log.d(TAG, "Detach event, ports not enumerated.");
                    }
                } else {
                    Log.d(TAG, "Detach event, ports enumerated.");
                }
            }
        }
    }
}
