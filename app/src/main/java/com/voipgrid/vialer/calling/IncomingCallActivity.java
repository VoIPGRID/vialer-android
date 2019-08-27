package com.voipgrid.vialer.calling;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipService;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.voipgrid.vialer.calling.CallingConstants.CALL_IS_CONNECTED;

public class IncomingCallActivity extends AbstractCallActivity {

    @Inject KeyguardManager mKeyguardManager;
    @Inject Contacts mContacts;
    @Inject CallActivityHelper mCallActivityHelper;

    @BindView(R.id.incoming_caller_title) TextView mIncomingCallerTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mIncomingCallerSubtitle;
    @BindView(R.id.profile_image) CircleImageView mContactImage;
    @BindView(R.id.button_decline) ImageButton mButtonDecline;
    @BindView(R.id.button_pickup) ImageButton mButtonPickup;
    @BindView(R.id.call_buttons) View mCallButtons;

    private boolean ringingIsPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mIncomingCallerTitle, mIncomingCallerSubtitle, getPhoneNumberFromIntent(), getCallerIdFromIntent(), mContactImage);
    }

    private boolean currentlyOnLockScreen() {
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    @OnClick(R.id.button_decline)
    public void onDeclineButtonClicked() {
        mLogger.d("decline");

        disableAllButtons();

        if (!mSipServiceConnection.isAvailable()) {
            return;
        }

        if (mSipServiceConnection.get().getCurrentCall() == null) {
            return;
        }

        SipService.performActionOnSipService(this, SipService.Actions.DECLINE_INCOMING_CALL);

        endRinging();

    }

    @OnClick(R.id.button_pickup)
    public void onPickupButtonClicked() {
        if (!mSipServiceConnection.isAvailable() || mSipServiceConnection.get().getCurrentCall() == null) {
            finish();
            return;
        }

        disableAllButtons();

        SipService.performActionOnSipService(this, SipService.Actions.ANSWER_INCOMING_CALL);
    }

    private void disableAllButtons() {
        mButtonPickup.setEnabled(false);
        mButtonDecline.setEnabled(false);
    }

    /**
     * Start the call activity.
     *
     */
    private void startCallActivity() {
        Intent intent = getIntent();
        intent.setClass(this, CallActivity.class);
        intent.putExtra(CALL_IS_CONNECTED, true);
        startActivity(intent);
        mLogger.d("callVisibleForUser");
    }

    /**
     * Ends the incoming calling.
     *
     */
    private void endRinging() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!allPermissionsGranted(permissions, grantResults)) {
            return;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        ringingIsPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCallButtons.setVisibility(View.VISIBLE);

        if (ringingIsPaused) {
            ringingIsPaused = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSipServiceConnection.disconnect(true);
    }

    @Override
    public void sipServiceHasConnected(SipService sipService) {
        super.sipServiceHasConnected(sipService);
        SipCall call = sipService.getFirstCall();

        if (call == null) {
            finish();
            return;
        }

        call.setCallerId(getCallerIdFromIntent());
        call.setPhoneNumber(getPhoneNumberFromIntent());
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {

    }

    @Override
    public void onCallConnected() {
        mSipServiceConnection.disconnect(true);
        startCallActivity();
    }

    @Override
    public void onCallDisconnected() {
        //TODO: Seems like this is the scenario 1 of missed call.
        mSipServiceConnection.disconnect(true);
        endRinging();
    }

    @Override
    public void onCallHold() {

    }

    @Override
    public void onCallUnhold() {

    }

    @Override
    public void onCallRingingOut() {

    }

    @Override
    public void onCallRingingIn() {

    }

    @Override
    public void onServiceStopped() {

    }
}
