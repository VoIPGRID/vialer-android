package com.voipgrid.vialer.call;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.vision.text.Text;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;

/**
 * Fragment for transferring a call
 */
public class CallTransferFragment extends Fragment implements KeyPadView.OnKeyPadClickListener, View.OnClickListener {
    private static final String FRAGMENT_ARGUMENTS_ORIGINAL_CALLER_ID = "ORIGINAL_CALLER_ID";
    private static final String FRAGMENT_ARGUMENTS_ORIGINAL_CALLER_PHONE_NUMBER = "ORIGINAL_CALLER_PHONE_NUMBER";
    private static final String FRAGMENT_ARGUMENTS_SECOND_CALL_IS_CONNECTED = "SECOND_CALL_IS_CONNECTED";

    private NumberInputView mNumberInputView;
    private String mOriginalCallerPhoneNumber;
    private String mOriginalCallerId;
    private View mCallTransferKeyPadView;
    private View mCallTransferButtonsContainer;
    private CallTransferFragmentListener mCallback;
    private boolean mSecondCallConnected = false;
    private View mTransferCallButton;

    public static CallTransferFragment newInstance(String originalCallerId, String originalCaller, String secondCallisConnected) {
        CallTransferFragment fragment = new CallTransferFragment();
        Bundle arguments = new Bundle();
        arguments.putString(FRAGMENT_ARGUMENTS_ORIGINAL_CALLER_ID, originalCallerId);
        arguments.putString(FRAGMENT_ARGUMENTS_ORIGINAL_CALLER_PHONE_NUMBER, originalCaller);
        arguments.putString(FRAGMENT_ARGUMENTS_SECOND_CALL_IS_CONNECTED, secondCallisConnected);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mOriginalCallerId = getArguments().getString(FRAGMENT_ARGUMENTS_ORIGINAL_CALLER_ID);
        mOriginalCallerPhoneNumber = getArguments().getString(FRAGMENT_ARGUMENTS_ORIGINAL_CALLER_PHONE_NUMBER);
        mSecondCallConnected = Boolean.parseBoolean(getArguments().getString(FRAGMENT_ARGUMENTS_SECOND_CALL_IS_CONNECTED));
        return inflater.inflate(R.layout.fragment_call_transfer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView callTransferOnHoldNumberTextView = ((TextView) view.findViewById(R.id.call_transfer_number));

        if (!TextUtils.isEmpty(mOriginalCallerId)) {
            callTransferOnHoldNumberTextView.setText(mOriginalCallerId);
        } else {
            callTransferOnHoldNumberTextView.setText(mOriginalCallerPhoneNumber);
        }

        ViewGroup keyPadViewContainer = (ViewGroup) view.findViewById(R.id.fragment_call_transfer);
        KeyPadView keyPadView = (KeyPadView) keyPadViewContainer.findViewById(R.id.key_pad_view);
        keyPadView.setOnKeyPadClickListener(this);

        mNumberInputView = (NumberInputView) view.findViewById(R.id.number_input_edit_text);
        mNumberInputView.setOnInputChangedListener(new NumberInputView.OnInputChangedListener() {
            @Override
            public void onInputChanged(String number) {
                // This is needed to get the remove button to show up.
            }
        });

        View makeTransferCall = view.findViewById(R.id.button_call);
        makeTransferCall.setOnClickListener(this);

        View hangupButton = view.findViewById(R.id.button_transfer_hangup);
        hangupButton.setOnClickListener(this);

        mCallTransferKeyPadView = view.findViewById(R.id.call_transfer_key_pad);
        mCallTransferButtonsContainer = view.findViewById(R.id.call_transfer_buttons);

        mTransferCallButton = view.findViewById(R.id.button_transfer_call);
        mTransferCallButton.setOnClickListener(this);


        if (mSecondCallConnected) {
            mCallTransferKeyPadView.setVisibility(View.GONE);
            mCallTransferButtonsContainer.setVisibility(View.VISIBLE);
        } else {
            mTransferCallButton.setActivated(false);
            mTransferCallButton.setAlpha(0.5f);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSecondCallConnected) {
            mCallTransferKeyPadView.setVisibility(View.GONE);
            mCallTransferButtonsContainer.setVisibility(View.VISIBLE);
        }
    }

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

    @Override
    public void onDestroy() {
        mNumberInputView.clear();
        super.onDestroy();
    }

    @Override
    public void onKeyPadButtonClick(String digit, String chars) {
        mNumberInputView.add(digit);
        mNumberInputView.setCorrectFontSize();
    }

    public void secondCallIsConnected() {
        mSecondCallConnected = true;
        mTransferCallButton.setActivated(true);
        mTransferCallButton.setAlpha(1.0f);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        switch (viewId) {
            case R.id.button_call:
                String numberToCall = mNumberInputView.getNumber();
                if (!TextUtils.isEmpty(numberToCall)) {
                    mCallback.callTransferMakeSecondCall(numberToCall);

                    mCallTransferKeyPadView.setVisibility(View.GONE);
                    mCallTransferButtonsContainer.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.button_transfer_hangup:
                mCallTransferKeyPadView.setVisibility(View.VISIBLE);
                mCallTransferButtonsContainer.setVisibility(View.GONE);
                mCallback.callTransferHangupSecondCall();
                break;

            case R.id.button_transfer_call:
                if (mSecondCallConnected) {
                    mCallback.callTransferConnectTheCalls();
                }
                break;
        }
    }

    public interface CallTransferFragmentListener {
        void callTransferMakeSecondCall(String numberToCall);
        void callTransferHangupSecondCall();
        void callTransferConnectTheCalls();
    }
}
