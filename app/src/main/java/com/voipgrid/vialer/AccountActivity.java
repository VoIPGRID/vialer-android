package com.voipgrid.vialer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.DialogHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.MiddlewareHelper;
import com.voipgrid.vialer.util.PhoneAccountHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends LoginRequiredActivity implements
        Switch.OnCheckedChangeListener, AdapterView.OnItemSelectedListener,
        Callback {

    private CompoundButton mSwitch;
    private CompoundButton m3GSwitch;
    private EditText mSipIdEditText;
    private EditText mRemoteLogIdEditText;

    private PhoneAccount mPhoneAccount;
    private PhoneAccountHelper mPhoneAccountHelper;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;

    private boolean mEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mJsonStorage = new JsonStorage(this);
        mPhoneAccountHelper = new PhoneAccountHelper(this);
        mPreferences = new Preferences(this);

        /* set the Toolbar to use as ActionBar */
        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);
        mRemoteLogIdEditText = (EditText) findViewById(R.id.remote_logging_id_edit_text);
        mRemoteLogIdEditText.setVisibility(View.GONE);
        mSipIdEditText = ((EditText) findViewById(R.id.account_sip_id_edit_text));
        mSwitch = (CompoundButton) findViewById(R.id.account_sip_switch);
        mSwitch.setOnCheckedChangeListener(this);

        initConnectionSpinner();
        initRemoteLoggingSwitch();
        initUse3GSwitch();
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
            mSwitch.setChecked(mPreferences.hasSipEnabled());
            if(mPhoneAccount != null) {
                mSipIdEditText.setText(mPhoneAccount.getAccountId());
            }
        } else {
            mSwitch.setVisibility(View.GONE);
        }
        ((EditText) findViewById(R.id.account_mobile_number_edit_text))
                .setText(mSystemUser.getMobileNumber());
        ((EditText) findViewById(R.id.account_outgoing_number_edit_text))
                .setText(mSystemUser.getOutgoingCli());
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
            MiddlewareHelper.executeUnregisterTask(this);
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
    public void onResponse(Call call, Response response) {
        if (response.isSuccess()) {
            // Success callback for updating mobile number.
            // Update the systemuser.
            mJsonStorage.save(mSystemUser);
        } else {
            failedFeedback();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update and populate the fields.
        updateAndPopulate();

        // Update phone account and systemuser.
        updateSystemUserAndPhoneAccount();

        // When coming back from the SetUpVoipAccountFragment's created webactivity
        // we cannot immediately check if a voipaccount has been set. Processing the
        // fragment and the checks for a permitted sip permission can take 1 up to 3 seconds.
        // For this reason we re-check if the mSwitch has the correct value after a timer.
        enableProgressBar(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwitch.setChecked(mPreferences.hasSipEnabled() && mPreferences.hasPhoneAccount());
                enableProgressBar(false);
            }
        }, 3000);
    }

    @Override
    public void onFailure(Call call, Throwable t) {
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
        View view = findViewById(R.id.progressBar);
        if(view != null) {
            view.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        }
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
}
