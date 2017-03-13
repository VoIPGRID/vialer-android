package com.voipgrid.vialer.call;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.voipgrid.vialer.R;

/**
 * Fragment for when a transfer is completed.
 */
public class CallTransferCompleteFragment extends Fragment {
    public static final String CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_ID = "ORIGINAL_CALLER_ID";
    public static final String CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_PHONE_NUMBER = "ORIGINAL_CALLER_PHONE_NUMBER";
    public static final String CALL_TRANSFER_COMPLETE_FRAGMENT_TRANSFERRED_PHONE_NUMBER = "TRANSFERRED_PHONE_NUMBER";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_transfer_complete, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView originalCallerView = (TextView) view.findViewById(R.id.original_caller);
        String callerId = getArguments().getString(CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_ID);
        if (!TextUtils.isEmpty(callerId)) {
            originalCallerView.setText(callerId);
        } else {
            originalCallerView.setText(
                    getArguments().getString(CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_PHONE_NUMBER)
            );
        }

        TextView transferredCallerView = (TextView) view.findViewById(R.id.transfer_caller);
        transferredCallerView.setText(
                getArguments().getString(CALL_TRANSFER_COMPLETE_FRAGMENT_TRANSFERRED_PHONE_NUMBER)
        );

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getActivity().finish();  // Close this activity after 3 seconds.
            }
        }, 3000);
    }
}
