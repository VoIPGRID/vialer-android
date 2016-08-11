package com.voipgrid.vialer.call;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;

/**
 * Fragment for the keypad when calling.
 */
public class CallKeyPadFragment extends Fragment implements KeyPadView.OnKeyPadClickListener {
    private NumberInputView mNumberInputView;
    private CallKeyPadFragmentListener mCallback;

    public static CallKeyPadFragment newInstance() {
        CallKeyPadFragment fragment = new CallKeyPadFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_key_pad, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup keyPadViewContainer = (ViewGroup) view.findViewById(R.id.fragment_call_key_pad);
        KeyPadView keyPadView = (KeyPadView) keyPadViewContainer.findViewById(R.id.key_pad_view);
        keyPadView.setOnKeyPadClickListener(this);

        mNumberInputView = (NumberInputView) view.findViewById(R.id.number_input_edit_text);
        mNumberInputView.setOnInputChangedListener(new NumberInputView.OnInputChangedListener() {
            @Override
            public void onInputChanged(String number) {
                // This is needed to get the remove button to show up.
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;

        try {
            mCallback = (CallKeyPadFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement CallKeyPadFragmentListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNumberInputView.clear();
    }

    @Override
    public void onKeyPadButtonClick(String digit, String chars) {
        String currentDTMF = mNumberInputView.getNumber();
        mNumberInputView.setNumber(currentDTMF + digit);
        mCallback.callKeyPadButtonClicked(digit);
    }

    public interface CallKeyPadFragmentListener {
        void callKeyPadButtonClicked(String dtmf);
    }
}
