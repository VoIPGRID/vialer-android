package com.voipgrid.vialer;

import static com.voipgrid.vialer.util.ConnectivityHelper.converseFromPreference;
import static com.voipgrid.vialer.util.ConnectivityHelper.converseToPreference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ActionMode;
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
import android.widget.Toast;

import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;
import com.voipgrid.vialer.notifications.VoipDisabledNotification;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BatteryOptimizationManager;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ClipboardHelper;
import com.voipgrid.vialer.util.DialogHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.PhoneAccountHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnItemSelected;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends LoginRequiredActivity {

    @Inject BatteryOptimizationManager batteryOptimizationManager;

    @BindView(R.id.container) View mContainer;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    @BindView(R.id.account_sip_switch) CompoundButton mVoipSwitch;
    @BindView(R.id.account_sip_id_container) View mSipIdContainer;
    @BindView(R.id.account_sip_id_edit_text) EditText mSipIdEditText;

    @BindView(R.id.account_mobile_number_edit_text) EditText mMobileNumberEditText;
    @BindView(R.id.account_outgoing_number_edit_text) EditText mOutgoingNumberEditText;

    @BindView(R.id.use_3g_switch) CompoundButton mUse3GSwitch;

    @BindView(R.id.call_connection_spinner) Spinner mConnectionSpinner;
    @BindView(R.id.connection_container) View connectionContainer;
    @BindView(R.id.codec_spinner) Spinner mCodecSpinner;

    @BindView(R.id.remote_logging_switch) CompoundButton mRemoteLoggingSwitch;
    @BindView(R.id.remote_logging_id_container) View mRemoteLogIdContainer;
    @BindView(R.id.remote_logging_id_edit_text) EditText mRemoteLogIdEditText;
    @BindView(R.id.ignore_battery_optimization_switch) CompoundButton ignoreBatteryOptimizationSwitch;

    @BindView(R.id.advanced_settings_layout) LinearLayout advancedSettings;
    @BindView(R.id.tls_switch) Switch tlsSwitch;
    @BindView(R.id.stun_switch) Switch stunSwitch;

    private PhoneAccount mPhoneAccount;
    private PhoneAccountHelper mPhoneAccountHelper;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;
    private VoipgridApi mVoipgridApi;
    private Logger mLogger;
    private ClipboardHelper mClipboardHelper;
    private BroadcastReceiverManager mBroadcastReceiverManager;

    private boolean mEditMode = false;
    private boolean mIsSetupComplete = false;
    private BroadcastReceiver mVoipDisabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAndPopulate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mJsonStorage = new JsonStorage(this);
        mPhoneAccountHelper = new PhoneAccountHelper(this);
        mPreferences = new Preferences(this);
        mVoipgridApi = ServiceGenerator.createApiService(this);
        mLogger = new Logger(this.getClass());
        mClipboardHelper =  ClipboardHelper.fromContext(this);
        mBroadcastReceiverManager = BroadcastReceiverManager.fromContext(this);

        setupActionBar();

        initConnectionSpinner();
        initCodecSpinner();
        initRemoteLoggingSwitch();
        initUse3GSwitch();
    }

    @Override
    public void onResume() {
        super.onResume();
        initIgnoreBatteryOptimizationSwitch();

        // Update and populate the fields.
        updateAndPopulate();

        // Update phone account and systemuser.
        updateSystemUserAndPhoneAccount();

        initializeAdvancedSettings();

        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(mVoipDisabledReceiver, FcmMessagingService.VOIP_HAS_BEEN_DISABLED);

        if (Build.VERSION.SDK_INT >= BuildConfig.ANDROID_Q_SDK_VERSION) {
            mConnectionSpinner.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBroadcastReceiverManager.unregisterReceiver(mVoipDisabledReceiver);
    }

    private void setupActionBar() {
        /* set the Toolbar to use as ActionBar */
        setSupportActionBar(findViewById(R.id.action_bar));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void initConnectionSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.connection_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mConnectionSpinner.setAdapter(adapter);
        mConnectionSpinner.setSelection(adapter.getPosition(converseFromPreference((mPreferences.getConnectionPreference()), this)));
    }

    private void initCodecSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.codec_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCodecSpinner.setAdapter(adapter);
        mCodecSpinner.setSelection(mPreferences.getAudioCodec() - 1);
    }

    private void initRemoteLoggingSwitch() {
        mRemoteLoggingSwitch.setChecked(mPreferences.remoteLoggingIsActive());
        mRemoteLogIdEditText.setInputType(InputType.TYPE_NULL);
        mRemoteLogIdEditText.setTextIsSelectable(true);
        mRemoteLogIdEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
        });
        mRemoteLogIdEditText.setOnLongClickListener(view -> {
            mClipboardHelper.copyToClipboard(mRemoteLogIdEditText.getText().toString());
            Toast.makeText(SettingsActivity.this,R.string.remote_logging_id_copied , Toast.LENGTH_SHORT).show();
            return true;
        });
        if (mPreferences.remoteLoggingIsActive()) {
            mRemoteLogIdContainer.setVisibility(View.VISIBLE);
            mRemoteLogIdEditText.setText(mPreferences.getLoggerIdentifier());
        }
    }

    private void initUse3GSwitch() {
        mUse3GSwitch.setChecked(mPreferences.has3GEnabled());
    }

    private void initIgnoreBatteryOptimizationSwitch() {
        ignoreBatteryOptimizationSwitch.setOnClickListener(null);
        ignoreBatteryOptimizationSwitch.setChecked(batteryOptimizationManager.isIgnoringBatteryOptimization());
        ignoreBatteryOptimizationSwitch.setOnClickListener(
                view -> batteryOptimizationManager.prompt(SettingsActivity.this));
    }

    /**
     * We are sometimes prompting the user to disable battery optimisation, this callback will be received when they have
     * finished the dialog.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mLogger.i("User has changed ignore battery optimization setting to: " + batteryOptimizationManager.isIgnoringBatteryOptimization());
        initIgnoreBatteryOptimizationSwitch();
    }

    @OnCheckedChanged(R.id.account_sip_switch)
    public void onSipCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mPreferences.hasSipEnabled() == isChecked) return;

        mPreferences.setSipEnabled(isChecked);

        if (!isChecked) {
            // Unregister at middleware.
            MiddlewareHelper.unregister(this);
            // Stop the sipservice.
            stopService(new Intent(this, SipService.class));
            mSipIdContainer.setVisibility(View.GONE);
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
                        SetupActivity.launchToSetVoIPAccount(SettingsActivity.this);
                    }

                    new VoipDisabledNotification().remove();
                }
            }.execute();
        }
    }

    @OnCheckedChanged(R.id.use_3g_switch)
    void on3GCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (mPreferences.has3GEnabled() == isChecked) {
            return;
        }
        mPreferences.set3GEnabled(isChecked);
    }

    @OnItemSelected(R.id.call_connection_spinner)
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String selected = parent.getItemAtPosition(pos).toString();
        mPreferences.setConnectionPreference(converseToPreference(selected, this));
    }

    @OnItemSelected(R.id.codec_spinner)
    public void onCodecSelected(AdapterView<?> parent, View view, int pos, long id) {
        mPreferences.setAudioCodec(pos + 1);
        SecureCalling.fromContext(this).updateApiBasedOnCurrentPreferenceSetting();
    }

    @OnCheckedChanged(R.id.remote_logging_switch)
    void onRemoteLoggingCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (mPreferences.remoteLoggingIsActive() == isChecked) {
            return;
        }
        mPreferences.setRemoteLogging(isChecked);
        if (isChecked) {
            mRemoteLogIdContainer.setVisibility(View.VISIBLE);
            mRemoteLogIdEditText.setText(mPreferences.getLoggerIdentifier());
        } else {
            mRemoteLogIdContainer.setVisibility(View.GONE);
        }

        // If the remote logging setting is changed we want to re-register with the middleware to make sure that
        // the token is configured correctly.
        MiddlewareHelper.registerAtMiddleware(this);
    }

    @OnCheckedChanged(R.id.tls_switch)
    void tlsSwitchChanged(CompoundButton compoundButton, final boolean b) {
        if (!mIsSetupComplete) return;

        SecureCalling secureCalling = SecureCalling.fromContext(this);

        enableProgressBar(true);

        SecureCalling.Callback callback = new SecureCallingUpdatedCallback(b);

        if (b) {
            secureCalling.enable(callback);
        } else {
            secureCalling.disable(callback);
        }
    }

    @OnCheckedChanged(R.id.stun_switch)
    void stunSwitchChanged(CompoundButton compoundButton, boolean b) {
        if (!mIsSetupComplete) return;
        mPreferences.setStunEnabled(b);
        mLogger.i("STUN has been set to: " + b);
        initializeAdvancedSettings();
    }

    private void updateAndPopulate() {
        mSystemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        mPhoneAccount = (PhoneAccount) mJsonStorage.get(PhoneAccount.class);

        populate();
    }

    private void populate() {
        if (mPreferences.hasSipPermission()) {
            mVoipSwitch.setChecked(mPreferences.hasSipEnabled());
            if (mPhoneAccount != null) {
                mSipIdEditText.setText(mPhoneAccount.getAccountId());
            }
        } else {
            mVoipSwitch.setVisibility(View.GONE);
        }

        mSipIdContainer.setVisibility(mPreferences.hasSipEnabled() ? View.VISIBLE : View.GONE);
        enableProgressBar(false);

        if (mSystemUser == null) {
            mLogger.e("Attempted to populate AccountActivity but there does not seem to be a SystemUser available");
            return;
        }

        mMobileNumberEditText.setText(mSystemUser.getMobileNumber());
        mOutgoingNumberEditText.setText(mSystemUser.getOutgoingCli());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

        if (id != R.id.action_edit && id != R.id.action_done) {
            return super.onOptionsItemSelected(item);
        }

        if (isBusyWithApiRequest()) return true;

        if (id == R.id.action_edit) {
            mEditMode = true;
        } else {
            if (isValidNumber()) {
                saveMobileNumber();
            } else {
                DialogHelper.displayAlert(
                        this,
                        getString(R.string.invalid_mobile_number_title),
                        getString(R.string.invalid_mobile_number_message)
                );
            }
        }

        updateUiBasedOnCurrentEditMode();

        return true;
    }

    /**
     * Updates the UI, enabling/disabling fields and changing the menu icon
     * based on the current value of mEditMode.
     */
    private void updateUiBasedOnCurrentEditMode() {
        invalidateOptionsMenu();
        mMobileNumberEditText.setEnabled(mEditMode);
    }

    /**
     * isValidNumber returns true if the number currently entered is a valid phone number.
     */
    private boolean isValidNumber() {
        String mobileNumber = mMobileNumberEditText.getText().toString();

        return PhoneNumberUtils.isValidMobileNumber(PhoneNumberUtils.formatMobileNumber(mobileNumber));
    }

    /**
     * Sends an API request to update the phone number.
     */
    private void saveMobileNumber() {
        mContainer.setFocusableInTouchMode(true);

        String number = PhoneNumberUtils.formatMobileNumber(mMobileNumberEditText.getText().toString());

        mVoipgridApi
                .mobileNumber(new MobileNumber(number))
                .enqueue(new MobileNumberUpdatedCallback(number));

        enableProgressBar(true);
    }

    /**
     * Check if we are currently busy doing an API request.
     *
     * @return TRUE if an api request is in progress
     */
    private boolean isBusyWithApiRequest() {
        return mProgressBar.getVisibility() == View.VISIBLE;
    }

    /**
     * Sets the advanced settings checkboxes to the correct position depending
     * on the current values in shared preferences.
     */
    protected void initializeAdvancedSettings() {
        if (isFinishing()) return;
        mIsSetupComplete = false;
        enableProgressBar(false);
        tlsSwitch.setChecked(mPreferences.hasTlsEnabled());
        stunSwitch.setChecked(mPreferences.hasStunEnabled());
        mIsSetupComplete = true;
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
     * The class that will handle the API response when updating the mobile number.
     */
    private class MobileNumberUpdatedCallback implements Callback<MobileNumber> {

        private String mNumber;

        MobileNumberUpdatedCallback(String number) {
            mNumber = number;
        }

        @Override
        public void onResponse(Call<MobileNumber> call, Response<MobileNumber> response) {
            if (!response.isSuccessful()) {
                failedFeedback();
                return;
            }

            mEditMode = false;

            updateUiBasedOnCurrentEditMode();

            mSystemUser.setMobileNumber(mNumber);

            mJsonStorage.save(mSystemUser);

            populate();
        }

        @Override
        public void onFailure(Call<MobileNumber> call, Throwable t) {
            failedFeedback();
        }

        /**
         * Function to inform the user of a failed requests.
         */
        private void failedFeedback() {
            enableProgressBar(false);

            DialogHelper.displayAlert(
                    SettingsActivity.this,
                    getString(R.string.onboarding_account_configure_failed_title),
                    getString(R.string.onboarding_account_configure_invalid_phone_number)
            );
        }
    }

    /**
     * This class will handle the API response when updating the secure calling setting.
     */
    private class SecureCallingUpdatedCallback implements SecureCalling.Callback {

        private boolean mSwitchEnabled;

        SecureCallingUpdatedCallback(boolean b) {
            mSwitchEnabled = b;
        }

        @Override
        public void onSuccess() {
            mPreferences.setTlsEnabled(mSwitchEnabled);
            mLogger.i("TLS switch has been set to: " + mSwitchEnabled);
            initializeAdvancedSettings();
        }

        @Override
        public void onFail() {
            initializeAdvancedSettings();
        }
    }
}
