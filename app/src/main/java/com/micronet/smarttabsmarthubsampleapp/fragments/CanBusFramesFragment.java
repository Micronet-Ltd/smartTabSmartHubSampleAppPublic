/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.smarttabsmarthubsampleapp.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.micronet.smarttabsmarthubsampleapp.R;
import com.micronet.smarttabsmarthubsampleapp.CanFramesViewModel;

/**
 * Created by Eemaan Siddiqi on 3/3/2017.
 */

public class CanBusFramesFragment extends Fragment {
    private TextView lvJ1939Port1Frames;
    private TextView lvJ1939Port2Frames;
    private CanFramesViewModel mCanFramesViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void subscribeCanFrames(){
        mCanFramesViewModel.can1FramesRxStr.observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String can1FramesRx) {
                if (can1FramesRx != null && mCanFramesViewModel.can1FramesRxStr.getValue().length() != 0){
                    lvJ1939Port1Frames.setText(mCanFramesViewModel.can1FramesRxStr.getValue());

                    //we want to limit the maximum number of frames being stored in lvJ1939Port1Frames
                    //the side effect of this is that the frames screen could go blank a little bit
                    if (mCanFramesViewModel.can1FramesRxStr.getValue().length() > 5000){
                        lvJ1939Port1Frames.setText("");
                        mCanFramesViewModel.can1FramesRxStr.setValue("");
                    }
                }
            }
        });

        mCanFramesViewModel.can2FramesRxStr.observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String can2FramesRx) {
                if (can2FramesRx != null && mCanFramesViewModel.can2FramesRxStr.getValue().length() != 0){
                    lvJ1939Port2Frames.setText(mCanFramesViewModel.can2FramesRxStr.getValue());

                    //we want to limit the maximum number of frames being stored in lvJ1939Port1Frames
                    //the side effect of this is that the frames screen could go blank a little bit
                    if (mCanFramesViewModel.can2FramesRxStr.getValue().length() > 5000){
                        lvJ1939Port2Frames.setText("");
                        mCanFramesViewModel.can2FramesRxStr.setValue("");
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_can_frames, container, false);

        mCanFramesViewModel = ViewModelProviders.of(getActivity()).get(CanFramesViewModel.class);

        lvJ1939Port1Frames = view.findViewById(R.id.lvJ1939FramesPort1);
        lvJ1939Port1Frames.setMovementMethod(new ScrollingMovementMethod());
        lvJ1939Port2Frames = view.findViewById(R.id.lvJ1939FramesPort2);
        lvJ1939Port2Frames.setMovementMethod(new ScrollingMovementMethod());

        subscribeCanFrames();
        return view;
    }
}
