package com.voipgrid.vialer.call;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;

/**
 * Fragment for transferring a call
 */
public class CallTransferFragment extends Fragment implements KeyPadView.OnKeyPadClickListener, View.OnClickListener {
    private NumberInputView mNumberInputView;
    private static String mOriginalCaller;
    private CallTransferFragmentListener mCallback;

    public static CallTransferFragment newInstance(String originalCaller) {
        mOriginalCaller = originalCaller;
        CallTransferFragment fragment = new CallTransferFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_transfer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.call_transfer_number)).setText(mOriginalCaller);

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

        View onHoldButton = view.findViewById(R.id.call_transfer_hold);
        onHoldButton.setOnClickListener(this);

        View makeTransferCall = view.findViewById(R.id.button_call);
        makeTransferCall.setOnClickListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;

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

    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        switch (viewId) {
            case R.id.call_transfer_hold:
                mCallback.callTransferToggleOnHoldInitialCall();
                break;

            case R.id.button_call:
                break;
        }
    }

    public interface CallTransferFragmentListener {
        void callTransferToggleOnHoldInitialCall();
    }
}
