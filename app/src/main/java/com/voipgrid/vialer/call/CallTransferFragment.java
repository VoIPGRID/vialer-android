package com.voipgrid.vialer.call;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Fragment for transferring a call
 */
public class CallTransferFragment extends Fragment {
    public static final String ORIGINAL_CALLER_ID = "ORIGINAL_CALLER_ID";
    public static final String ORIGINAL_CALLER_PHONE_NUMBER = "ORIGINAL_CALLER_PHONE_NUMBER";
    public static final String SECOND_CALL_IS_CONNECTED = "SECOND_CALL_IS_CONNECTED";

    private String mOriginalCallerPhoneNumber;
    private String mOriginalCallerId;
    private CallTransferFragmentListener mCallback;
    private boolean mSecondCallConnected = false;

    @BindView(R.id.call_transfer_number) TextView mTransferNumberTv;
    @BindView(R.id.call_transfer_buttons) View mCallTransferButtonsContainer;
    @BindView(R.id.button_transfer_call) View mTransferCallButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_transfer, container, false);
        ButterKnife.bind(this, view);
        mOriginalCallerId = getArguments().getString(ORIGINAL_CALLER_ID);
        mOriginalCallerPhoneNumber = getArguments().getString(ORIGINAL_CALLER_PHONE_NUMBER);
        mSecondCallConnected = Boolean.parseBoolean(getArguments().getString(SECOND_CALL_IS_CONNECTED));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!TextUtils.isEmpty(mOriginalCallerId)) {
            mTransferNumberTv.setText(mOriginalCallerId);
        } else {
            mTransferNumberTv.setText(mOriginalCallerPhoneNumber);
        }

        if (mSecondCallConnected) {
            mCallTransferButtonsContainer.setVisibility(View.VISIBLE);
        } else {
            mTransferCallButton.setActivated(false);
            mTransferCallButton.setAlpha(0.5f);
        }

        mCallTransferButtonsContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSecondCallConnected) {
            mCallTransferButtonsContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Use the deprecated version of the onAttach method for api levels < 23
     * The old android 4.1.2 API level 16 calls this method.
     * If don't use this function no callback will be set.
     *
     * @param activity Activity the attached activity/
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (CallTransferFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement CallKeyPadFragmentListener");
        }
    }

    public void secondCallIsConnected() {
        mSecondCallConnected = true;
        mTransferCallButton.setActivated(true);
        mTransferCallButton.setAlpha(1.0f);
    }

    @OnClick(R.id.button_transfer_call)
    public void startTransferButtonWasClicked(View view) {
        if (mSecondCallConnected) {
            mCallback.callTransferConnectTheCalls();
        }
    }

    @OnClick(R.id.button_transfer_hangup)
    public void hangupButtonWasClicked(View view) {
        mCallTransferButtonsContainer.setVisibility(View.GONE);
        mCallback.callTransferHangupSecondCall();
    }

    public interface CallTransferFragmentListener {
        void callTransferHangupSecondCall();
        void callTransferConnectTheCalls();
    }
}