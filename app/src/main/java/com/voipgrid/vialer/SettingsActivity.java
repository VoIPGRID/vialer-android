package com.voipgrid.vialer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.UserSynchronizer;
import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.middleware.MiddlewareHelper;
import com.voipgrid.vialer.notifications.VoipDisabledNotification;
import com.voipgrid.vialer.onboarding.SingleOnboardingStepActivity;
import com.voipgrid.vialer.onboarding.steps.MissingVoipAccountStep;
import com.voipgrid.vialer.persistence.VoipSettings;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BatteryOptimizationManager;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.ClipboardHelper;
import com.voipgrid.vialer.util.DialogHelper;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnItemSelected;
import kotlin.Unit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.voipgrid.vialer.util.ConnectivityHelper.converseFromPreference;
import static com.voipgrid.vialer.util.ConnectivityHelper.converseToPreference;

public class SettingsActivity extends LoginRequiredActivity {

    @Inject BatteryOptimizationManager batteryOptimizationManager;
    @Inject UserSynchronizer userSynchronizer;

    @BindView(R.id.container) View mContainer;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    @BindView(R.id.account_sip_switch) CompoundButton mVoipSwitch;

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
    @BindView(R.id.use_phones_ringtone_switch) CompoundButton usePhoneRingtoneSwitch;
    @BindView(R.id.use_phones_ringtone_switch_description) TextView usePhoneRingtoneSwitchDescription;

    @BindView(R.id.advanced_settings_layout) LinearLayout advancedSettings;
    @BindView(R.id.tls_switch) Switch tlsSwitch;
    @BindView(R.id.stun_switch) Switch stunSwitch;

    private VoipgridApi mVoipgridApi;
    private Logger mLogger;
    private ClipboardHelper mClipboardHelper;
    private BroadcastReceiverManager mBroadcastReceiverManager;
    private final int RIGHT = 2;

    private boolean mEditMode = false;
    private boolean mIsSetupComplete = false;
    private BroadcastReceiver mVoipDisabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mVoipgridApi = ServiceGenerator.createApiService(this);
        mLogger = new Logger(this.getClass());
        mClipboardHelper = ClipboardHelper.fromContext(this);
        mBroadcastReceiverManager = BroadcastReceiverManager.fromContext(this);

        setupActionBar();

        initConnectionSpinner();
        initCodecSpinner();
        initRemoteLoggingSwitch();
        initUse3GSwitch();
        initMobileNumberEditText();
    }

    @Override
    public void onResume() {
        super.onResume();
        initIgnoreBatteryOptimizationSwitch();
        initUsePhoneRingtoneSwitch();
        userSynchronizer.syncWithCallback(() -> {
            runOnUiThread(() -> {
                updateUi();
                initializeAdvancedSettings();
            });
            return Unit.INSTANCE;
        });

        updateUi();
        initializeAdvancedSettings();

        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(mVoipDisabledReceiver, FcmMessagingService.VOIP_HAS_BEEN_DISABLED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mConnectionSpinner.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBroadcastReceiverManager.unregisterReceiver(mVoipDisabledReceiver);
    }

    private void setupActionBar() {
        // Set the Toolbar to use as ActionBar
        setSupportActionBar(findViewById(R.id.action_bar));

        // Enabled home as up for the Toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Enabled home button for the Toolbar
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    private void initConnectionSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.connection_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mConnectionSpinner.setAdapter(adapter);
        mConnectionSpinner.setSelection(adapter.getPosition(converseFromPreference((User.userPreferences.getConnectionPreference()), this)));
    }

    private void initCodecSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.codec_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCodecSpinner.setAdapter(adapter);
        mCodecSpinner.setSelection(User.voip.getAudioCodec() == VoipSettings.AudioCodec.iLBC ? 0 : 1);
    }

    private void initRemoteLoggingSwitch() {
        mRemoteLoggingSwitch.setChecked(User.remoteLogging.isEnabled());
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
        mRemoteLogIdEditText.setText(User.remoteLogging.getId());
        if (User.remoteLogging.isEnabled()) {
            mRemoteLogIdContainer.setVisibility(View.VISIBLE);
        }
    }

    private void initUse3GSwitch() {
        mUse3GSwitch.setChecked(User.voip.getWantsToUse3GForCalls());
    }

    private void initIgnoreBatteryOptimizationSwitch() {
        ignoreBatteryOptimizationSwitch.setOnClickListener(null);
        ignoreBatteryOptimizationSwitch.setChecked(batteryOptimizationManager.isIgnoringBatteryOptimization());
        ignoreBatteryOptimizationSwitch.setOnClickListener(
                view -> batteryOptimizationManager.prompt(SettingsActivity.this, false));
    }

    private void initUsePhoneRingtoneSwitch() {
        usePhoneRingtoneSwitchDescription.setText(getString(R.string.use_phones_ringtone_switch_description, getString(R.string.app_name)));
        usePhoneRingtoneSwitch.setOnClickListener(null);
        usePhoneRingtoneSwitch.setChecked(User.userPreferences.getUsePhoneRingtone());
        usePhoneRingtoneSwitch.setOnClickListener(view -> User.userPreferences.setUsePhoneRingtone(usePhoneRingtoneSwitch.isChecked()));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initMobileNumberEditText() {
        mMobileNumberEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (mMobileNumberEditText.getCompoundDrawables()[RIGHT] != null) {
                    if (event.getX() >= (mMobileNumberEditText.getRight() - mMobileNumberEditText.getLeft() - mMobileNumberEditText.getCompoundDrawables()[RIGHT].getBounds().width())) {
                        if (isBusyWithApiRequest()) return true;
                        if (mEditMode) {
                            if (isValidNumber()) {
                                saveMobileNumber();
                            } else {
                                DialogHelper.displayAlert(
                                        this,
                                        getString(R.string.invalid_mobile_number_title),
                                        getString(R.string.invalid_mobile_number_message)
                                );
                            }
                        } else {
                            mEditMode = true;
                            updateUiBasedOnCurrentEditMode();
                        }
                    }
                }
            }
            return false;
        });
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
    public void onSipCheckedChanged(CompoundButton button, boolean enable) {
        if (!button.isPressed()) return;

        if (!enable) {
            MiddlewareHelper.unregister(this);
            stopService(new Intent(this, SipService.class));
            User.voip.setHasEnabledSip(false);
            return;
        }

        enableProgressBar(true);
        userSynchronizer.syncWithCallback(() -> {
            if (!User.getHasVoipAccount()) {
                runOnUiThread(() -> SingleOnboardingStepActivity.Companion.launch(SettingsActivity.this,
                        MissingVoipAccountStep.class));
                return Unit.INSTANCE;
            }

            runOnUiThread(() -> {
                User.voip.setHasEnabledSip(true);
                updateUi();
                new VoipDisabledNotification().remove();
            });
            return Unit.INSTANCE;
        });
    }

    @OnCheckedChanged(R.id.use_3g_switch)
    void on3GCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (User.voip.getWantsToUse3GForCalls() == isChecked) {
            return;
        }
        User.voip.setWantsToUse3GForCalls(isChecked);
    }

    @OnItemSelected(R.id.call_connection_spinner)
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String selected = parent.getItemAtPosition(pos).toString();
        User.userPreferences.setConnectionPreference(converseToPreference(selected, this));
    }

    @OnItemSelected(R.id.codec_spinner)
    public void onCodecSelected(AdapterView<?> parent, View view, int pos, long id) {
        User.voip.setAudioCodec(pos == 0 ? VoipSettings.AudioCodec.iLBC : VoipSettings.AudioCodec.OPUS);
        SecureCalling.fromContext(this).updateApiBasedOnCurrentPreferenceSetting();
    }

    @OnCheckedChanged(R.id.remote_logging_switch)
    void onRemoteLoggingCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (User.remoteLogging.isEnabled() == isChecked) {
            return;
        }
        User.remoteLogging.setEnabled(isChecked);
        if (isChecked) {
            mRemoteLogIdContainer.setVisibility(View.VISIBLE);
            mRemoteLogIdEditText.setText(User.remoteLogging.getId());
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
        User.voip.setHasStunEnabled(b);
        mLogger.i("STUN has been set to: " + b);
        initializeAdvancedSettings();
    }

    private void updateUi() {
        if (User.voip.isAccountSetupForSip()) {
            mVoipSwitch.setChecked(User.voip.getHasEnabledSip());
        } else {
            mVoipSwitch.setVisibility(View.GONE);
        }

        enableProgressBar(false);

        if (!User.isLoggedIn()) {
            mLogger.e("Attempted to updateUi AccountActivity but there does not seem to be a SystemUser available");
            return;
        }

        mMobileNumberEditText.setText(User.getVoipgridUser().getMobileNumber());
        mOutgoingNumberEditText.setText(User.getVoipgridUser().getOutgoingCli());
    }

    /**
     * Updates the UI, enabling/disabling fields and changing the menu icon
     * based on the current value of mEditMode.
     */
    private void updateUiBasedOnCurrentEditMode() {
        int enabled = ContextCompat.getColor(getApplicationContext(), R.color.black);
        int disabled = ContextCompat.getColor(getApplicationContext(), R.color.edit_text_disabled_color);
        Drawable checkDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_white_18dp);
        Drawable editDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_mode_edit_white_18dp);
        checkDrawable.setTint(enabled);
        editDrawable.setTint(enabled);

        if (mEditMode) {
            mMobileNumberEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, checkDrawable, null);
            mMobileNumberEditText.setTextColor(enabled);
        } else {
            mMobileNumberEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, editDrawable, null);
            mMobileNumberEditText.setTextColor(disabled);
        }

        mMobileNumberEditText.setFocusableInTouchMode(mEditMode);
        mMobileNumberEditText.setFocusable(mEditMode);
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
        tlsSwitch.setChecked(User.voip.getHasTlsEnabled());
        stunSwitch.setChecked(User.voip.getHasStunEnabled());
        mIsSetupComplete = true;
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

            userSynchronizer.syncWithCallback(() -> {
                runOnUiThread(SettingsActivity.this::updateUi);
                return Unit.INSTANCE;
            });
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
            User.voip.setHasTlsEnabled(mSwitchEnabled);
            mLogger.i("TLS switch has been set to: " + mSwitchEnabled);
            initializeAdvancedSettings();
            ActivityLifecycleTracker.removeEncryptionNotification();
        }

        @Override
        public void onFail() {
            initializeAdvancedSettings();
        }
    }
}
