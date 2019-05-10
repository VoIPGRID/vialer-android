package com.voipgrid.vialer;

import static com.voipgrid.vialer.calling.CallingConstants.CALL_BLUETOOTH_ACTIVE;
import static com.voipgrid.vialer.calling.CallingConstants.CALL_BLUETOOTH_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_CONNECTED_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_INCOMING_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_OUTGOING_CALL;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.voipgrid.vialer.call.CallActionButton;
import com.voipgrid.vialer.call.CallDetail;
import com.voipgrid.vialer.call.DisplayCallDetail;
import com.voipgrid.vialer.call.HangupButton;
import com.voipgrid.vialer.call.TransferCompleteDialog;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.calling.Dialer;
import com.voipgrid.vialer.calling.NetworkAvailabilityActivity;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.media.MediaManager;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.NetworkUtil;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import javax.inject.Inject;

import androidx.constraintlayout.widget.Group;
import butterknife.BindView;
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
    @BindView(R.id.call_status) TextView mCallStatusTv;

    @Inject NetworkUtil mNetworkUtil;

    @BindView(R.id.button_transfer) CallActionButton mTransferButton;
    @BindView(R.id.button_onhold) CallActionButton mOnHoldButton;
    @BindView(R.id.button_mute) CallActionButton mMuteButton;
    @BindView(R.id.button_dialpad) CallActionButton mDialpadButton;
    @BindView(R.id.button_speaker) CallActionButton mSpeakerButton;

    private CallPresenter mCallPresenter;
    private SweetAlertDialog mTransferCompleteDialog;

    private boolean mConnected = false;
    private boolean mOnTransfer = false;
    private String mType;
    private boolean mCallIsTransferred = false;

    private CallDetail mInitialCallDetail;
    private CallDetail mTransferCallDetail;

    /**
     * The call details stored in this property will always be displayed
     * on the screen, this is only used before the appropriate call has been
     * setup correctly, when set to null the information will be pulled from
     * the SipCall object directly.
     *
     */
    private DisplayCallDetail mForceDisplayedCallDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);
        mCallPresenter = new CallPresenter(this);
        updateUi();

        mType = getIntent().getType();

        updateMediaManager(TYPE_CONNECTED_CALL);

        mConnected = getIntent().getBooleanExtra(CALL_IS_CONNECTED, false);

        if (!mBluetoothAudioActive) {
            mBluetoothAudioActive = getIntent().getBooleanExtra(CALL_BLUETOOTH_ACTIVE, false);
        }

        if (!mBluetoothDeviceConnected) {
            mBluetoothDeviceConnected = getIntent().getBooleanExtra(CALL_BLUETOOTH_CONNECTED, false);
        }

        if (!TYPE_INCOMING_CALL.equals(mType) && !TYPE_OUTGOING_CALL.equals(mType)) {
            return;
        }

        mForceDisplayedCallDetails = new DisplayCallDetail(getIntent().getStringExtra(PHONE_NUMBER), getIntent().getStringExtra(CONTACT_NAME));
        updateMediaManager(mType);
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();

        if(!mNetworkUtil.isOnline()) {
          NetworkAvailabilityActivity.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mTransferCompleteDialog != null) {
            mTransferCompleteDialog.dismiss();
        }
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {
        updateUi();
    }

    @Override
    public void onCallConnected() {
        updateMediaManager(TYPE_CONNECTED_CALL);
        mConnected = true;

        mForceDisplayedCallDetails = null;

        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            mInitialCallDetail = CallDetail.fromSipCall(mSipServiceConnection.get().getFirstCall());
        }

        if (mOnTransfer && mSipServiceConnection.get().getCurrentCall() != null && mSipServiceConnection.get().getFirstCall() != null) {
            mTransferCallDetail = CallDetail.fromSipCall(mSipServiceConnection.get().getCurrentCall());
        }

        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            VialerStatistics.callWasSuccessfullySetup(mSipServiceConnection.get().getCurrentCall());
        }

        updateUi();
    }

    @Override
    public void onCallDisconnected() {
        mOnTransfer = false;

        if (mCallIsTransferred) {
            showCallTransferCompletedDialog();
        }

        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            mForceDisplayedCallDetails = null;
            getMediaManager().setCallOnSpeaker(false);
            mConnected = true;
            updateUi();
        } else {
            mConnected = false;
            getMediaManager().callEnded();
            finishAfterDelay();
        }
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
        mConnected = false;
        finishAfterDelay();
    }

    /**
     * Update the UI with the latest call information, this includes buttons and labels.
     *
     */
    private void updateUi() {
        if (mCallPresenter == null) {
            return;
        }

        mCallPresenter.update();
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

        if (mOnTransfer) {
            if (mSipServiceConnection.get().getCurrentCall() == null || mSipServiceConnection.get().getFirstCall() == null) {
                super.onBackPressed();
            } else {
                hangupViaBackButton();
            }
        } else if (mHangupButton != null && mHangupButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
            hangupViaBackButton();
        } else if (isDialpadVisible()) {
            hideDialpad();
        }
    }

    /**
     * Presents a confirmation box before hanging up the call.
     *
     */
    private void hangupViaBackButton() {
        DialogInterface.OnClickListener listener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    hangup();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        new AlertDialog.Builder(this)
                .setMessage(R.string.call_back_button_confirmation)
                .setPositiveButton(R.string.call_back_button_confirmation_yes, listener)
                .setNegativeButton(R.string.call_back_button_confirmation_no, listener)
                .show();
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

    // Toggle the hold the call when the user presses the button.
    private void toggleOnHold() {
        mLogger.d("toggleOnHold");
        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return;
        }

        try {
            SipCall call = mOnTransfer ? mSipServiceConnection.get().getCurrentCall() : mSipServiceConnection.get().getFirstCall();
            call.toggleHold();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hang-up the call, this will only hang-up the current call, not any call on-hold in the background.
     *
     */
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
        } catch (Exception e) {
            finish();
            return;
        }

        finishAfterDelay();
    }

    @OnClick(R.id.button_mute)
    public void onMuteButtonClick(View view) {
        if ((mOnTransfer && mSipServiceConnection.get().getCurrentCall().isConnected()) || mConnected) {
            if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
                mSipServiceConnection.get().getCurrentCall().toggleMute();
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

        if (!isCallOnHold()) {
            onHoldButtonClick(mOnHoldButton);
        }

        Intent intent = new Intent(this, DialerActivity.class);
        intent.putExtra(DialerActivity.EXTRA_RETURN_AS_RESULT, true);
        startActivityForResult(intent, DialerActivity.RESULT_DIALED_NUMBER);
    }

    @OnClick(R.id.button_onhold)
    public void onHoldButtonClick(View view) {
        if ((mOnTransfer && mSipServiceConnection.get().getCurrentCall().isConnected()) || mConnected) {
            if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
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
        mForceDisplayedCallDetails = new DisplayCallDetail(number, null);
        updateUi();
    }

    @Override
    public void numberWasChanged(String number) {

    }

    public void callTransferHangupSecondCall() {
        mForceDisplayedCallDetails = null;
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

    /**
     * This method will connect the two calls that we have setup and send the appropriate analytics events.
     *
     */
    public void callTransferConnectTheCalls() {
        try {
            mSipServiceConnection.get().getFirstCall().xFerReplaces(mSipServiceConnection.get().getCurrentCall());
            mCallIsTransferred = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check whether the keypad is currently being presented to the user.
     *
     * @return TRUE if the keypad is on the screen.
     */
    private boolean isDialpadVisible() {
        return mDialer.getVisibility() == View.VISIBLE;
    }

    /**
     * Returns TRUE if the call that started this activity is an incoming
     * call.
     *
     * @return TRUE if incoming
     */
    private boolean isIncomingCall() {
        return TYPE_INCOMING_CALL.equals(mType);
    }

    @Override
    public void sipServiceHasConnected(SipService sipService) {
        super.sipServiceHasConnected(sipService);
        if (isIncomingCall()) {
            onCallConnected();
        }
        updateUi();
    }

    /**
     * Determine if there are currently two calls, this would suggest a transfer
     * is in progress.
     *
     * @return TRUE if there is a second call, otherwise FALSE
     */
    public boolean hasSecondCall() {
        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return false;
        }

        SipCall initialCall = mSipServiceConnection.get().getFirstCall();
        SipCall transferCall = mSipServiceConnection.get().getCurrentCall();

        return !initialCall.getIdentifier().equals(transferCall.getIdentifier());
    }

    /**
     * Display an alert informing the user that the call merge has occurred successfully.
     *
     */
    private void showCallTransferCompletedDialog() {
        if (isFinishing()) {
            return;
        }

        mTransferCompleteDialog = TransferCompleteDialog.createAndShow(this, mInitialCallDetail.getPhoneNumber(), mTransferCallDetail.getPhoneNumber());
    }

    public boolean isOnTransfer() {
        return mOnTransfer;
    }

    public boolean isMuted() {
        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return mSipServiceConnection.get().getCurrentCall().isMuted();
        }

        return false;
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

    public CallDetail getInitialCallDetail() {
        return mInitialCallDetail;
    }

    public CallDetail getTransferCallDetail() {
        return mTransferCallDetail;
    }

    public DisplayCallDetail getForceDisplayedCallDetails() {
        return mForceDisplayedCallDetails;
    }

    /**
     * Check if the primary call is on hold.
     *
     * @return TRUE if it is on hold, otherwise FALSE.
     */
    public boolean isCallOnHold() {
        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            return mSipServiceConnection.get().getCurrentCall().isOnHold();
        }

        return false;
    }
}
