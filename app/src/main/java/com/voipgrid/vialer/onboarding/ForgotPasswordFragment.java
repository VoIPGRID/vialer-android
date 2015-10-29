package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
 * Use the {@link ForgotPasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForgotPasswordFragment extends OnboardingFragment implements View.OnClickListener {

    public static final String ARG_EMAIL = "email";

    private FragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ForgotPasswordFragment.
     */
    public static ForgotPasswordFragment newInstance(String email) {
        ForgotPasswordFragment fragment = new ForgotPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    public ForgotPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Button submitButton = (Button) view.findViewById(R.id.button_send_password_email);
        submitButton.setOnClickListener(this);

        String email = getArguments().getString(ARG_EMAIL);
        final EditText emailEditText = (EditText) view.findViewById(R.id.forgotPasswordEmailTextDialog);
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // NOOP
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // NOOP
            }

            @Override
            public void afterTextChanged(Editable s) {
                submitButton.setEnabled(s.length() > 0);
            }
        });
        emailEditText.setText(email);


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
        String email = ((EditText) getView()
                .findViewById(R.id.forgotPasswordEmailTextDialog))
                .getText()
                .toString();
        if (!email.isEmpty()) {
            mListener.onForgotPassword(this, email);
        }
    }

    @Override
    public void onError(String error) {

    }

    interface FragmentInteractionListener extends OnboardingFragment.FragmentInteractionListener {
        void onForgotPassword(Fragment fragment, String email);
    }
}
