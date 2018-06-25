package com.micronet_inc.smarthubsampleapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.micronet_inc.smarthubsampleapp.fragments.InputOutputsFragment.sInstance;

public class DockStateReceiver extends BroadcastReceiver {
    public final String TAG = getClass().getSimpleName();
    public DockStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sInstance.mDockState = intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1);
        sInstance.displayCradleState();
    }
}
