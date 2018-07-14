package com.micronet_inc.smarthubsampleapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.micronet_inc.smarthubsampleapp.CanTest;
import com.micronet_inc.smarthubsampleapp.R;

/**
 * Created by Eemaan Siddiqi on 3/3/2017.
 */

public class CanBusFramesFragment extends Fragment {
    private TextView lvJ1939Port1Frames;
    private TextView lvJ1939Port2Frames;
    private CanTest canTest;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canTest = CanTest.getInstance();
        mHandler = new Handler(Looper.getMainLooper());

    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler.post(updateUIRunnable);
    }

    private final Runnable updateUIRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountUI();
            mHandler.postDelayed(this, 1000);
        }
    };


    private void updateCountUI()
    {
        if(lvJ1939Port1Frames.length() > 2000) {
            lvJ1939Port1Frames.setText("");
        }
        lvJ1939Port1Frames.append(canTest.can1Data);
        canTest.can1Data.setLength(0);

        if(lvJ1939Port2Frames.length() > 2000) {
            lvJ1939Port2Frames.setText("");
        }
        lvJ1939Port2Frames.append(canTest.can2Data);
        canTest.can2Data.setLength(0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_can_frames, container, false);
        lvJ1939Port1Frames = view.findViewById(R.id.lvJ1939FramesPort1);
        lvJ1939Port1Frames.setMovementMethod(new ScrollingMovementMethod());
        lvJ1939Port2Frames = view.findViewById(R.id.lvJ1939FramesPort2);
        lvJ1939Port2Frames.setMovementMethod(new ScrollingMovementMethod());
        return view;
    }
}
