package com.voipgrid.vialer.call;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;

/**
 * Fragment for when the call is connected.
 */
public class CallConnectedFragment extends Fragment implements View.OnClickListener {
    public static CallConnectedFragment newInstance() {
        CallConnectedFragment fragment = new CallConnectedFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_connected, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View hangupButton = view.findViewById(R.id.button_hangup);
        hangupButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        ((CallActivity) getActivity()).hangup(view.getId());
    }
}
