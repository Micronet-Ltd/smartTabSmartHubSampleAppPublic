package com.micronet_inc.smarthubsampleapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.micronet_inc.smarthubsampleapp.BuildConfig;
import com.micronet_inc.smarthubsampleapp.R;

public class AboutFragment extends Fragment {
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
        final View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtAbout = (TextView)rootView.findViewById(R.id.txtAbout);
        txtAbout.setText(String.format("Smarthub Sample App v %s\nCopyright © 2018 Micronet Inc.\n", BuildConfig.VERSION_NAME));
        return rootView;
    }
}
