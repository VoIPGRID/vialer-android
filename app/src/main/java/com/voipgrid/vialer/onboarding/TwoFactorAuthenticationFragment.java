package com.voipgrid.vialer.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.util.TwoFactorFragmentHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class TwoFactorAuthenticationFragment extends OnboardingFragment {

    @BindView(R.id.two_factor_code_field) EditText mCodeField;
    @BindView(R.id.button_continue) Button mButtonContinue;

    private Unbinder mUnbinder;
    private TwoFactorFragmentHelper mTwoFactorFragmentHelper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_two_factor_authentication, container, false);

        mUnbinder = ButterKnife.bind(this, view);
        mTwoFactorFragmentHelper = new TwoFactorFragmentHelper(getActivity(), mCodeField);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTwoFactorFragmentHelper.focusOnTokenField();
        mTwoFactorFragmentHelper.pasteCodeFromClipboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTwoFactorFragmentHelper.pasteCodeFromClipboard();
    }

    /**
     * If the continue button is clicked, find the token and call the appropriate method
     * on the listener.
     *
     */
    @OnClick(R.id.button_continue)
    public void onContinueButtonClicked() {
        mButtonContinue.setEnabled(false);

        String token = mCodeField.getText().toString();

        ((FragmentInteractionListener) mListener).userDidSupply2faCode(token);
    }

    @Override
    public void onError(String error) {
        mButtonContinue.setEnabled(true);

        reportErrorOrConnectivityError(
                R.string.two_factor_authentication_token_invalid_title,
                R.string.two_factor_authentication_token_invalid_description
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }


    public static Fragment newInstance() {
        return new TwoFactorAuthenticationFragment();
    }

    interface FragmentInteractionListener extends OnboardingFragment.FragmentInteractionListener {
        void userDidSupply2faCode(String code);
    }
}
