package com.voipgrid.vialer;

import static com.voipgrid.vialer.calling.CallingConstants.CALL_BLUETOOTH_ACTIVE;
import static com.voipgrid.vialer.calling.CallingConstants.CALL_BLUETOOTH_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_CONNECTED_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_INCOMING_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_OUTGOING_CALL;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;
import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_INVALID_STATE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_PUT_ON_HOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.CALL_UNHOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.SERVICE_STOPPED;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.Group;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.call.CallActionButton;
import com.voipgrid.vialer.call.CallDetail;
import com.voipgrid.vialer.call.HangupButton;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.CallActivityHelper;
import com.voipgrid.vialer.calling.Dialer;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.media.BluetoothMediaButtonReceiver;
import com.voipgrid.vialer.media.MediaManager;
import com.voipgrid.vialer.permissions.ReadExternalStoragePermission;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;


/**
 * CallActivity for incoming or outgoing call.
 */
public class CallActivity extends AbstractCallActivity implements
        MediaManager.AudioChangedInterface, PopupMenu.OnMenuItemClickListener, Dialer.Listener {

    @BindView(R.id.duration_text_view) TextView mCallDurationView;
    @BindView(R.id.incoming_caller_title) TextView mTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mSubtitle;
    @BindView(R.id.speaker_label) TextView mSpeakerLabel;
    @BindView(R.id.button_hangup) HangupButton mHangupButton;
    @BindView(R.id.call_actions) Group mCallActions;
    @BindView(R.id.dialer) Dialer mDialer;
    @BindView(R.id.profile_image) ImageView mContactImage;
    @BindView(R.id.transfer_label) TextView mTransferLabel;

    @Inject AnalyticsHelper mAnalyticsHelper;
    @Inject CallActivityHelper mCallActivityHelper;

    @BindView(R.id.button_transfer) CallActionButton mTransferButton;
    @BindView(R.id.button_onhold) CallActionButton mOnHoldButton;
    @BindView(R.id.button_mute) CallActionButton mMuteButton;
    @BindView(R.id.button_dialpad) CallActionButton mDialpadButton;
    @BindView(R.id.button_speaker) CallActionButton mSpeakerButton;

    private CallPresenter mCallPresenter;

    private boolean mConnected = false;
    private boolean mMute = false;
    private boolean mOnHold = false;
    private boolean mOnTransfer = false;
    private boolean mSelfHangup = false;

    public String mPhoneNumberToDisplay;
    public String mType;
    public String mCallerIdToDisplay;
    private boolean mCallIsTransferred = false;

    private CallDetail mInitialCallDetail;
    private CallDetail mTransferCallDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);
        mCallPresenter = new CallPresenter(this);

        updateUi();

        // Get the intent to see if it's an outgoing or an incoming call.
        mType = getIntent().getType();

        updateMediaManager(TYPE_CONNECTED_CALL);

        mConnected = getIntent().getBooleanExtra(CALL_IS_CONNECTED, false);

        if (!mBluetoothAudioActive) {
            mBluetoothAudioActive = getIntent().getBooleanExtra(CALL_BLUETOOTH_ACTIVE, false);
        }

        if (!mBluetoothDeviceConnected) {
            mBluetoothDeviceConnected = getIntent().getBooleanExtra(CALL_BLUETOOTH_CONNECTED, false);
        }

        if (!mType.equals(TYPE_INCOMING_CALL) && !mType.equals(TYPE_OUTGOING_CALL)) {
            return;
        }

        mPhoneNumberToDisplay = getIntent().getStringExtra(PHONE_NUMBER);
        mCallerIdToDisplay = getIntent().getStringExtra(CONTACT_NAME);
        displayCallInfo();

        if (wasOpenedViaNotification() && !isIncomingCall()) {
            mCallNotifications.callWasOpenedFromNotificationButIsNotIncoming(
                    getCallNotificationDetails());
            updateMediaManager(TYPE_CONNECTED_CALL);
            updateUi();
            return;
        }

        updateMediaManager(mType);

        if (isIncomingCall()) {
            mLogger.d("inComingCall");

            // Ringing event.
            mAnalyticsHelper.sendEvent(
                    getString(R.string.analytics_event_category_call),
                    getString(R.string.analytics_event_action_inbound),
                    getString(R.string.analytics_event_label_ringing)
            );

            if (!ReadExternalStoragePermission.hasPermission(this)) {
                ReadExternalStoragePermission.askForPermission(this);
            }

        } else {
            mLogger.d("outgoingCall");
            mCallNotifications.outgoingCall(getCallNotificationDetails());
        }

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCallNotifications.removeAll();
    }

    private void displayCallInfo() {
        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mTitle, mSubtitle, mPhoneNumberToDisplay, mCallerIdToDisplay, mContactImage);
    }

    /**
     * Based on type show/hide and position the buttons to answer/decline a call.
     *
     * @param type a string containing a call type (INCOMING or OUTGOING)
     */
    private void updateMediaManager(String type) {
        if (type.equals(TYPE_OUTGOING_CALL) || type.equals(TYPE_CONNECTED_CALL)) {
            if (!mOnTransfer) {
                if (type.equals(TYPE_CONNECTED_CALL) && !mConnected) {
                    getMediaManager().callAnswered();
                }
                if (type.equals(TYPE_OUTGOING_CALL) && !mConnected) {
                    getMediaManager().callOutgoing();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        mLogger.d("onBackPressed");
        View hangupButton = findViewById(R.id.button_hangup);
        View lockRingView = findViewById(R.id.lock_ring);

        if (mOnTransfer) {
            // During a transfer
            if (mSipServiceConnection.get().getCurrentCall() == null || mSipServiceConnection.get().getFirstCall() == null) {
                super.onBackPressed();
            } else {
                hangup();
            }
        } else if (mHangupButton != null && mHangupButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
            hangup();
        } else if (hangupButton != null && hangupButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
        } else if (lockRingView != null && lockRingView.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
        } else if (isDialpadVisible()) {
            hideDialpad();
        }
    }

    @Override
    public void bluetoothAudioAvailable(boolean available) {
        super.bluetoothAudioAvailable(available);
        updateUi();
    }

    @Override
    public void bluetoothDeviceConnected(boolean connected) {
        super.bluetoothDeviceConnected(connected);
        updateUi();
    }

    // Toggle the call on speaker when the user presses the button.
    private void toggleSpeaker() {
        mLogger.d("toggleSpeaker");
        getMediaManager().setCallOnSpeaker(!isOnSpeaker());
        updateUi();
    }

    // Mute or un-mute a call when the user presses the button.
    private void toggleMute() {
        mLogger.d("toggleMute");
        mMute = !mMute;
        updateMicrophoneVolume(mMute ? R.integer.mute_microphone_volume_value : R.integer.unmute_microphone_volume_value);
        updateUi();
    }

    // Toggle the hold the call when the user presses the button.
    private void toggleOnHold() {
        mLogger.d("toggleOnHold");
        mOnHold = !mOnHold;
        if (mSipServiceConnection.isAvailable()) {
            try {
                if (mOnTransfer) {
                    mSipServiceConnection.get().getCurrentCall().toggleHold();
                } else {
                    mSipServiceConnection.get().getFirstCall().toggleHold();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void hangup() {
        if (!mSipServiceConnection.isAvailable()) {
            return;
        }

        if (mOnTransfer) {
            callTransferHangupSecondCall();
            updateUi();
            return;
        }

        try {
            mSipServiceConnection.get().getCurrentCall().hangup(true);
            updateUi();
            mSelfHangup = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        finishAfterDelay();
        sendBroadcast(new Intent(BluetoothMediaButtonReceiver.HANGUP_BTN));
    }

    /**
     * Method for setting new  microphone volume value (SIP Rx level):
     * - level 0 means mute:
     * - level 1 means no volume change, so we won't use this.
     * - level 2 mean 100% increase.
     *
     * @param newVolume new volume level for the Rx level of the current media of active call.
     */
    void updateMicrophoneVolume(long newVolume) {
        mLogger.d("updateMicrophoneVolume");
        if (mSipServiceConnection.isAvailable()) {
            mSipServiceConnection.get().getCurrentCall().updateMicrophoneVolume(newVolume);
        }
    }

    @OnClick(R.id.button_mute)
    public void onMuteButtonClick(View view) {
        if (mOnTransfer) {
            if (mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
                toggleMute();
            }
        } else {
            if (mConnected) {
                toggleMute();
            }
        }
    }

    @OnClick(R.id.button_transfer)
    public void onTransferButtonClick(View view) {
        if (!mConnected) {
            return;
        }

        if (mOnTransfer) {
            callTransferConnectTheCalls();
            return;
        }

        if (!mOnHold) {
            onHoldButtonClick(mOnHoldButton);
        }

        Intent intent = new Intent(this, DialerActivity.class);
        intent.putExtra(DialerActivity.EXTRA_RETURN_AS_RESULT, true);
        startActivityForResult(intent, DialerActivity.RESULT_DIALED_NUMBER);
    }

    @OnClick(R.id.button_onhold)
    public void onHoldButtonClick(View view) {
        if (mOnTransfer) {
            if (mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
                toggleOnHold();
            }
        } else {
            if (mConnected) {
                toggleOnHold();
            }
        }
    }

    @OnClick(R.id.button_speaker)
    public void onAudioSourceButtonClick(View view) {
        if (!mBluetoothDeviceConnected) {
            toggleSpeaker();
            return;
        }

        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_audio_source, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    @OnClick(R.id.button_hangup)
    protected void onDeclineButtonClicked() {
        mLogger.i("Hangup the call");
        hangup();
    }

    @OnClick(R.id.button_dialpad)
    void onDialpadButtonClick(View view) {
        mCallActions.setVisibility(View.GONE);
        mDialer.setListener(this);
        mDialer.setVisibility(View.VISIBLE);
    }

    @Override
    public void digitWasPressed(String dtmf) {
        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return;
        }

        try {
            mSipServiceConnection.get().getCurrentCall().dialDtmf(dtmf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callTransferMakeSecondCall(String numberToCall) {
        Uri sipAddressUri = SipUri.sipAddressUri(
                getApplicationContext(),
                PhoneNumberUtils.format(numberToCall)
        );

        mSipServiceConnection.get().makeCall(sipAddressUri, "", numberToCall);
    }

    public void audioLost(boolean lost) {
        super.audioLost(lost);

        if (mSipServiceConnection.get() == null) {
            mLogger.e("mSipService is null");
        } else {
            if (lost) {
                // Don't put the call on hold when there is a native call is ringing.
                if (mConnected && !mSipServiceConnection.get().getNativeCallManager().nativeCallIsRinging()) {
                    onCallHold();
                }
            } else {
                if (mConnected && mSipServiceConnection.get().getCurrentCall() != null && mSipServiceConnection.get().getCurrentCall().isOnHold()) {
                    onCallHold();
                }
            }
        }
    }

    @Override
    public void onCallDurationUpdate(long seconds) {
        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return;
        }

        mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.audio_source_option_phone:
                getMediaManager().useBluetoothAudio(false);
                getMediaManager().setCallOnSpeaker(false);
                break;

            case R.id.audio_source_option_speaker:
                getMediaManager().useBluetoothAudio(false);
                getMediaManager().setCallOnSpeaker(true);
                break;

            case R.id.audio_source_option_bluetooth:
                getMediaManager().useBluetoothAudio(true);
                break;
        }

        updateUi();

        return false;
    }

    /**
     * Hide the dialpad and make the call actions visible again.
     *
     */
    private void hideDialpad() {
        mCallActions.setVisibility(View.VISIBLE);
        mDialer.setVisibility(View.GONE);
    }

    @Override
    public void exitButtonWasPressed() {
        hideDialpad();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            mOnTransfer = false;
            return;
        }

        String number = data.getStringExtra("DIALED_NUMBER");

        if (number == null || number.isEmpty()) {
            return;
        }

        beginCallTransferTo(number);
    }

    private void beginCallTransferTo(String number) {
        mOnTransfer = true;
        callTransferMakeSecondCall(number);
        mOnHold = mSipServiceConnection.get().getCurrentCall().isOnHold();
        mCallActivityHelper.updateLabelsBasedOnPhoneNumber(mTitle, mSubtitle, number, null, mContactImage);
        updateUi();
    }

    @Override
    public void numberWasChanged(String number) {

    }

    public void callTransferHangupSecondCall() {
        try {
            if (mSipServiceConnection.get().getFirstCall().isOnHold()) {
                mSipServiceConnection.get().getCurrentCall().hangup(true);
            } else {
                mSipServiceConnection.get().getFirstCall().hangup(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callTransferConnectTheCalls() {
        try {
            mSipServiceConnection.get().getFirstCall().xFerReplaces(mSipServiceConnection.get().getCurrentCall());
            mAnalyticsHelper.sendEvent(
                    getString(R.string.analytics_event_category_call),
                    getString(R.string.analytics_event_action_transfer),
                    getString(R.string.analytics_event_label_success)
            );
        } catch (Exception e) {
            e.printStackTrace();
            mAnalyticsHelper.sendEvent(
                    getString(R.string.analytics_event_category_call),
                    getString(R.string.analytics_event_action_transfer),
                    getString(R.string.analytics_event_label_fail)
            );
        }

        mCallIsTransferred = true;
    }

    /**
     * Check whether the keypad is currently being presented to the user.
     *
     * @return TRUE if the keypad is on the screen.
     */
    private boolean isDialpadVisible() {
        return mDialer.getVisibility() == View.VISIBLE;
    }

    private boolean isIncomingCall() {
        return mType.equals(TYPE_INCOMING_CALL);
    }

    @Override
    public void sipServiceHasConnected(SipService sipService) {
        super.sipServiceHasConnected(sipService);
        if (isIncomingCall()) {
            onCallConnected();
            updateUi();
        }
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {
        updateUi();
    }

    public boolean hasSecondCall() {
        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return false;
        }

        SipCall intialCall = mSipServiceConnection.get().getFirstCall();
        SipCall transferCall = mSipServiceConnection.get().getCurrentCall();

        return intialCall.getIdentifier().equals(transferCall.getIdentifier());
    }

    @Override
    public void onCallConnected() {
        updateMediaManager(TYPE_CONNECTED_CALL);
        mConnected = true;

        mCallNotifications.update(getCallNotificationDetails(), R.string.callnotification_active_call);

        if (mOnTransfer && mSipServiceConnection.get().getCurrentCall() != null && mSipServiceConnection.get().getFirstCall() != null) {
            mInitialCallDetail = CallDetail.fromSipCall(mSipServiceConnection.get().getFirstCall());
            mTransferCallDetail = CallDetail.fromSipCall(mSipServiceConnection.get().getCurrentCall());
        }

        if (mSipServiceConnection.get().getCurrentCall() != null) {
            VialerStatistics.callWasSuccessfullySetup(mSipServiceConnection.get().getCurrentCall());
        }
    }

    @Override
    public void onCallDisconnected() {
        if (!mConnected && !mSelfHangup) {
            // Call has never been connected. Meaning the dialed number was unreachable.
            sendBroadcast(new Intent(DECLINE_BTN));
        }

        // Stop duration timer.
        mConnected = false;

        // Stop the ringtone and vibrator when the call has been disconnected.
        getMediaManager().stopIncomingCallRinger();

        // When the user is transferring a call.
        if (mOnTransfer) {
            // Transferring is successful done.
            if (mCallIsTransferred) {
                new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setContentText(getString(R.string.call_transfer_complete_success, mInitialCallDetail.getPhoneNumber(), mTransferCallDetail.getPhoneNumber()))
                        .show();

                mOnTransfer = false;
                try {
                    mSipServiceConnection.get().getFirstCall().hangup(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (mSipServiceConnection.get().getCurrentCall() != null) {
                // The "second" call has been disconnected. But there is still a call available.
                String currentCallIdentifier = mSipServiceConnection.get().getCurrentCall().getIdentifier();
                String initialCallIdentifier = mSipServiceConnection.get().getFirstCall().getIdentifier();

                getMediaManager().setCallOnSpeaker(false);

                mConnected = true;

                mTransferButton.setVisibility(View.VISIBLE);

                if (!currentCallIdentifier.equals(initialCallIdentifier)) {
                    mCurrentCallId = mSipServiceConnection.get().getCurrentCall().getIdentifier();
                    mCallerIdToDisplay = mSipServiceConnection.get().getCurrentCall().getCallerId();
                    mPhoneNumberToDisplay = mSipServiceConnection.get().getCurrentCall().getPhoneNumber();
                } else {
                    mCurrentCallId = mSipServiceConnection.get().getFirstCall().getIdentifier();
                    mCallerIdToDisplay = mSipServiceConnection.get().getFirstCall().getCallerId();
                    mPhoneNumberToDisplay = mSipServiceConnection.get().getFirstCall().getPhoneNumber();
                }

                displayCallInfo();

                mOnHold = mSipServiceConnection.get().getCurrentCall().isOnHold();
                mOnTransfer = false;
            } else {
                if (mSipServiceConnection.get() != null && mSipServiceConnection.get().getCurrentCall() == null && mSipServiceConnection.get().getFirstCall() == null) {
                    displayCallInfo();

                    mOnTransfer = false;
                    mOnHold = false;
                }
            }
        } else {
            getMediaManager().callEnded();
            finishAfterDelay();
        }
    }

    @Override
    public void onCallHold() {
        mOnHold = true;
        mCallNotifications.update(getCallNotificationDetails(), R.string.callnotification_on_hold);
    }

    @Override
    public void onCallUnhold() {
        mOnHold = false;
        mCallNotifications.update(getCallNotificationDetails(), R.string.callnotification_active_call);
    }

    @Override
    public void onCallRingingOut() {
    }

    @Override
    public void onCallRingingIn() {

    }

    @Override
    public void onServiceStopped() {
        mConnected = false;
        finishAfterDelay();
    }

    public boolean isOnTransfer() {
        return mOnTransfer;
    }

    public boolean isMuted() {
        return mMute;
    }

    public boolean hasBluetoothDeviceConnected() {
        return mBluetoothDeviceConnected;
    }

    public boolean isOnSpeaker() {
        return getMediaManager().isCallOnSpeaker();
    }

    public boolean isBluetoothAudioActive() {
        return mBluetoothAudioActive;
    }

    private void updateUi() {
        if (mCallPresenter == null) {
            return;
        }

        mCallPresenter.update();
    }
}
