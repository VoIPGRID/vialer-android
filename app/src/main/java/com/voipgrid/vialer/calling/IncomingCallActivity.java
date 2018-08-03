package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.LoginRequiredActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class IncomingCallActivity extends AbstractCallActivity {

    @BindView(R.id.incoming_caller_title) TextView mIncomingCallerTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mIncomingCallerSubtitle;
    @BindView(R.id.profile_image) CircleImageView mProfileImage;
    @BindView(R.id.button_decline) ImageButton mButtonDecline;
    @BindView(R.id.button_pickup) ImageButton mButtonPickup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        mMediaManager.startIncomingCallRinger();
        updateViewBasedOnIntent();
    }

    /**
     * Update the labels on the view based on the information from the
     * intent.
     */
    private void updateViewBasedOnIntent() {
        String contactName = null; // TODO this should use the method to lookup the name from the contacts
        String callerId = getCallerIdFromIntent();
        String number = getPhoneNumberFromIntent();

        if (contactName == null) {
            contactName = callerId;
        }

        if (contactName != null) {
            mIncomingCallerTitle.setText(contactName);
            mIncomingCallerSubtitle.setText(number);
            return;
        }

        mIncomingCallerTitle.setText(number);
        mIncomingCallerSubtitle.setText("");
    }

    @OnClick(R.id.button_decline)
    public void onDeclineButtonClicked() {
        mLogger.d("decline");

        if (!mSipServiceConnection.isAvailable()) {
            return;
        }

        if (mSipServiceConnection.get().getCurrentCall() == null) {
            return;
        }

        try {
            mSipServiceConnection.get().getCurrentCall().decline();
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendBroadcast(new Intent(DECLINE_BTN));
        endRinging();
    }

    @OnClick(R.id.button_pickup)
    public void onPickupButtonClicked() {
        if (!mSipServiceConnection.isAvailable() || mSipServiceConnection.get().getCurrentCall() == null) {
            finish();
            return;
        }
        try {
            mSipServiceConnection.get().getCurrentCall().answer();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = getIntent();
        intent.setClass(this, CallActivity.class);
        startActivity(intent);
        mLogger.d("callVisibleForUser");
    }

    @Override
    public void onCallStatusReceived(String status, String callId) {
        super.onCallStatusReceived(status, callId);

        if (!status.equals(CALL_DISCONNECTED_MESSAGE)) {
            return;
        }

        endRinging();
    }

    /**
     * Ends the incoming calling.
     *
     */
    private void endRinging() {
        mMediaManager.stopIncomingCallRinger();
        mButtonDecline.setEnabled(false);
        finish();
    }
}
