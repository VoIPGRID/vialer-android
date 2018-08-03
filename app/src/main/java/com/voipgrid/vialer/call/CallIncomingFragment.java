package com.voipgrid.vialer.call;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;

/**
 * Fragment for an incoming call.
 */
public class CallIncomingFragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_incoming, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // OnClickListener for the accept button.
        View acceptButton = view.findViewById(R.id.button_pickup);
        acceptButton.setOnClickListener(this);

        // OnClickListener for the decline button.
        View declineButton = view.findViewById(R.id.button_decline);
        declineButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_decline:
//                ((CallActivity) getActivity()).decline();
                break;

            case R.id.button_pickup:
                ((CallActivity) getActivity()).answer();
                break;
        }
    }
}
