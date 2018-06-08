package com.micronet_inc.smarthubsampleapp.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.micronet_inc.smarthubsampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CanBusFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CanBusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CanBusFragment extends Fragment {
    public CanBusFragment() {
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
        return inflater.inflate(R.layout.fragment_can_bus, container, false);
    }
}
