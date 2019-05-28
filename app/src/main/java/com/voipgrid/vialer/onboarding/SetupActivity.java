package com.voipgrid.vialer.onboarding;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.voipgrid.vialer.SettingsActivity;
import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.WebActivityHelper;
import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.ApiTokenFetcher;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.logging.VialerBaseActivity;
import com.voipgrid.vialer.middleware.MiddlewareHelper;
import com.voipgrid.vialer.models.PasswordResetParams;
import com.voipgrid.vialer.util.AccountHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.PhoneAccountHelper;

import java.io.IOException;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity that handles the on boarding.
 */
public class SetupActivity extends VialerBaseActivity implements
        OnboardingFragment.FragmentInteractionListener,
        LoginFragment.FragmentInteractionListener,
        AccountFragment.FragmentInteractionListener,
        ForgotPasswordFragment.FragmentInteractionListener,
        SetUpVoipAccountFragment.FragmentInteractionListener,
        TwoFactorAuthenticationFragment.FragmentInteractionListener,
        Callback {

    private final static String TAG = SetupActivity.class.getSimpleName();
    private String mPassword;
    private String mActivityToReturnToName = "";

    private VoipgridApi mVoipgridApi;
    private JsonStorage mJsonStorage;
    private Preferences mPreferences;
    private Logger mLogger;
    private ServiceGenerator mServiceGen;
    private AlertDialog mAlertDialog;
    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mJsonStorage = new JsonStorage(this);
        mPreferences = new Preferences(this);

        // Forced logging due to user not being able to set/unset it at this point.
        mLogger = new Logger(SetupActivity.class).forceRemoteLogging(true);

        Fragment gotoFragment = null;
        Integer fragmentId = null;
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            fragmentId = bundle.getInt("fragment");
            mActivityToReturnToName = bundle.getString("activity");
            if (fragmentId == R.id.fragment_voip_account_missing) {
                gotoFragment = ((SetUpVoipAccountFragment) getFragmentManager()
                        .findFragmentById(fragmentId)).newInstance();
            } else if (fragmentId == R.id.fragment_account) {
                SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
                gotoFragment = ((AccountFragment) getFragmentManager()
                        .findFragmentById(fragmentId))
                        .newInstance(systemUser.getMobileNumber(), systemUser.getOutgoingCli());
            }
        }

        if (findViewById(R.id.fragment_container) != null) {
            if (gotoFragment == null) {
                gotoFragment = LogoFragment.newInstance();
            }

            if (fragmentId != null && fragmentId == R.id.fragment_account) {
                swapFragment(gotoFragment, AccountFragment.class.getSimpleName());
            } else {
                swapFragment(gotoFragment, gotoFragment.getClass().getSimpleName());
            }
        }
    }

    /**
     * Swap the current fragment for a new one with the next step of the configure process.
     *
     * @param newFragment next step in the setup process to present to the user.
     */
    private void swapFragment(Fragment newFragment, String tag) {
        if(isFinishing()) return;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (tag != null) {
            transaction.addToBackStack(null);
        } else {
            tag = "fragment";
        }

        transaction.replace(R.id.fragment_container, newFragment, tag).commitAllowingStateLoss();

        // Hide the keyboard when switching fragments.
        manageKeyboard();
    }

    /**
     * Display an alert with a certain title and message. The Finish button only does dialog
     * dismiss.
     * @param title the title of the alert to show.
     * @param message the message body of the alert to show.
     */
    private void displayAlert(String title, String message) {
        if(isFinishing()) return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();
    }

    @Override
    public void onBackPressed() {
        Fragment forgotPasswordFragment = getFragmentManager()
                .findFragmentByTag(ForgotPasswordFragment.class.getSimpleName());
        if (forgotPasswordFragment instanceof ForgotPasswordFragment) {
            // In case of a forgot password fragment we want to move back.
            getFragmentManager().popBackStack();
        } else {
            // Otherwise normal behavior
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableProgressBar(false);
    }

    @Override
    public void onUpdateMobileNumber(Fragment fragment, String mobileNumber) {
        enableProgressBar(true);

        mVoipgridApi = ServiceGenerator.createApiService(this);

        Call<MobileNumber> call = mVoipgridApi.mobileNumber(new MobileNumber(mobileNumber));
        call.enqueue(this);
    }

    @Override
    public void onConfigure(Fragment fragment, String mobileNumber, String outgoingNumber) {
        enableProgressBar(true);

        // Save mobile and outgoing number.
        SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        systemUser.setMobileNumber(mobileNumber);
        systemUser.setOutgoingCli(outgoingNumber);
        mJsonStorage.save(systemUser);

        String phoneAccountId = systemUser.getPhoneAccountId();

        if (phoneAccountId != null) {
            Call<PhoneAccount> call = mVoipgridApi.phoneAccount(phoneAccountId);
            call.enqueue(this);
        } else {
            enableProgressBar(false);
            onNextStep(WelcomeFragment.newInstance(
                            ((SystemUser) mJsonStorage.get(SystemUser.class)).getFullName())
            );
        }
    }

    @Override
    public void onSetVoipAccount(Fragment fragment) {
        WebActivityHelper webHelper = new WebActivityHelper(this);
        webHelper.startWebActivity(
                getString(R.string.user_change_title),
                getString(R.string.web_user_change),
                getString(R.string.analytics_user_change)
        );
    }

    @Override
    public void onNextStep(Fragment nextFragment) {
        swapFragment(nextFragment, nextFragment.getClass().getSimpleName());
    }

    @Override
    public void onFinish(Fragment fragment) {
        if (mActivityToReturnToName.equals(SettingsActivity.class.getSimpleName())){
            PhoneAccountHelper phoneAccountHelper = new PhoneAccountHelper(this);
            phoneAccountHelper.savePhoneAccountAndRegister(
                    (PhoneAccount) mJsonStorage.get(PhoneAccount.class));
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onAlertDialog(String title, String message) {
        displayAlert(title, message);
    }

    private void enableProgressBar(boolean enabled) {
        View view = findViewById(R.id.progress_bar);
        if(view != null) {
            view.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private String[] tags = {
            LogoFragment.class.getSimpleName(),
            LoginFragment.class.getSimpleName(),
            TwoFactorAuthenticationFragment.class.getSimpleName(),
            ForgotPasswordFragment.class.getSimpleName(),
            AccountFragment.class.getSimpleName(),
            WelcomeFragment.class.getSimpleName()
    };

    private OnboardingFragment getCurrentFragment() {
        OnboardingFragment fragment = null;
        int count = 0;
        while (fragment == null && count < tags.length) {
            fragment = getCurrentFragment(tags[count]);
            count++;
        }
        return fragment;
    }

    private OnboardingFragment getCurrentFragment(String tag) {
        OnboardingFragment fragment = (OnboardingFragment) getFragmentManager()
                .findFragmentByTag(tag);

        if(fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }

    @Override
    public void onForgotPassword(Fragment fragment, final String email) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.forgot_password_alert_title));
        alertDialogBuilder
                .setMessage(getString(R.string.forgot_password_alert_message, email))
                .setCancelable(false)
                .setPositiveButton(this.getString(R.string.ok),
                        (dialog, id) -> resetPassword(email))
                .setNegativeButton(this.getString(R.string.cancel),
                        (dialog, id) -> dialog.dismiss());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
    }

    private void resetPassword(String email) {
        VoipgridApi voipgridApi = ServiceGenerator.createApiService(this, null, null, null);
        Call<Void> call = voipgridApi.resetPassword(new PasswordResetParams(email));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    failedFeedback("Request failed");
                    return;
                }

               new AlertDialog.Builder(SetupActivity.this)
                        .setTitle(R.string.forgot_password_success_title)
                        .setMessage(R.string.forgot_password_success_message)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss())
                        .create()
                        .show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                failedFeedback(t.getMessage());
            }
        });
    }

    /**
     *  Hide keyboard on fragment switch
     */
    public void manageKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager keyboard = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) {
        if (response.isSuccessful()) {
            enableProgressBar(false);
            if (response.body() instanceof SystemUser) {
                SystemUser systemUser = ((SystemUser) response.body());
                if (systemUser.getPartner() != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onAlertDialog(getString(R.string.user_is_partner_error_title),
                                    getString(R.string.user_is_partner_error_message));
                        }
                    });
                } else {
                    if (systemUser.getOutgoingCli() == null || systemUser.getOutgoingCli().isEmpty()) {
                        mLogger.d("missedCallsHaveBeenRetrieved getOutgoingCli is null");
                    }
                    mPreferences.setSipPermission(true);

                    AccountHelper accountHelper = new AccountHelper(this);
                    accountHelper.setCredentials(systemUser.getEmail(), mPassword);

                    mJsonStorage.save(systemUser);

                    onNextStep(AccountFragment.newInstance(
                            systemUser.getMobileNumber(),
                            systemUser.getOutgoingCli()
                    ));
                }
            } else if (response.body() instanceof PhoneAccount) {
                mJsonStorage.save(response.body());
                if (mPreferences.hasSipPermission()) {
                    MiddlewareHelper.registerAtMiddleware(this);
                }

                SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);

                onNextStep(WelcomeFragment.newInstance(systemUser.getFullName()));
            } else {
                FragmentManager fragmentManager = getFragmentManager();

                // First see if an AccountFragment exists
                AccountFragment fragment = (AccountFragment) fragmentManager
                        .findFragmentByTag(AccountFragment.class.getSimpleName());

                if (fragment != null) {
                    fragment.onNextStep();
                    return;
                }

                // Check if the current fragment is the account fragment.
                AccountFragment accountFragment = (AccountFragment) getCurrentFragment();
                if (accountFragment != null) {
                    accountFragment.onNextStep();
                    return;
                }

                ForgotPasswordFragment forgotFragment = (ForgotPasswordFragment) fragmentManager
                        .findFragmentByTag(ForgotPasswordFragment.class.getSimpleName());
                if (forgotFragment != null) {
                    onNextStep(LoginFragment.newInstance());
                    return;
                }
            }
        } else {
            String errorString = "";
            try {
                errorString = response.errorBody().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (errorString.equals("You need to change your password in the portal")) {
                WebActivityHelper webHelper = new WebActivityHelper(this);
                webHelper.startWebActivity(
                        getString(R.string.password_change_title),
                        getString(R.string.web_password_change),
                        getString(R.string.analytics_password_change)
                );
                Toast.makeText(this, R.string.change_password_webview_toast, Toast.LENGTH_LONG).show();
                return;
            }

            failedFeedback(errorString);
        }
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull Throwable t) {
        failedFeedback("Failed");
    }

    private void failedFeedback(String message) {
        final String mMessage = message;
        runOnUiThread(() -> {
            enableProgressBar(false);
            OnboardingFragment fragment = getCurrentFragment();
            if (fragment != null) {
                fragment.onError(mMessage);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    public void onLogin(final Fragment fragment, String username, String password) {
        mUsername = username;
        mPassword = password;
        enableProgressBar(true);
        enableProgressBar(true);

        mVoipgridApi = ServiceGenerator.createApiService(this, mUsername, mPassword, null);

        ApiTokenFetcher
                .forCredentials(this, mUsername, mPassword)
                .setListener(new ApiTokenFetchListener())
                .fetch();
    }

    @Override
    public void userDidSupply2faCode(String code) {
        enableProgressBar(true);

        ApiTokenFetcher
                .forCredentials(this, mUsername, mPassword)
                .setListener(new ApiTokenFetchListener())
                .fetch(code);
    }

    public static void launchToSetVoIPAccount(Activity fromActivity) {
        Intent intent = new Intent(fromActivity, SetupActivity.class);
        Bundle b = new Bundle();
        b.putInt("fragment", R.id.fragment_voip_account_missing);
        b.putString("activity", SettingsActivity.class.getSimpleName());
        intent.putExtras(b);

        fromActivity.startActivity(intent);
    }

    private class ApiTokenFetchListener implements ApiTokenFetcher.ApiTokenListener {

        @Override
        public void twoFactorCodeRequired() {
            enableProgressBar(false);
            onNextStep(TwoFactorAuthenticationFragment.newInstance());
        }

        @Override
        public void onSuccess(String apiToken) {
            enableProgressBar(false);
            AccountHelper accountHelper = new AccountHelper(SetupActivity.this);
            accountHelper.setCredentials(mUsername, mPassword, apiToken);

            mVoipgridApi = ServiceGenerator.createApiService(SetupActivity.this);

            Call<SystemUser> call2 = mVoipgridApi.systemUser();
            call2.enqueue(SetupActivity.this);
        }

        @Override
        public void onFailure() {
            enableProgressBar(false);
            failedFeedback("Failed");
        }
    }
}
