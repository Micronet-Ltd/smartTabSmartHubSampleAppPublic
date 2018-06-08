package com.micronet_inc.smarthubsampleapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.micronet_inc.smarthubsampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InputOutputsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InputOutputsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InputOutputsFragment extends Fragment {

    public InputOutputsFragment() {
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
        return inflater.inflate(R.layout.fragment_input_outputs, container, false);
    }
}
