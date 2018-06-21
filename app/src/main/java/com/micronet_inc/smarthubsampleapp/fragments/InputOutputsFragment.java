package com.micronet_inc.smarthubsampleapp.fragments;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.micronet_inc.smarthubsampleapp.R;
import com.micronet_inc.smarthubsampleapp.adapters.GpiAdcTextAdapter;
import com.micronet_inc.smarthubsampleapp.receivers.DockStateReceiver;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InputOutputsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InputOutputsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InputOutputsFragment extends Fragment {
    public static InputOutputsFragment sInstance = null;
    View rootView;
    private final String TAG = "SHInputOutputFragment";
    public static long POLLING_INTERVAL_MS = 10000;
    private GpiAdcTextAdapter gpiAdcTextAdapter;
    private Handler mHandler = null;
    public int mDockState = -1;

    private IntentFilter dockFilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
    private DockStateReceiver dockStateReceiver = new DockStateReceiver();

    private static int count = 0;

    public InputOutputsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (count == 0){ //TODO: hack
            startPollingThread();
            count++;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView =  inflater.inflate(R.layout.fragment_input_outputs, container, false);

        gpiAdcTextAdapter = new GpiAdcTextAdapter(getContext().getApplicationContext());

        final GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(gpiAdcTextAdapter);

        final Button btnRefresh = (Button) rootView.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
                Toast.makeText(getContext().getApplicationContext(), "GPIs and ADCs refreshed", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        sInstance = this;
        getActivity().registerReceiver(dockStateReceiver, dockFilter);
//        mDockState = getCurrentDockState();
//        gpiAdcTextAdapter.populateGpisAdcs();
//        gpiAdcTextAdapter.notifyDataSetChanged();

//        displayCradleState();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(dockStateReceiver);
        sInstance = null;
        super.onPause();
        Log.d(TAG, "onPause");
    }

    private void startPollingThread() {
        if (mHandler == null) {
            mHandler = new Handler();
            mHandler.post(pollingThreadRunnable);
        }
    }

    final Runnable pollingThreadRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                gpiAdcTextAdapter.populateGpisAdcs();
                gpiAdcTextAdapter.notifyDataSetChanged();
                Toast.makeText(getContext().getApplicationContext(), "GPIs and ADCs polled", Toast.LENGTH_SHORT).show();
            }catch (Exception ex){
                Log.d(TAG, ex.getMessage());
            }finally {
                mHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    /*
     * get the current docking state
     * from the last ACTION_DOCK_EVENT sticky intent
     */
    public int getCurrentDockState() {
        int currentDockState = -1;
        /*
         * Receiving the current docking state
         */
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent intent = getActivity().registerReceiver(null, ifilter);
        if (intent != null) {
            currentDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
        }
        return currentDockState;
    }

    public void displayCradleState() {
        String cradleStateMsg, ignitionStateMsg;
        switch (mDockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                //ignitionStateMsg = getString(R.string.ignition_off_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_on_state_text);
                break;
            default:
                /* this state indicates un-defined docking state */
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
        }

        TextView cradleStateTextview = (TextView) rootView.findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextview = (TextView) rootView.findViewById(R.id.textViewIgnitionState);
        cradleStateTextview.setText(cradleStateMsg);
        ignitionStateTextview.setText(ignitionStateMsg);
    }
}
