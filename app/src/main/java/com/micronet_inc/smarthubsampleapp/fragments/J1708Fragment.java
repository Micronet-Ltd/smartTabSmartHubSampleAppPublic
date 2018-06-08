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
 * {@link J1708Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link J1708Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class J1708Fragment extends Fragment {
    public J1708Fragment() {
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
        return inflater.inflate(R.layout.fragment_j1708, container, false);
    }
}
