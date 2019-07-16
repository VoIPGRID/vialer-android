package com.voipgrid.vialer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.voipgrid.vialer.api.ApiTokenFetcher;
import com.voipgrid.vialer.util.TwoFactorFragmentHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class TwoFactorAuthenticationDialogFragment extends DialogFragment {

    @BindView(R.id.two_factor_code_field) EditText mCodeField;
    @BindView(R.id.button_continue) Button mButtonContinue;
    @BindView(R.id.form) RelativeLayout mForm;
    @BindView(R.id.completion_image) ImageView completionImage;

    private Unbinder unbinder;
    private TwoFactorFragmentHelper mTwoFactorFragmentHelper;
    private ApiTokenFetcher mFetchApiToken;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setView(getActivity().getLayoutInflater().inflate(
                        R.layout.onboarding_step_two_factor, null
                ))
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        unbinder = ButterKnife.bind(this, getDialog());
        mTwoFactorFragmentHelper = new TwoFactorFragmentHelper(getActivity(), mCodeField);
        mFetchApiToken = ApiTokenFetcher.usingSavedCredentials(getActivity());

        mTwoFactorFragmentHelper.focusOnTokenField();
        mTwoFactorFragmentHelper.pasteCodeFromClipboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTwoFactorFragmentHelper.pasteCodeFromClipboard();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    /**
     * If the continue button is clicked, find the token and call the appropriate method
     * on the listener.
     *
     */
    @OnClick(R.id.button_continue)
    public void onContinueButtonClicked() {
        String token = mCodeField.getText().toString();

        mButtonContinue.setEnabled(false);
        mCodeField.setEnabled(false);

        mFetchApiToken.setListener(new ApiTokenFetchListener()).fetch(token);
    }

    private class ApiTokenFetchListener implements ApiTokenFetcher.ApiTokenListener {

        @Override
        public void twoFactorCodeRequired() {
            onFailure();
        }

        @Override
        public void onSuccess(String apiToken) {
            if (!isAdded()) {
                return;
            }

            mForm.setVisibility(View.GONE);
            completionImage.setVisibility(View.VISIBLE);

            new Handler().postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }

                dismiss();
            }, 1500);
        }

        @Override
        public void onFailure() {
            if (!isAdded()) {
                return;
            }

            mButtonContinue.setEnabled(true);
            mCodeField.setEnabled(true);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.two_factor_authentication_token_invalid_title)
                    .setMessage(R.string.two_factor_authentication_token_invalid_description)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), (dialog, id) -> dialog.dismiss())
                    .show();
        }
    }
}
