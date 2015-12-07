package com.voipgrid.vialer;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.squareup.okhttp.OkHttpClient;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.Destination;
import com.voipgrid.vialer.api.models.FixedDestination;
import com.voipgrid.vialer.api.models.MobileNumber;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.Middleware;
import com.voipgrid.vialer.util.Storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

public class AccountActivity extends AppCompatActivity implements
        Switch.OnCheckedChangeListener,
        Callback, AdapterView.OnItemSelectedListener {

    public static final String LOG_TAG = AccountActivity.class.getSimpleName();

    private boolean mEditMode = false;
    private Storage mStorage;
    private SystemUser mSystemUser;
    private PhoneAccount mPhoneAccount;
    private Api mApi;
    private ConnectivityHelper mConnectivityHelper;
    private CompoundButton mSwitch;
    private EditText mSipIdEditText;
    private Preferences mPreferences;
    private String mDestinationId;
    private boolean mFirstTimeOnItemSelected = true;

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

        mApi.getUserDestination(this);
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

        String number = ((EditText) findViewById(R.id.account_mobile_number_edit_text)).getText().toString();

        mApi.mobileNumber(new MobileNumber(number), this);

        mSystemUser.setMobileNumber(number);

        populate();
    }

    private void invalidateEditText() {
        findViewById(R.id.account_mobile_number_edit_text).setEnabled(mEditMode);
    }

    List<Destination> destinationSpinnerArray = new ArrayList();

    @Override
    public void success(Object object, Response response) {
        if(object instanceof VoipGridResponse) {
            Destination notAvailableDestination = new FixedDestination();
            notAvailableDestination.setDescription(getString(R.string.not_available));
            destinationSpinnerArray.add(notAvailableDestination);

            UserDestination userDestination = ((VoipGridResponse<UserDestination>) object).getObjects().get(0);
            mDestinationId = userDestination.getId();


            Destination activeDestination = userDestination.getActiveDestination();

            List<Destination> destinations = userDestination.getDestinations();
            int activeIndex = 0;
            for(int i=0, size=destinations.size(); i<size; i++) {
                Destination destination = destinations.get(i);
                destinationSpinnerArray.add(destination);
                if(activeDestination != null && destination.getId().equals(activeDestination.getId())) {
                    activeIndex = i+1;
                }
            }
            populateSpinner(activeIndex);
        } else {
            mStorage.save(mSystemUser);
        }
    }

    private void populateSpinner(int activeIndex) {
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<Destination> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, destinationSpinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(activeIndex);
        spinner.setOnItemSelectedListener(this);
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
                Log.e(LOG_TAG, "Unregister failed", exception);
            }
        } else {
            // Register. Fix this later in SIP vialer version.
            // TODO: VIALA-364.
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(mFirstTimeOnItemSelected) {
            mFirstTimeOnItemSelected = false;
        } else {
            Destination destination = (Destination) parent.getAdapter().getItem(position);
            SelectedUserDestinationParams params = new SelectedUserDestinationParams();
            params.fixedDestination = destination instanceof FixedDestination ? destination.getId() : null;
            params.phoneAccount = destination instanceof PhoneAccount ? destination.getId() : null;
            mApi.setSelectedUserDestination(mDestinationId, params, this);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
