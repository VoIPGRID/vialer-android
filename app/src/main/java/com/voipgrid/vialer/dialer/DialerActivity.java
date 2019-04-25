package com.voipgrid.vialer.dialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.calling.Dialer;
import com.voipgrid.vialer.contacts.ContactsSyncTask;
import com.voipgrid.vialer.contacts.SyncUtils;
import com.voipgrid.vialer.t9.ContactsImportProgressUpdater;
import com.voipgrid.vialer.t9.T9Fragment;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DialerActivity extends LoginRequiredActivity implements
        Dialer.Listener,
        T9Fragment.Listener {

    @Inject SharedPreferences mSharedPreferences;
    @Inject AnalyticsHelper mAnalyticsHelper;
    @Inject ConnectivityHelper mConnectivityHelper;
    @Inject JsonStorage mJsonStorage;

    @BindView(R.id.button_call) CallButton callButton;
    @BindView(R.id.bottom) Dialer mDialer;
    @BindView(R.id.top) ViewGroup mTop;
    @BindView(R.id.progress_bar) ContactsProgressBar mProgressBar;
    @BindView(R.id.progress_text) TextView mProgressText;
    @BindView(R.id.contact_processing_container) View mContactsProcessingContainer;
    @BindView(R.id.no_connectivity_container) View noConnectivityContainer;

    private DialHelper dialHelper;
    private T9Fragment mT9Fragment;
    private Thread mContactsProcessingThread;

    /**
     * The key for where the dialer number will be stored
     * when activity returns a result.
     *
     */
    public static final int RESULT_DIALED_NUMBER = 1;

    /**
     * If this extra is present, the dialed number will be returned as a result
     * from the activity rather than making a call. This can be used, for example,
     * when you are trying to prompt the user to provide a transfer destination.
     *
     */
    public static final String EXTRA_RETURN_AS_RESULT = "EXTRA_RETURN_AS_RESULT";

    /**
     * The key in shared preferences where the last dialled number is stored.
     *
     */
    public static final String LAST_DIALED = "last_dialed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);
        dialHelper = DialHelper.fromActivity(this);
        mDialer.setListener(this);
        mT9Fragment = (T9Fragment) getSupportFragmentManager().findFragmentById(R.id.t9_search);
        mT9Fragment.setListener(this);

        preventKeyboardFromBeingDisplayed();
    }

    /**
     * Ensures that a keyboard does not pop-up when pasting into the dialer input field
     *
     */
    private void preventKeyboardFromBeingDisplayed() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ConnectivityHelper.mWifiKilled) {
            mConnectivityHelper.useWifi(this, true);
            ConnectivityHelper.mWifiKilled = false;
        }

        refreshUi();
    }

    private boolean shouldReturnNumberAsResult() {
        return getIntent().getBooleanExtra(EXTRA_RETURN_AS_RESULT, false);
    }

    public void numberWasChanged(String phoneNumber) {
        mT9Fragment.search(phoneNumber);
    }

    @Override
    public void digitWasPressed(String digit) {

    }

    @Override
    public void exitButtonWasPressed() {

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mContactsProcessingThread != null && mContactsProcessingThread.isAlive()) {
            mContactsProcessingThread.interrupt();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
        if (!allPermissionsGranted(permissions, grantResults)) {
            return;
        }

        if (requestCode == this.getResources().getInteger(R.integer.contact_permission_request_code)) {
            SyncUtils.requestContactSync(this);
            Intent intent = getIntent();
            startActivity(intent);
            finish();
        } else if (requestCode == this.getResources().getInteger(R.integer.microphone_permission_request_code)) {
            dialHelper.callAttemptedNumber();
        }
    }

    /**
     * Initiate an outgoing call by starting CallActivity and pass a SipUri based on the number
     *
     * @param number      number to call
     * @param contactName contact name to display
     */
    public void onCallNumber(String number, String contactName) {
        if (shouldReturnNumberAsResult()) {
            Intent intent = new Intent();
            intent.putExtra("DIALED_NUMBER", number);
            setResult(RESULT_DIALED_NUMBER, intent);
            finish();
            return;
        }

        String phoneNumberToCall = PhoneNumberUtils.format(number);
        if (number.length() < 1) {
            Toast.makeText(this, getString(R.string.dialer_invalid_number), Toast.LENGTH_LONG).show();
        } else {
            dialHelper.callNumber(phoneNumberToCall, contactName);
            mSharedPreferences.edit().putString(LAST_DIALED, number).apply();
        }
    }

    @OnClick(R.id.button_call)
    void onCallButtonClicked() {
        String phoneNumber = mDialer.getNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            callButton.setClickable(false);
            onCallNumber(PhoneNumberUtils.format(phoneNumber), null);
        } else {
            // Set last dialed number on call button clicked when number is empty.
            String last_dialed = mSharedPreferences.getString(LAST_DIALED, "");
            mDialer.setNumber(last_dialed);
        }
    }

    @OnClick(R.id.button_dialpad)
    void showKeypad() {
        mDialer.setVisibility(View.VISIBLE);
        findViewById(R.id.button_dialpad).setVisibility(View.INVISIBLE);
        findViewById(R.id.button_call).setVisibility(View.VISIBLE);
    }

    public void refreshUi() {
        callButton.setImageResource(
                shouldReturnNumberAsResult() ?
                        R.drawable.ic_call_transfer :
                        R.drawable.ic_call_white
        );

        if (!hasInternetConnectivity()) {
            noConnectivityContainer.setVisibility(View.VISIBLE);
            mContactsProcessingContainer.setVisibility(View.GONE);
            mT9Fragment.hide();
            mDialer.fadeOut();
            return;
        }

        mDialer.fadeIn();

        if (!ContactsSyncTask.getProgress().isComplete()) {
            if (mContactsProcessingThread == null || !mContactsProcessingThread.isAlive()) {
                mContactsProcessingThread = ContactsImportProgressUpdater.start(this, mProgressText, mProgressBar);
            }
            mContactsProcessingContainer.setVisibility(View.VISIBLE);
            mT9Fragment.hide();
            noConnectivityContainer.setVisibility(View.GONE);
            return;
        }

        mT9Fragment.show();
        noConnectivityContainer.setVisibility(View.GONE);
        mContactsProcessingContainer.setVisibility(View.GONE);
    }


    @Override
    public void onBackPressed() {
        if (isContactsExpanded()) {
            showKeypad();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isContactsExpanded() {
        return mDialer.getVisibility() != View.VISIBLE;
    }

    @Override
    protected void onInternetConnectivityGained() {
        super.onInternetConnectivityGained();
        refreshUi();
    }

    @Override
    protected void onInternetConnectivityLost() {
        super.onInternetConnectivityLost();
        refreshUi();
    }

    @Override
    public void onExpandRequested() {
        if (mDialer.getVisibility() != View.VISIBLE) return;
        mDialer.setVisibility(View.GONE);
        findViewById(R.id.button_dialpad).setVisibility(View.VISIBLE);
        findViewById(R.id.button_call).setVisibility(View.GONE);
    }

    @Override
    public void onContactSelected(String number, String name) {
        onCallNumber(number, name);
    }
}