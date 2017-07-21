package com.voipgrid.vialer.call;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;
import com.voipgrid.vialer.util.PhoneNumberUtils;

/**
 * Fragment for transferring a call
 */
public class CallTransferFragment extends Fragment implements KeyPadView.OnKeyPadClickListener, View.OnClickListener {
    public static final String ORIGINAL_CALLER_ID = "ORIGINAL_CALLER_ID";
    public static final String ORIGINAL_CALLER_PHONE_NUMBER = "ORIGINAL_CALLER_PHONE_NUMBER";
    public static final String SECOND_CALL_IS_CONNECTED = "SECOND_CALL_IS_CONNECTED";

    private NumberInputView mNumberInputView;
    private String mOriginalCallerPhoneNumber;
    private String mOriginalCallerId;
    private View mCallTransferKeyPadView;
    private View mCallTransferButtonsContainer;
    private CallTransferFragmentListener mCallback;
    private boolean mSecondCallConnected = false;
    private View mTransferCallButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mOriginalCallerId = getArguments().getString(ORIGINAL_CALLER_ID);
        mOriginalCallerPhoneNumber = getArguments().getString(ORIGINAL_CALLER_PHONE_NUMBER);
        mSecondCallConnected = Boolean.parseBoolean(getArguments().getString(SECOND_CALL_IS_CONNECTED));
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
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (CallTransferFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
                String numberToCall = PhoneNumberUtils.format(mNumberInputView.getNumber());
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
