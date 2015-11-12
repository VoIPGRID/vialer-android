package com.voipgrid.vialer.onboarding;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.squareup.okhttp.OkHttpClient;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerGcmRegistrationService;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.models.PasswordResetParams;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.Storage;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;


/**
 * Activity that handles the onboarding.
 */
public class SetupActivity extends AppCompatActivity implements
        OnboardingFragment.FragmentInteractionListener,
        LoginFragment.FragmentInteractionListener,
        AccountFragment.FragmentInteractionListener,
        ForgotPasswordFragment.FragmentInteractionListener,
        Callback {

    private String mPassword;

    private Api mApi;
    private ConnectivityHelper mConnectivityHelper;
    private Preferences mPreferences;
    private Storage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mStorage = new Storage(this);

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mPreferences = new Preferences(this);

        if (findViewById(R.id.fragment_container) != null) {
            LogoFragment firstFragment = LogoFragment.newInstance();
            // Add the fragment to the 'fragment_container' FrameLayout
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container, firstFragment).commit();
        }
    }

    /**
     * Swap the current fragment for a new one with the next step of the configure process.
     * @param newFragment next step in the setup process to present to the user.
     */
    void swapFragment(Fragment newFragment, String tag) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (tag != null) {
            transaction.addToBackStack(null);
        } else {
            tag = "fragment";
        }
        transaction.replace(R.id.fragment_container, newFragment, tag).commit();

        //Hide the keyboard when switching fragments
        manageKeyboard();
    }

    /**
     * display an alert with a certain title and message. The Finish button only does dialog
     * dismiss.
     * @param title the title of the alert to show.
     * @param message the message body of the alert to show.
     */
    private void displayAlert(String title, String message) {
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
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
    public void onLogin(final Fragment fragment, final String username, String password) {
        mPassword = password;
        enableProgressBar(true);

        mApi = ServiceGenerator.createService(
                mConnectivityHelper,
                Api.class,
                getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(this, username, password))
        );
        mApi.systemUser(this);
    }

    @Override
    public void onUpdateMobileNumber(Fragment fragment, String mobileNumber) {
        enableProgressBar(true);

        /* post mobileNumber to VoipGrip platform */
        mApi.mobileNumber(new MobileNumber(mobileNumber), this);
    }

    @Override
    public void onConfigure(Fragment fragment, String mobileNumber, String outgoingNumber) {
        enableProgressBar(true);

        /*  save mobile and outgoing number */
        SystemUser systemUser = (SystemUser) mStorage.get(SystemUser.class);
        systemUser.setMobileNumber(mobileNumber);
        systemUser.setOutgoingCli(outgoingNumber);
        mStorage.save(systemUser);

        String phoneAccountId = systemUser.getPhoneAccountId();
        if(phoneAccountId != null) {
            mApi.phoneAccount(phoneAccountId, this);
        } else {
            enableProgressBar(false);
            // TODO add UI to let the user know the sip features are not available without
            // a sip account VIALA-157
            onNextStep(WelcomeFragment.newInstance(
                            ((SystemUser) mStorage.get(SystemUser.class)).getFullName())
            );
        }
    }

    @Override
    public void onNextStep(Fragment nextFragment) {
        swapFragment(nextFragment, nextFragment.getClass().getSimpleName());
    }

    @Override
    public void onFinish(Fragment fragment) {
        startActivity(new Intent(this, MainActivity.class));
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

    @Override
    public void success(Object object, Response response) {
        enableProgressBar(false);
        if(object instanceof SystemUser) {
            SystemUser systemUser = ((SystemUser) object);
            if(systemUser.getPartner() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onAlertDialog(getString(R.string.user_is_partner_error_title),
                                getString(R.string.user_is_partner_error_message));
                    }
                });
            } else {
                mPreferences.setSipPermission(systemUser.hasSipPermission());
                systemUser.setPassword(mPassword);
                mStorage.save(systemUser);
                onNextStep(AccountFragment.newInstance(
                        systemUser.getMobileNumber(),
                        systemUser.getOutgoingCli()
                ));
            }
        } else if(object instanceof PhoneAccount) {
            mStorage.save(object);
            if(mPreferences.hasSipPermission()) {
                startService(new Intent(this, VialerGcmRegistrationService.class));
            }
            onNextStep(WelcomeFragment.newInstance(
                    ((SystemUser) mStorage.get(SystemUser.class)).getFullName())
            );
        } else {
            FragmentManager fragmentManager = getFragmentManager();
            // First see if a AccountFragment exists
            AccountFragment fragment = (AccountFragment) fragmentManager
                    .findFragmentByTag(AccountFragment.class.getSimpleName());
            if(fragment != null) {
                fragment.onNextStep();
            }

            ForgotPasswordFragment forgotFragment = (ForgotPasswordFragment) fragmentManager
                    .findFragmentByTag(ForgotPasswordFragment.class.getSimpleName());
            if (forgotFragment != null) {
                onNextStep(LoginFragment.newInstance());
            }
        }
    }

    @Override
    public void failure(RetrofitError error) {
        final String errorMessage = error.getMessage();
        final String failedUrl = error.getUrl();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enableProgressBar(false);
                OnboardingFragment fragment = getCurrentFragment();
                if (failedUrl.endsWith("password_reset/")) {
                    displayAlert(
                            getString(R.string.forgot_password_error_title),
                            getString(R.string.forgot_password_error_text)
                    );
                }
                if (fragment != null) {
                    fragment.onError(errorMessage);
                }
            }
        });
    }

    private String[] tags = {
            LogoFragment.class.getSimpleName(),
            LoginFragment.class.getSimpleName(),
            ForgotPasswordFragment.class.getSimpleName(),
            AccountFragment.class.getSimpleName(),
            WelcomeFragment.class.getSimpleName()
    };

    private OnboardingFragment getCurrentFragment() {
        OnboardingFragment fragment = null;
        int count = 0;
        while(fragment == null) {
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
    public void onForgotPassword(Fragment fragment, String email) {
        Api api = ServiceGenerator.createService(
                mConnectivityHelper,
                Api.class,
                getString(R.string.api_url),
                new OkClient(new OkHttpClient())
        );
        api.resetPassword(new PasswordResetParams(email), this);
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
}
