package com.voipgrid.vialer;

import static com.voipgrid.vialer.calling.CallingConstants.CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_INCOMING_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_OUTGOING_CALL;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.voipgrid.vialer.sip.CallDisconnectedReason;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
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
public class CallActivity extends AbstractCallActivity implements PopupMenu.OnMenuItemClickListener, Dialer.Listener {

    @BindView(R.id.duration_text_view) TextView mCallDurationView;
    @BindView(R.id.incoming_caller_title) TextView mTitle;
    @BindView(R.id.incoming_caller_subtitle) TextView mSubtitle;
    @BindView(R.id.speaker_label) TextView mSpeakerLabel;
    @BindView(R.id.button_hangup) HangupButton mHangupButton;
    @BindView(R.id.call_actions) Group mCallActions;
    @BindView(R.id.dialer) Dialer mDialer;
    @BindView(R.id.transfer_label) TextView mTransferLabel;
    @BindView(R.id.call_status) TextView mCallStatusTv;

    @Inject NetworkUtil mNetworkUtil;
    @Inject BroadcastReceiverManager broadcastReceiverManager;

    @BindView(R.id.button_transfer) CallActionButton mTransferButton;
    @BindView(R.id.button_onhold) CallActionButton mOnHoldButton;
    @BindView(R.id.button_mute) CallActionButton mMuteButton;
    @BindView(R.id.button_dialpad) CallActionButton mDialpadButton;
    @BindView(R.id.button_speaker) CallActionButton mSpeakerButton;

    private CallPresenter mCallPresenter;
    private SweetAlertDialog mTransferCompleteDialog;
    private UpdateUiReceiver updateUiReceiver = new UpdateUiReceiver();

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

        mConnected = getIntent().getBooleanExtra(CALL_IS_CONNECTED, false);

        if (!TYPE_INCOMING_CALL.equals(mType) && !TYPE_OUTGOING_CALL.equals(mType)) {
            return;
        }

        mForceDisplayedCallDetails = new DisplayCallDetail(getIntent().getStringExtra(PHONE_NUMBER), getIntent().getStringExtra(CONTACT_NAME));
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();

        if(!mNetworkUtil.isOnline()) {
            NetworkAvailabilityActivity.start();
        }

        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(updateUiReceiver, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED, Intent.ACTION_HEADSET_PLUG);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mTransferCompleteDialog != null) {
            mTransferCompleteDialog.dismiss();
        }

        broadcastReceiverManager.unregisterReceiver(updateUiReceiver);
    }

    @Override
    public void onCallStatusChanged(String status, String callId) {
        updateUi();
    }

    @Override
    public void onCallConnected() {
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
    public void onCallDisconnected(CallDisconnectedReason reason) {
        mOnTransfer = false;

        if (mCallIsTransferred) {
            showCallTransferCompletedDialog();
        }

        if (mSipServiceConnection.isAvailableAndHasActiveCall()) {
            mForceDisplayedCallDetails = null;
            mConnected = true;
            updateUi();
        } else {
            mCallPresenter.showDisconnectedReason(reason);
            mConnected = false;
            finish();
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
        finish();
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

    @Override
    public void onBackPressed() {
        getLogger().d("onBackPressed");

        if (!mSipServiceConnection.isAvailableAndHasActiveCall()) {
            super.onBackPressed();
            return;
        }

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

    // Toggle the call on speaker when the user presses the button.
    private void toggleSpeaker() {
        getLogger().d("toggleSpeaker");
        if (!getAudioRouter().isCurrentlyRoutingAudioViaSpeaker()) {
            getAudioRouter().routeAudioViaSpeaker();
        } else {
            getAudioRouter().routeAudioViaEarpiece();
        }
        updateUi();
    }

    // Toggle the hold the call when the user presses the button.
    private void toggleOnHold() {
        getLogger().d("toggleOnHold");
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
        } catch (Exception ignored) { }
        finally {
            finish();
        }
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
        if (!getAudioRouter().isBluetoothRouteAvailable()) {
            toggleSpeaker();
            return;
        }

        BluetoothDevice bluetoothDevice = getAudioRouter().getConnectedBluetoothHeadset();

        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_audio_source, popup.getMenu());
        if (bluetoothDevice != null) {
            MenuItem menuItem = popup.getMenu().getItem(2);
            menuItem.setTitle(menuItem + " (" + bluetoothDevice.getName() + ")");
        }
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    @OnClick(R.id.button_hangup)
    protected void onDeclineButtonClicked() {
        getLogger().i("Hangup the call");
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.audio_source_option_phone:
                getAudioRouter().routeAudioViaEarpiece();
                break;

            case R.id.audio_source_option_speaker:
                getAudioRouter().routeAudioViaSpeaker();
                break;

            case R.id.audio_source_option_bluetooth:
                getAudioRouter().routeAudioViaBluetooth();
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
        if (isFinishing() || mInitialCallDetail == null) {
            return;
        }

        if (mTransferCallDetail != null && mTransferCompleteDialog == null) {
            mTransferCompleteDialog = TransferCompleteDialog.createAndShow(this, mInitialCallDetail.getPhoneNumber(), mTransferCallDetail.getPhoneNumber());
        }
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

    public boolean isOnSpeaker() {
        return getAudioRouter().isCurrentlyRoutingAudioViaSpeaker();
    }

    @Override
    public void finish() {
        if (mTransferCompleteDialog != null && mTransferCompleteDialog.isShowing()) {
            finishAfterTransferDialogIsComplete();
            return;
        }

        super.finish();
    }

    private void finishAfterTransferDialogIsComplete() {
        new Handler().postDelayed(() -> {
            if (mTransferCompleteDialog != null) {
                mTransferCompleteDialog.cancel();
                mTransferCompleteDialog = null;
            }
            finish();
        }, 3000);
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

    /**
     * Updates the UI whenever any events are received.
     *
     */
    private class UpdateUiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            runOnUiThread(CallActivity.this::updateUi);
        }
    }
}
