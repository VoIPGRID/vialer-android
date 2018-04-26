package com.voipgrid.vialer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.DialogHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.PhoneAccountHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends LoginRequiredActivity implements
        Switch.OnCheckedChangeListener, AdapterView.OnItemSelectedListener,
        Callback {

    @BindView(R.id.account_sip_switch) CompoundButton mVoipSwitch;
    @BindView(R.id.account_sip_id_edit_text) EditText mSipIdEditText;
    @BindView(R.id.remote_logging_id_edit_text) EditText mRemoteLogIdEditText;
    @BindView(R.id.advanced_settings_layout) LinearLayout advancedSettings;
    @BindView(R.id.tls_switch) Switch tlsSwitch;
    @BindView(R.id.stun_switch) Switch stunSwitch;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    private PhoneAccount mPhoneAccount;
    private PhoneAccountHelper mPhoneAccountHelper;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;
    private Api mApi;
    private RemoteLogger mRemoteLogger;

    private boolean mEditMode = false;
    private boolean isSetupComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        ButterKnife.bind(this);

        mJsonStorage = new JsonStorage(this);
        mPhoneAccountHelper = new PhoneAccountHelper(this);
        mPreferences = new Preferences(this);
        mApi = ServiceGenerator.createApiService(this);
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();

        /* set the Toolbar to use as ActionBar */
        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);
        mRemoteLogIdEditText.setVisibility(View.GONE);
        mVoipSwitch.setOnCheckedChangeListener(this);

        initConnectionSpinner();
        initRemoteLoggingSwitch();
        initUse3GSwitch();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update and populate the fields.
        updateAndPopulate();

        // Update phone account and systemuser.
        updateSystemUserAndPhoneAccount();

        initializeAdvancedSettings();
    }

    private void initConnectionSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.call_connection_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.connection_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition(converseFromPreference((mPreferences.getConnectionPreference()))));
        spinner.setOnItemSelectedListener(this);
    }

    /**
     * One way conversion to charsequence from preference (long) because bidirectional maps
     * are not nativly supported in java.
     */
    private long converseToPreference(CharSequence connectionPreference) {
        if (connectionPreference.equals(getString(R.string.call_connection_only_cellular))) {
            return Preferences.CONNECTION_PREFERENCE_LTE;
        } else if (connectionPreference.equals(getString(R.string.call_connection_use_wifi_cellular))) {
            return Preferences.CONNECTION_PREFERENCE_WIFI;
        }
        return Preferences.CONNECTION_PREFERENCE_NONE;
    }

    /**
     * One way conversion to preference (long) from charsequence because bidirectional maps
     * are not nativly supported in java.
     */
    private CharSequence converseFromPreference(long preference) {
        if (preference == Preferences.CONNECTION_PREFERENCE_LTE) {
            return getString(R.string.call_connection_only_cellular);
        } else if (preference == Preferences.CONNECTION_PREFERENCE_WIFI) {
            return getString(R.string.call_connection_use_wifi_cellular);
        }
        return getString(R.string.call_connection_optional);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String selected = parent.getItemAtPosition(pos).toString();
        mPreferences.setConnectionPreference(converseToPreference(selected));
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // No need to implement. The preferences class will return a default value.
    }

    /**
     * Function to set the initial state of the remote logging switch and a onCheckChangeListener.
     */
    private void initRemoteLoggingSwitch() {
        CompoundButton remoteLoggingSwitch = (CompoundButton) findViewById(R.id.remote_logging_switch);
        remoteLoggingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (mPreferences.remoteLoggingIsActive() == isChecked) {
                    return;
                }
                mPreferences.setRemoteLogging(isChecked);
                if (isChecked) {
                    mRemoteLogIdEditText.setVisibility(View.VISIBLE);
                    mRemoteLogIdEditText.setText(mPreferences.getLoggerIdentifier());
                } else {
                    mRemoteLogIdEditText.setVisibility(View.GONE);
                }
            }
        });
        remoteLoggingSwitch.setChecked(mPreferences.remoteLoggingIsActive());
        if (mPreferences.remoteLoggingIsActive()) {
            mRemoteLogIdEditText.setVisibility(View.VISIBLE);
            mRemoteLogIdEditText.setText(mPreferences.getLoggerIdentifier());
        }
    }

    private void initUse3GSwitch() {
        CompoundButton use3GSwitch = (CompoundButton) findViewById(R.id.use_3g_switch);
        use3GSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mPreferences.has3GEnabled() == isChecked) {
                    return;
                }
                mPreferences.set3GEnabled(isChecked);
            }
        });
        use3GSwitch.setChecked(mPreferences.has3GEnabled());
    }

    private void updateAndPopulate() {
        mSystemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        mPhoneAccount = (PhoneAccount) mJsonStorage.get(PhoneAccount.class);

        populate();
    }

    private void populate() {
        if(mPreferences.hasSipPermission()) {
            mVoipSwitch.setChecked(mPreferences.hasSipEnabled());
            if(mPhoneAccount != null) {
                mSipIdEditText.setText(mPhoneAccount.getAccountId());
            }
        } else {
            mVoipSwitch.setVisibility(View.GONE);
        }
        ((EditText) findViewById(R.id.account_mobile_number_edit_text))
                .setText(mSystemUser.getMobileNumber());
        ((EditText) findViewById(R.id.account_outgoing_number_edit_text))
                .setText(mSystemUser.getOutgoingCli() == null || mSystemUser.getOutgoingCli().isEmpty() ? " " : mSystemUser.getOutgoingCli());
        mSipIdEditText.setVisibility(mPreferences.hasSipEnabled() ? View.VISIBLE : View.GONE);
        enableProgressBar(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_account, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_edit).setVisible(!mEditMode);
        menu.findItem(R.id.action_done).setVisible(mEditMode);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit || id == R.id.action_done) {
            if (id == R.id.action_edit) {
                mEditMode = true;
            }
            if (id == R.id.action_done) {

                if (isValidNumber()) {
                    mEditMode = false;
                    save();
                } else {
                    DialogHelper.displayAlert(
                            this,
                            getString(R.string.invalid_mobile_number_title),
                            getString(R.string.invalid_mobile_number_message)
                    );
                }
            }
            invalidateOptionsMenu();
            invalidateEditText();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * isValidNumber returns true if the number currently entered is a valid phone number.
     */
    private boolean isValidNumber() {
        String mobileNumber = ((EditText) findViewById(
                R.id.account_mobile_number_edit_text)).getText().toString();

        return PhoneNumberUtils.isValidMobileNumber(PhoneNumberUtils.formatMobileNumber(mobileNumber));
    }

    private void save() {
        findViewById(R.id.container).setFocusableInTouchMode(true);

        String number = ((EditText) findViewById(
                R.id.account_mobile_number_edit_text)).getText().toString();
        number = PhoneNumberUtils.formatMobileNumber(number);

        Api api = ServiceGenerator.createService(
                this,
                Api.class,
                getString(R.string.api_url),
                getEmail(),
                getPassword()
        );
        Call<MobileNumber> call = api.mobileNumber(new MobileNumber(number));
        call.enqueue(this);

        mSystemUser.setMobileNumber(number);

        populate();
    }

    private void invalidateEditText() {
        findViewById(R.id.account_mobile_number_edit_text).setEnabled(mEditMode);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mPreferences.hasSipEnabled() == isChecked) {
            /* nothing changed, so return */
            return;
        }
        mPreferences.setSipEnabled(isChecked);

        if (!isChecked) {
            // Unregister at middleware.
            MiddlewareHelper.unregister(this);
            // Stop the sipservice.
            stopService(new Intent(this, SipService.class));
            mSipIdEditText.setVisibility(View.GONE);
        } else {
            enableProgressBar(true);
            new AsyncTask<Void, Void, PhoneAccount>() {

                @Override
                protected PhoneAccount doInBackground(Void... params) {
                    return mPhoneAccountHelper.getLinkedPhoneAccount();
                }

                @Override
                protected void onPostExecute(PhoneAccount phoneAccount) {
                    super.onPostExecute(phoneAccount);

                    if (phoneAccount != null) {
                        mPhoneAccountHelper.savePhoneAccountAndRegister(phoneAccount);
                        updateAndPopulate();
                    } else {
                        // Make sure sip is disabled in preference and the switch is returned
                        // to disabled. Setting disabled in the settings first makes sure
                        // the onCheckChanged does not execute the code that normally is executed
                        // on a change in the check of the switch.
                        setVoIPAccount();
                    }
                }
            }.execute();
        }
    }

    /**
     * Loads setupactivity with the SetUpVoipAccountFragment.
     */
    private void setVoIPAccount(){
        Intent intent = new Intent(this, SetupActivity.class);
        Bundle b = new Bundle();
        b.putInt("fragment", R.id.fragment_voip_account_missing);
        b.putString("activity", AccountActivity.class.getSimpleName());
        intent.putExtras(b);

        startActivity(intent);
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) {
        if (response.isSuccessful()) {
            // Success callback for updating mobile number.
            // Update the systemuser.
            mJsonStorage.save(mSystemUser);
        } else {
            failedFeedback();
        }
    }

    /**
     * Sets the advanced settings checkboxes to the correct position depending
     * on the current values in shared preferences.
     */
    protected void initializeAdvancedSettings() {
        if (isFinishing()) return;
        isSetupComplete = false;
        enableProgressBar(false);
        tlsSwitch.setChecked(mPreferences.hasTlsEnabled());
        stunSwitch.setChecked(mPreferences.hasStunEnabled());
        isSetupComplete = true;
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull Throwable t) {
        failedFeedback();
    }

    /**
     * Function to update the systemuser and phone account. Update views after update.
     */
    private void updateSystemUserAndPhoneAccount() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                mPhoneAccountHelper.updatePhoneAccount();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                updateAndPopulate();
            }
        }.execute();
    }

    private void enableProgressBar(boolean enabled) {
        mProgressBar.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Function to inform the user of a failed requests.
     */
    private void failedFeedback() {
        DialogHelper.displayAlert(
                this,
                getString(R.string.onboarding_account_configure_failed_title),
                getString(R.string.onboarding_account_configure_invalid_phone_number)
        );
    }

    @OnCheckedChanged(R.id.tls_switch)
    public void tlsSwitchChanged(CompoundButton compoundButton, final boolean b) {
        if (!isSetupComplete) return;

        SecureCalling secureCalling = SecureCalling.fromContext(this);

        enableProgressBar(true);

        SecureCalling.Callback callback = new SecureCalling.Callback() {
            @Override
            public void onSuccess() {
                mPreferences.setTlsEnabled(b);
                mRemoteLogger.i("TLS switch has been set to: " + b);
                initializeAdvancedSettings();
            }

            @Override
            public void onFail() {
                initializeAdvancedSettings();
            }
        };

        if(b) {
            secureCalling.enable(callback);
        } else {
            secureCalling.disable(callback);
        }
    }

    @OnCheckedChanged(R.id.stun_switch)
    public void stunSwitchChanged(CompoundButton compoundButton, boolean b) {
        if (!isSetupComplete) return;
        mPreferences.setStunEnabled(b);
        mRemoteLogger.i("STUN has been set to: " + b);
        initializeAdvancedSettings();
    }
}
