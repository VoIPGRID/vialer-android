package com.voipgrid.vialer.onboarding;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.voipgrid.vialer.AccountActivity;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerGcmRegistrationService;
import com.voipgrid.vialer.WebActivityHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.PreviousRequestNotFinishedException;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.models.PasswordResetParams;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.PhoneAccountHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity that handles the onboarding.
 */
public class SetupActivity extends AppCompatActivity implements
        OnboardingFragment.FragmentInteractionListener,
        LoginFragment.FragmentInteractionListener,
        AccountFragment.FragmentInteractionListener,
        ForgotPasswordFragment.FragmentInteractionListener,
        SetUpVoipAccountFragment.FragmentInteractionListener,
        Callback {

    private String mPassword;
    private String mActivityToReturnToName = "";

    private Api mApi;
    private JsonStorage mJsonStorage;
    private Preferences mPreferences;
    private ServiceGenerator mServiceGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mJsonStorage = new JsonStorage(this);
        mPreferences = new Preferences(this);

        Fragment firstFragment = null;
        Bundle b = getIntent().getExtras();
        if (b != null) {
            int fragmentId = b.getInt("fragment");
            mActivityToReturnToName = b.getString("activity");
            firstFragment = ((SetUpVoipAccountFragment) getFragmentManager().findFragmentById(fragmentId)).newInstance();
        }
        if (findViewById(R.id.fragment_container) != null) {
            if (firstFragment == null) {
                firstFragment = LogoFragment.newInstance();
            }
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

        try {
            mServiceGen = ServiceGenerator.getInstance();
        } catch(PreviousRequestNotFinishedException e) {
            e.printStackTrace();
            return;
        }

        mApi = mServiceGen.createService(
                this,
                Api.class,
                getString(R.string.api_url),
                username,
                password
        );
        Call<SystemUser> call = mApi.systemUser();
        call.enqueue(this);
    }

    @Override
    public void onUpdateMobileNumber(Fragment fragment, String mobileNumber) {
        enableProgressBar(true);

        /* post mobileNumber to VoipGrip platform */
        Call<MobileNumber> call = mApi.mobileNumber(new MobileNumber(mobileNumber));
        call.enqueue(this);
    }

    @Override
    public void onConfigure(Fragment fragment, String mobileNumber, String outgoingNumber) {
        enableProgressBar(true);

        /*  save mobile and outgoing number */
        SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        systemUser.setMobileNumber(mobileNumber);
        systemUser.setOutgoingCli(outgoingNumber);
        mJsonStorage.save(systemUser);

        String phoneAccountId = systemUser.getPhoneAccountId();
        if (phoneAccountId != null) {
            Call<PhoneAccount> call = mApi.phoneAccount(phoneAccountId);
            call.enqueue(this);
        } else {
            enableProgressBar(false);
            // TODO add UI to let the user know the sip features are not available without
            // a sip account VIALA-157
            onNextStep(WelcomeFragment.newInstance(
                            ((SystemUser) mJsonStorage.get(SystemUser.class)).getFullName())
            );
        }
    }

    @Override
    public void onSetVoipAccount(Fragment fragment) {
        WebActivityHelper webHelper = new WebActivityHelper(this);
        webHelper.startWebActivity(getString(R.string.user_change_title), getString(R.string.web_user_change));
    }

    @Override
    public void onNextStep(Fragment nextFragment) {
        swapFragment(nextFragment, nextFragment.getClass().getSimpleName());
    }

    @Override
    public void onFinish(Fragment fragment) {
        if (mActivityToReturnToName.equals(AccountActivity.class.getSimpleName())){
            savePhoneAccountAndRegister(mPreferences);
            startActivity(new Intent(this, AccountActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    public void savePhoneAccountAndRegister(final Preferences mPreferences) {
        final PhoneAccountHelper phoneAccountHelper = new PhoneAccountHelper(this);

        new AsyncTask<Void, Void, PhoneAccount>() {

            @Override
            protected PhoneAccount doInBackground(Void... params) {
                return phoneAccountHelper.getLinkedPhoneAccount();
            }

            @Override
            protected void onPostExecute(PhoneAccount phoneAccount) {
                super.onPostExecute(phoneAccount);
                if (phoneAccount != null) {
                    phoneAccountHelper.savePhoneAccountAndRegister(phoneAccount);
                    mPreferences.setSipEnabled(true);
                } else {
                    mPreferences.setSipEnabled(false);
                }
            }
        }.execute();
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
    public void onForgotPassword(Fragment fragment, final String email) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.forgot_password_alert_title));
        alertDialogBuilder
                .setMessage(getString(R.string.forgot_password_alert_message, email))
                .setCancelable(false)
                .setPositiveButton(this.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        resetPassword(email);
                    }
                })
                .setNegativeButton(this.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

    private void resetPassword(String email) {
        Api api = ServiceGenerator.createService(
                this,
                Api.class,
                getString(R.string.api_url)
        );
        Call<Object> call = api.resetPassword(new PasswordResetParams(email));
        call.enqueue(this);
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
    public void onResponse(Call call, Response response) {
        if (mServiceGen != null){
            mServiceGen.release();
        }
        if (response.isSuccess()) {
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
                    mPreferences.setSipPermission(systemUser.hasSipPermission());
                    systemUser.setPassword(mPassword);
                    mJsonStorage.save(systemUser);
                    onNextStep(AccountFragment.newInstance(
                            systemUser.getMobileNumber(),
                            systemUser.getOutgoingCli()
                    ));
                }
            } else if (response.body() instanceof PhoneAccount) {
                mJsonStorage.save(response.body());
                if (mPreferences.hasSipPermission()) {
                    startService(new Intent(this, VialerGcmRegistrationService.class));
                }
                onNextStep(WelcomeFragment.newInstance(
                                ((SystemUser) mJsonStorage.get(SystemUser.class)).getFullName())
                );
            } else {
                FragmentManager fragmentManager = getFragmentManager();
                // First see if a AccountFragment exists
                AccountFragment fragment = (AccountFragment) fragmentManager
                        .findFragmentByTag(AccountFragment.class.getSimpleName());
                if (fragment != null) {
                    fragment.onNextStep();
                }

                ForgotPasswordFragment forgotFragment = (ForgotPasswordFragment) fragmentManager
                        .findFragmentByTag(ForgotPasswordFragment.class.getSimpleName());
                if (forgotFragment != null) {
                    onNextStep(LoginFragment.newInstance());
                }
            }
        } else {
            failedFeedback(response.errorBody().toString());
        }
    }

    @Override
    public void onFailure(Call call, Throwable t) {
        if (mServiceGen != null){
            mServiceGen.release();
        }
        failedFeedback("Failed");
    }

    private void failedFeedback(String message) {
        final String mMessage = message;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enableProgressBar(false);
                OnboardingFragment fragment = getCurrentFragment();
                if (fragment != null) {
                    fragment.onError(mMessage);
                }
            }
        });
    }
}
