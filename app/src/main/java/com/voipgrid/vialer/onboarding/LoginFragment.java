package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.WebActivity;
import com.voipgrid.vialer.util.ConnectivityHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends OnboardingFragment implements
        View.OnClickListener,
        TextWatcher {

    private FragmentInteractionListener mListener;
    private EditText mEmailEdittext;
    private EditText mPasswordEdittext;
    private Button mLoginButton;
    private Button mForgotPasswordButton;
    private Button mInfoButton;
    private ConnectivityHelper mConnectivityHelper;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LoginFragment.
     */
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = getActivity();
        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE)
        );

        mEmailEdittext = (EditText) view.findViewById(R.id.emailTextDialog);
        mEmailEdittext.addTextChangedListener(this);

        mPasswordEdittext = (EditText) view.findViewById(R.id.passwordTextDialog);
        mPasswordEdittext.addTextChangedListener(this);
        mPasswordEdittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Add an EditorAction when clicking done which override selection of
                // forgot password and initiates a login.
                return actionId == EditorInfo.IME_ACTION_DONE && mLoginButton.performClick();
            }
        });

        mLoginButton = (Button) view.findViewById(R.id.button_login);
        mLoginButton.setOnClickListener(this);

        mForgotPasswordButton = (Button) view.findViewById(R.id.button_forgot_password);
        mForgotPasswordButton.setOnClickListener(this);

        mInfoButton = (Button) view.findViewById(R.id.button_info);
        mInfoButton.setOnClickListener(this);
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
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_login:
                mListener.onLogin(
                        this,
                        String.valueOf(mEmailEdittext.getText()),
                        String.valueOf(mPasswordEdittext.getText())
                );
                break;
            case R.id.button_forgot_password:
                mListener.onNextStep(
                        ForgotPasswordFragment.newInstance(
                        String.valueOf(mEmailEdittext.getText()))
                );
                break;
            case R.id.button_info:
                Intent intent = new Intent(getActivity(), WebActivity.class);
                intent.putExtra(WebActivity.PAGE, getString(R.string.url_app_info));
                intent.putExtra(WebActivity.TITLE, getString(R.string.info_menu_item_title));
                startActivity(intent);
                break;
        }
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
        if(mEmailEdittext.length() > 0 && mPasswordEdittext.length() > 0) {
            mLoginButton.setEnabled(true);
        } else {
            mLoginButton.setEnabled(false);
        }
    }

    public void onSucces() {
        mListener.onNextStep(this);
    }

    public void onError(String error) {
        if(!mConnectivityHelper.hasNetworkConnection()) {
            error = getString(R.string.onboarding_no_internet_message);
        } else {
            error = getString(R.string.onboarding_login_failed_message);
        }
        mListener.onAlertDialog(getString(R.string.onboarding_login_failed_title), error);
    }

    interface FragmentInteractionListener extends OnboardingFragment.FragmentInteractionListener {
        void onLogin(Fragment fragment, String username, String password);
    }

}
