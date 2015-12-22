package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.voipgrid.vialer.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AccountFragment extends OnboardingFragment implements
        View.OnClickListener,
        TextWatcher {
    public static final String ARG_MOBILE = "mobile";
    public static final String ARG_OUTGOING = "outgoing";

    private static final String SUPRESSED = "suppressed";

    private FragmentInteractionListener mListener;

    private EditText mMobileEdittext;
    private EditText mOutgoingEdittext;
    private Button mConfigureButton;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param mobileNumberString Parameter 1.
     * @param outgoingNumberString Parameter 2.
     * @return A new instance of fragment AccountFragment.
     */
    public static AccountFragment newInstance(String mobileNumberString,
                                              String outgoingNumberString) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MOBILE, mobileNumberString);
        args.putString(ARG_OUTGOING, outgoingNumberString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mMobileEdittext = (EditText) view.findViewById(R.id.mobileNumberTextDialog);
            mMobileEdittext.setText(arguments.getString(ARG_MOBILE));
            mMobileEdittext.addTextChangedListener(this);

            mOutgoingEdittext = (EditText) view.findViewById(R.id.outgoingNumberTextDialog);
            String outGoingNumber = arguments.getString(ARG_OUTGOING);
            /* NB: sometimes the outgoing number is supressed (anonymous),
            so we capture that here */
            if (outGoingNumber.equals(SUPRESSED)) {
                outGoingNumber = getString(R.string.supressed_number);
            }
            mOutgoingEdittext.setText(outGoingNumber);
            mOutgoingEdittext.addTextChangedListener(this);

            mConfigureButton = (Button) view.findViewById(R.id.button_configure);
            mConfigureButton.setOnClickListener(this);

            // enable configure button when mobile number is available for the SystemUser
            if(mMobileEdittext.getText().length() > 0) {
                mConfigureButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onError(String error) {
        mListener.onAlertDialog(getString(R.string.onboarding_account_configure_failed_title),
                getString(R.string.onboarding_account_configure_invalid_phone_number));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        mListener.onUpdateMobileNumber(this, String.valueOf(mMobileEdittext.getText()));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // not used
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // not used
    }

    @Override
    public void afterTextChanged(Editable s) {
        if(mMobileEdittext.length() > 0 && mOutgoingEdittext.length() > 0) {
            mConfigureButton.setEnabled(true);
        } else {
            mConfigureButton.setEnabled(false);
        }
    }

    public void onNextStep() {
        mListener.onConfigure(
                this,
                String.valueOf(mMobileEdittext.getText()),
                String.valueOf(mOutgoingEdittext.getText())
        );
    }

    interface FragmentInteractionListener extends OnboardingFragment.FragmentInteractionListener {
        void onUpdateMobileNumber(Fragment fragment, String mobileNumber);
        void onConfigure(Fragment fragment, String mobileNumber, String outgoingNumber);
    }
}
