package com.voipgrid.vialer;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.Middleware;
import com.voipgrid.vialer.util.Storage;

import java.io.IOException;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

public class AccountActivity extends AppCompatActivity implements
        Switch.OnCheckedChangeListener,
        Callback {

    private CompoundButton mSwitch;
    private EditText mSipIdEditText;

    private Api mApi;
    private ConnectivityHelper mConnectivityHelper;
    private PhoneAccount mPhoneAccount;
    private Preferences mPreferences;
    private Storage mStorage;
    private SystemUser mSystemUser;

    private boolean mEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mStorage = new Storage(this);
        mSystemUser = (SystemUser) mStorage.get(SystemUser.class);
        mPhoneAccount = (PhoneAccount) mStorage.get(PhoneAccount.class);

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mPreferences = new Preferences(this);

        mApi = ServiceGenerator.createService(
                mConnectivityHelper,
                Api.class,
                getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(
                        this, mSystemUser.getEmail(), mSystemUser.getPassword()))
        );

        /* set the Toolbar to use as ActionBar */
        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);
        
        mSwitch = (CompoundButton) findViewById(R.id.account_sip_switch);
        mSwitch.setOnCheckedChangeListener(this);

        mSipIdEditText = ((EditText) findViewById(R.id.account_sip_id_edit_text));

        populate();
    }

    private void populate() {
        if(mPreferences.hasSipPermission()) {
            mSwitch.setChecked(mPreferences.hasSipEnabled());
            if(!mSwitch.isChecked()) {
                mSipIdEditText.setVisibility(View.GONE);
            }
            if(mPhoneAccount != null) {
                mSipIdEditText.setText(mPhoneAccount.getAccountId());
            }
        } else {
            mSwitch.setVisibility(View.GONE);
            mSipIdEditText.setVisibility(View.GONE);
        }
        ((EditText) findViewById(R.id.account_mobile_number_edit_text))
                .setText(mSystemUser.getMobileNumber());
        ((EditText) findViewById(R.id.account_outgoing_number_edit_text))
                .setText(mSystemUser.getOutgoingCli());
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
                mEditMode = false;
                save();
            }
            invalidateOptionsMenu();
            invalidateEditText();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        findViewById(R.id.container).setFocusableInTouchMode(true);

        String number = ((EditText) findViewById(
                R.id.account_mobile_number_edit_text)).getText().toString();

        mApi.mobileNumber(new MobileNumber(number), this);

        mSystemUser.setMobileNumber(number);

        populate();
    }

    private void invalidateEditText() {
        findViewById(R.id.account_mobile_number_edit_text).setEnabled(mEditMode);
    }

    @Override
    public void success(Object object, Response response) {
            // Success callback for updating mobile number.
            // Update the systemuser.
            mStorage.save(mSystemUser);
    }

    @Override
    public void failure(RetrofitError error) {
        error.printStackTrace();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        /* First, view updates */

        mSipIdEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);

        if (mPreferences.hasSipEnabled() == isChecked) {
            /* nothing changed, so return */
            return;
        }
        mPreferences.setSipEnabled(isChecked);

        if (!isChecked) {
            // Unregister at middleware.
            try {
                // Blocking for now, quickfix for beta testers.
                Middleware.unregister(this);
            } catch (IOException exception) {

            }
        } else {
            // Register. Fix this later in SIP vialer version.
            // TODO: VIALA-364.
        }
    }
}
