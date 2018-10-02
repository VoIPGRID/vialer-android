package com.voipgrid.vialer;

import static com.voipgrid.vialer.calling.CallingConstants.CALL_BLUETOOTH_ACTIVE;
import static com.voipgrid.vialer.calling.CallingConstants.CALL_BLUETOOTH_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_ORIGINAL_CALLER_ID;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_ORIGINAL_CALLER_PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_SECOND_CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_TRANSFERRED_PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.TAG_CALL_CONNECTED_FRAGMENT;
import static com.voipgrid.vialer.calling.CallingConstants.TAG_CALL_TRANSFER_COMPLETE_FRAGMENT;
import static com.voipgrid.vialer.calling.CallingConstants.TAG_CALL_TRANSFER_FRAGMENT;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_CONNECTED_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_INCOMING_CALL;
import static com.voipgrid.vialer.calling.CallingConstants.TYPE_OUTGOING_CALL;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;
import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_PUT_ON_HOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.CALL_RINGING_IN_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_RINGING_OUT_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_UNHOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.SERVICE_STOPPED;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.constraint.Group;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.call.CallTransferCompleteFragment;
import com.voipgrid.vialer.call.CallTransferFragment;
import com.voipgrid.vialer.calling.AbstractCallActivity;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.media.BluetoothMediaButtonReceiver;
import com.voipgrid.vialer.media.MediaManager;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.permissions.ReadExternalStoragePermission;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.voipgrid.vialer.calling.Dialer;


/**
 * CallActivity for incoming or outgoing call.
 */
public class CallActivity extends AbstractCallActivity
        implements View.OnClickListener,
        MediaManager.AudioChangedInterface, PopupMenu.OnMenuItemClickListener, Dialer.Listener {

    @BindView(R.id.duration_text_view) TextView mCallDurationView;
    @BindView(R.id.incoming_caller_subtitle) TextView mNumber;
    @BindView(R.id.button_mute) ImageView mMuteButton;
    @BindView(R.id.button_dialpad) ImageView mDialpadButton;
    @BindView(R.id.button_speaker) ImageView mSpeakerButton;
    @BindView(R.id.speaker_label) TextView mSpeakerLabel;
    @BindView(R.id.button_transfer) ImageView mTransferButton;
    @BindView(R.id.button_onhold) ImageView mOnHoldButton;
    @BindView(R.id.button_hangup) ImageButton mHangupButton;
    @BindView(R.id.call_actions) Group mCallActions;
    @BindView(R.id.dialer) Dialer mDialer;
    @BindView(R.id.fragment_container) ViewGroup mFragmentContainer;

    @Inject AnalyticsHelper mAnalyticsHelper;

    private boolean mIsIncomingCall;

    private boolean mConnected = false;
    private boolean mMute = false;
    private boolean mOnHold = false;
    private boolean mKeyPadVisible = false;
    private boolean mOnSpeaker = false;
    private boolean mOnTransfer = false;
    private boolean mSelfHangup = false;
    private String mCurrentCallId;

    public String mPhoneNumberToDisplay;
    public String mType;
    public String mCallerIdToDisplay;
    private String mTransferredNumber;
    private boolean mCallIsTransferred = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        onCallStatesUpdateButtons(SERVICE_STOPPED);

        Intent intent = getIntent();

        // Get the intent to see if it's an outgoing or an incoming call.
        mType = intent.getType();

        toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);

        mConnected = getIntent().getBooleanExtra(CALL_IS_CONNECTED, false);

        if (!mBluetoothAudioActive) {
            mBluetoothAudioActive = getIntent().getBooleanExtra(CALL_BLUETOOTH_ACTIVE, false);
        }

        if (!mBluetoothDeviceConnected) {
            mBluetoothDeviceConnected = getIntent().getBooleanExtra(CALL_BLUETOOTH_CONNECTED, false);
        }

        if (mType.equals(TYPE_INCOMING_CALL) || mType.equals(TYPE_OUTGOING_CALL)) {
            // Update the textView with a number URI.
            mPhoneNumberToDisplay = intent.getStringExtra(PHONE_NUMBER);
            mCallerIdToDisplay = intent.getStringExtra(CONTACT_NAME);

            displayCallInfo();

            mIsIncomingCall = mType.equals(TYPE_INCOMING_CALL);
            Boolean openedFromNotification = intent.getBooleanExtra(NotificationHelper.TAG, false);
            if (openedFromNotification && !mIsIncomingCall) {
                mCallNotifications.callWasOpenedFromNotificationButIsNotIncoming(getCallNotificationDetails());
                toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);
                mCallDurationView.setVisibility(View.VISIBLE);
            } else {
                toggleCallStateButtonVisibility(mType);

                if (mIsIncomingCall) {
                    mLogger.d("inComingCall");

                    mCallNotifications.incomingCall(getCallNotificationDetails());

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
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mOnTransfer && mSipServiceConnection.get() != null && mSipServiceConnection.get().getCurrentCall() != null) {
            if (mSipServiceConnection.get().getCurrentCall().isOnHold()) {
                mLogger.d("SipService has call on hold");
                if (!mOnHold) {
                    mLogger.i("But the activity DOES not have the call on hold. Match the sip service.");
                    mOnHold = true;
                    updateCallButton(R.id.button_onhold, true);
                    onCallStatusUpdate(CALL_PUT_ON_HOLD_ACTION);
                } else if (!mOnHoldButton.isActivated()) {
                    mLogger.i("Call is on hold but the button is not active. Update the button");
                    updateCallButton(R.id.button_onhold, true);
                }
            }
        }

        refreshAudioSourceButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCallNotifications.removeAll();
    }

    private void displayCallInfo() {
        TextView nameTextView = findViewById(R.id.name_text_view);

        if(nameTextView == null || mNumber == null){
            return;
        }
        if (mCallerIdToDisplay != null && !mCallerIdToDisplay.isEmpty()) {
            nameTextView.setText(mCallerIdToDisplay);
            mNumber.setText(mPhoneNumberToDisplay);
        } else {
            nameTextView.setText(mPhoneNumberToDisplay);
            mNumber.setText("");
        }
    }

    /**
     * Based on type show/hide and position the buttons to answer/decline a call.
     *
     * @param type a string containing a call type (INCOMING or OUTGOING)
     */
    private void toggleCallStateButtonVisibility(String type) {
        if (type.equals(TYPE_OUTGOING_CALL) || type.equals(TYPE_CONNECTED_CALL)) {
            // Hide answer, decline = decline.

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

    /**
     * The call has transitioned to another state. We can now visually update the view with
     * extra info or perform required actions.
     *
     * @param newStatus the new interaction state to which we should act.
     * @see SipConstants for the possible states.
     */
    private void onCallStatusUpdate(String newStatus) {
        mLogger.d("onCallStatusUpdate: " + newStatus);

        switch (newStatus) {
            case CALL_CONNECTED_MESSAGE:
                toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);
                mConnected = true;

                mCallNotifications.update(getCallNotificationDetails(), R.string.callnotification_active_call);

                if (mSipServiceConnection.get().getCurrentCall() != null) {
                    VialerStatistics.callWasSuccessfullySetup(mSipServiceConnection.get().getCurrentCall());
                }

                mCallDurationView.setVisibility(View.VISIBLE);
                break;

            case CALL_DISCONNECTED_MESSAGE:
                if (!mConnected && !mSelfHangup) {
                    // Call has never been connected. Meaning the dialed number was unreachable.
                    sendBroadcast(new Intent(DECLINE_BTN));
                } else {
                    // Normal hangup.
                }

                // Stop duration timer.
                mConnected = false;

                // Stop the ringtone and vibrator when the call has been disconnected.
                getMediaManager().stopIncomingCallRinger();

                // When the user is transferring a call.
                if (mOnTransfer) {
                    // Transferring is successful done.
                    if (mCallIsTransferred) {

                        Map<String, String> map = new HashMap<>();
                        map.put(MAP_ORIGINAL_CALLER_ID, mSipServiceConnection.get().getFirstCall().getCallerId());
                        map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipServiceConnection.get().getFirstCall().getPhoneNumber());
                        map.put(MAP_TRANSFERRED_PHONE_NUMBER, mTransferredNumber);

                        swapFragment(TAG_CALL_TRANSFER_COMPLETE_FRAGMENT, map);

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
                        swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);

                        newStatus = mSipServiceConnection.get().getCurrentCall().getCurrentCallState();

                        if (!currentCallIdentifier.equals(initialCallIdentifier)) {
                            mCurrentCallId = mSipServiceConnection.get().getCurrentCall().getIdentifier();
                            mCallerIdToDisplay = mSipServiceConnection.get().getCurrentCall().getCallerId();
                            mPhoneNumberToDisplay = mSipServiceConnection.get().getCurrentCall().getPhoneNumber();
                        } else {
                            mCurrentCallId = mSipServiceConnection.get().getFirstCall().getIdentifier();
                            newStatus = mSipServiceConnection.get().getFirstCall().getCurrentCallState();
                            mCallerIdToDisplay = mSipServiceConnection.get().getFirstCall().getCallerId();
                            mPhoneNumberToDisplay = mSipServiceConnection.get().getFirstCall().getPhoneNumber();
                        }

                        displayCallInfo();

                        mOnHold = mSipServiceConnection.get().getCurrentCall().isOnHold();
                        mOnTransfer = false;
                        updateCallButton(R.id.button_transfer, true);
                        updateCallButton(R.id.button_onhold, true);
                        updateCallButton(R.id.button_mute, true);

                        onCallStatusUpdate(mSipServiceConnection.get().getCurrentCall().getCurrentCallState());
                    } else {
                        if (mSipServiceConnection.get() != null && mSipServiceConnection.get().getCurrentCall() == null && mSipServiceConnection.get().getFirstCall() == null) {
                            swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);
                            displayCallInfo();

                            mOnTransfer = false;
                            mOnHold = false;

                            updateCallButton(R.id.button_transfer, false);
                            updateCallButton(R.id.button_onhold, false);
                            updateCallButton(R.id.button_mute, false);
                            updateCallButton(R.id.button_hangup, false);

                            onCallStatusUpdate(CALL_DISCONNECTED_MESSAGE);
                        }
                    }
                } else {
                    getMediaManager().callEnded();
                    Log.e("TEST123", "361");
                    finishAfterDelay();
                }

                onCallStatesUpdateButtons(newStatus);
                break;

            case CALL_PUT_ON_HOLD_ACTION:
                mOnHold = true;
                // Remove a running timer which shows call duration.
                mCallDurationView.setVisibility(View.GONE);
                updateCallButton(R.id.button_onhold, true);
                mCallNotifications.update(getCallNotificationDetails(), R.string.callnotification_on_hold);
                break;

            case CALL_UNHOLD_ACTION:
                mOnHold = false;
                // Start the running timer which shows call duration.
                mCallDurationView.setVisibility(View.VISIBLE);
                updateCallButton(R.id.button_onhold, true);
                mCallNotifications.update(getCallNotificationDetails(), R.string.callnotification_active_call);
                break;

            case CALL_RINGING_OUT_MESSAGE:
                mSpeakerButton.setEnabled(false);
                mMuteButton.setEnabled(false);
                break;

            case CALL_RINGING_IN_MESSAGE:
                break;

            case SERVICE_STOPPED:
                Log.e("TEST123", "393");

                mConnected = false;
                finishAfterDelay();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        mLogger.d("onBackPressed");
        View hangupButton = findViewById(R.id.button_hangup);
        View lockRingView = findViewById(R.id.lock_ring);

        int count = getFragmentManager().getBackStackEntryCount();

        if (count > 0 && mCallIsTransferred) {
            // When transfer is done.
            getFragmentManager().popBackStack();
            Log.e("TEST123", "412");

            finishAfterDelay();
        } else if (mOnTransfer) {
            // During a transfer.
            if (mSipServiceConnection.get().getCurrentCall() == null || mSipServiceConnection.get().getFirstCall() == null) {
                super.onBackPressed();
            } else {
                String currentCallIdentifier = mSipServiceConnection.get().getCurrentCall().getIdentifier();
                String firstCallIdentifier = mSipServiceConnection.get().getFirstCall().getIdentifier();

                if (!firstCallIdentifier.equals(currentCallIdentifier)) {
                    try {
                        mSipServiceConnection.get().getCurrentCall().hangup(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    mOnTransfer = false;

                    swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);

                    updateCallButton(R.id.button_onhold, true);
                    updateCallButton(R.id.button_transfer, true);
                }
            }
        } else if (mHangupButton != null && mHangupButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
            // In call dialog.
            hangup(R.id.button_hangup);
        } else if (hangupButton != null && hangupButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
        } else if (lockRingView != null && lockRingView.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void bluetoothAudioAvailable(boolean available) {
        super.bluetoothAudioAvailable(available);
        refreshAudioSourceButton();
    }

    @Override
    public void bluetoothDeviceConnected(boolean connected) {
        super.bluetoothDeviceConnected(connected);
        refreshAudioSourceButton();
    }

    // Toggle the call on speaker when the user presses the button.
    private void toggleSpeaker() {
        mLogger.d("toggleSpeaker");
        getMediaManager().setCallOnSpeaker(!getMediaManager().isCallOnSpeaker());
        refreshAudioSourceButton();
    }

    // Mute or un-mute a call when the user presses the button.
    private void toggleMute() {
        mLogger.d("toggleMute");
        mMute = !mMute;
        int newVolume = 0;

         if(mMute) {
             newVolume=getResources().getInteger(R.integer.mute_microphone_volume_value);
             mMuteButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.color_primary, null));
             Drawable newDrawableMute = mMuteButton.getBackground();
             DrawableCompat.setTint(newDrawableMute,Color.WHITE );
             mMuteButton.setBackground(newDrawableMute);

         } else {
             newVolume=getResources().getInteger(R.integer.unmute_microphone_volume_value);
             mMuteButton.clearColorFilter();
             Drawable newDrawableMute = mMuteButton.getBackground();
             DrawableCompat.setTintList(newDrawableMute, null );
             mMuteButton.setBackground(newDrawableMute);
         }
             updateMicrophoneVolume(newVolume);
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
            if (mSipServiceConnection.get().getCurrentCall().isOnHold()) {
                mOnHoldButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.color_primary, null));
                Drawable newDrawableOnHold = mOnHoldButton.getBackground();
                DrawableCompat.setTint(newDrawableOnHold,Color.WHITE );
                mOnHoldButton.setBackground(newDrawableOnHold);
            } else {
                mOnHoldButton.clearColorFilter();
                Drawable newDrawableOnHold = mOnHoldButton.getBackground();
                DrawableCompat.setTintList(newDrawableOnHold,null );
                mOnHoldButton.setBackground(newDrawableOnHold);
            }
        }
    }
    /**
     * Update the state of a single call button.
     *
     * @param viewId        Integer The id of the button to change.
     * @param buttonEnabled Boolean Whether the button needs to be enabled or disabled.
     */
    private void updateCallButton(Integer viewId, boolean buttonEnabled) {
        View muteButton;
        View speakerButton;
        View transferButton;
        View onHoldButton;
        View hangupButton;


        switch (viewId) {
            case R.id.button_mute:
                muteButton = findViewById(viewId);
                muteButton.setActivated(mMute);
                break;

            case R.id.button_speaker:
                speakerButton = findViewById(viewId);
                speakerButton.setActivated(mOnSpeaker);

            case R.id.button_transfer:
                transferButton = findViewById(viewId);
                transferButton.setActivated(mOnTransfer);
                break;

            case R.id.button_onhold:
                onHoldButton = findViewById(viewId);
                onHoldButton.setActivated(mOnHold);
                break;

            case R.id.button_hangup:
                hangupButton = findViewById(viewId);
                if (hangupButton != null && hangupButton.getVisibility() == View.VISIBLE) {
                    hangupButton.setEnabled(false);
                    hangupButton.setClickable(false);
                    hangupButton.setAlpha(
                            buttonEnabled ? 1.0f : 0.5f
                    );
                }
                break;
        }
    }

    public void hangup(Integer viewId) {
        if (mSipServiceConnection.isAvailable()) {
            updateCallButton(viewId, false);
            try {
                mSipServiceConnection.get().getCurrentCall().hangup(true);
                mSelfHangup = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e("TEST123", "570");

            finishAfterDelay();
            sendBroadcast(new Intent(BluetoothMediaButtonReceiver.HANGUP_BTN));
        }
    }

    private void onCallStatesUpdateButtons(String callState) {
        Integer speakerButtonId = R.id.button_speaker;
        Integer microphoneButtonId = R.id.button_mute;
        Integer keypadButtonId = R.id.button_dialpad;
        Integer onHoldButtonId = R.id.button_onhold;
        Integer transferButtonId = R.id.button_transfer;

        View hangupButton = findViewById(R.id.button_hangup);
        View acceptButton = findViewById(R.id.button_pickup);

        switch (callState) {
            case CALL_CONNECTED_MESSAGE:
                updateCallButton(speakerButtonId, true);
                updateCallButton(microphoneButtonId, true);
                updateCallButton(keypadButtonId, true);
                updateCallButton(onHoldButtonId, true);
                updateCallButton(transferButtonId, true);
                break;

            case CALL_DISCONNECTED_MESSAGE:
                if (mIsIncomingCall) {
                    if (hangupButton != null && hangupButton.getVisibility() == View.VISIBLE) {
                        hangupButton.setEnabled(false);
                        hangupButton.setClickable(false);
                        hangupButton.setAlpha(0.5f);
                    }

                    if (acceptButton != null && acceptButton.getVisibility() == View.VISIBLE) {
                        acceptButton.setEnabled(false);
                        acceptButton.setClickable(false);
                        acceptButton.setAlpha(0.5f);
                    }
                }

                updateCallButton(R.id.button_hangup, false);
                updateCallButton(speakerButtonId, false);
                updateCallButton(microphoneButtonId, false);
                updateCallButton(keypadButtonId, false);
                updateCallButton(onHoldButtonId, false);
                updateCallButton(transferButtonId, false);

                if (mKeyPadVisible) {
                    updateCallButton(keypadButtonId, false);
                }

                break;

            case SERVICE_STOPPED:
                updateCallButton(microphoneButtonId, false);
                updateCallButton(keypadButtonId, false);
                updateCallButton(onHoldButtonId, false);
                updateCallButton(transferButtonId, false);
                break;
        }
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        Integer viewId = view.getId();

        // If we have no call we don't want to handle any button clicks from this activity
        // any more.
        if (mSipServiceConnection.get() == null || mSipServiceConnection.get().getCurrentCall() == null) {
            return;
        }

        switch (viewId) {
            case R.id.button_mute:
                if (mOnTransfer) {
                    if (mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
                        toggleMute();
                        updateCallButton(viewId, true);
                    }
                } else {
                    if (mConnected) {
                        toggleMute();
                        updateCallButton(viewId, true);
                    }
                }
                break;

            case R.id.button_transfer:
                if (!mConnected) {
                    break;
                }
                if (!mOnTransfer) {
                    if (!mOnHold) {
                        onClick(findViewById(R.id.button_onhold));
                    }
                    mOnTransfer = true;
                } else {
                    mOnTransfer = false;
                }

                Intent intent = new Intent(this, DialerActivity.class);
                intent.putExtra(DialerActivity.EXTRA_RETURN_AS_RESULT, true);
                startActivityForResult(intent, DialerActivity.RESULT_DIALED_NUMBER);

                break;

            case R.id.button_onhold:
                if (mOnTransfer) {
                    if (mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
                        toggleOnHold();
                        updateCallButton(viewId, true);
                    }
                } else {
                    if (mConnected) {
                        toggleOnHold();
                        updateCallButton(viewId, true);
                    }
                }
                break;
        }
    }

    public void answer() {
        mLogger.d("answer");
        getMediaManager().stopIncomingCallRinger();

        if (MicrophonePermission.hasPermission(this)) {
            if (mSipServiceConnection.isAvailable()) {
                try {
                    mSipServiceConnection.get().getCurrentCall().answer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mAnalyticsHelper.sendEvent(
                        getString(R.string.analytics_event_category_call),
                        getString(R.string.analytics_event_action_inbound),
                        getString(R.string.analytics_event_label_accepted)
                );
                BluetoothMediaButtonReceiver.setCallAnswered(true);

                mCallNotifications.activeCall(getCallNotificationDetails());

                mIsIncomingCall = false;
            }
        } else {
            Toast.makeText(CallActivity.this,
                    getString(R.string.permission_microphone_missing_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void digitWasPressed(String dtmf) {
        if (mSipServiceConnection.isAvailable()) {
            SipCall call = mSipServiceConnection.get().getCurrentCall();
            if (call != null) {
                try {
                    call.dialDtmf(dtmf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void callTransferMakeSecondCall(String numberToCall) {
        Uri sipAddressUri = SipUri.sipAddressUri(
                getApplicationContext(),
                PhoneNumberUtils.format(numberToCall)
        );
        mSipServiceConnection.get().makeCall(sipAddressUri, "", numberToCall);

        mTransferButton.setVisibility(View.GONE);

        mOnHold = mSipServiceConnection.get().getCurrentCall().isOnHold();
        updateCallButton(R.id.button_onhold, false);
        updateCallButton(R.id.button_dialpad, false);
        updateCallButton(R.id.button_mute, false);

        mCurrentCallId = mSipServiceConnection.get().getCurrentCall().getIdentifier();

        mCallerIdToDisplay = mSipServiceConnection.get().getCurrentCall().getCallerId();
        mPhoneNumberToDisplay = mSipServiceConnection.get().getCurrentCall().getPhoneNumber();

        displayCallInfo();
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

    public void hangupFromKeypad() {
        try {
            mSipServiceConnection.get().getCurrentCall().hangup(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callTransferConnectTheCalls() {
        try {
            mTransferredNumber = mSipServiceConnection.get().getCurrentCall().getPhoneNumber();
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

    public void audioLost(boolean lost) {
        super.audioLost(lost);

        if (mSipServiceConnection.get() == null) {
            mLogger.e("mSipService is null");
        } else {
            if (lost) {
                // Don't put the call on hold when there is a native call is ringing.
                if (mConnected && !mSipServiceConnection.get().getNativeCallManager().nativeCallIsRinging()) {
                    onCallStatusUpdate(CALL_PUT_ON_HOLD_ACTION);
                }
            } else {
                if (mConnected && mSipServiceConnection.get().getCurrentCall() != null && mSipServiceConnection.get().getCurrentCall().isOnHold()) {
                    onCallStatusUpdate(CALL_PUT_ON_HOLD_ACTION);
                }
            }
        }
    }

    @Override
    public void onCallStatusReceived(String status, String callId) {
        super.onCallStatusReceived(status, callId);

        if (mCurrentCallId != null) {
            if (!callId.equals(mCurrentCallId)) {
                if (mOnTransfer) {
                    onCallStatusUpdate(status);
                    if (mSipServiceConnection.get().getFirstCall() != null && !mSipServiceConnection.get().getFirstCall().getIsCallConnected()) {
                        swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);
                    }
                }
                return;
            }
        }

        onCallStatusUpdate(status);
        if (!status.equals(SipConstants.CALL_DISCONNECTED_MESSAGE)) {
            onCallStatesUpdateButtons(status);
        }
    }

    @Override
    protected void onPickupButtonClicked() {
        mLogger.i("Pickup call");
        answer();
    }

    @Override
    @OnClick(R.id.button_hangup)
    protected void onDeclineButtonClicked() {
            mLogger.i("Hangup the call");
            hangup(R.id.button_hangup);
    }

    @Override
    public void onCallDurationUpdate(long seconds) {
        if (!mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
            return;
        }

        mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));
    }

    private void refreshAudioSourceButton() {
        if (mSpeakerButton == null) {
            return;
        }

        int image = R.drawable.ic_volume_on_enabled;
        int text = R.string.speaker_label;

        if (mBluetoothDeviceConnected) {
            if (getMediaManager().isCallOnSpeaker()) {
                image = R.drawable.audio_source_dropdown_speaker;
                text = R.string.speaker_label;
            } else if (mBluetoothAudioActive) {
                image = R.drawable.audio_source_dropdown_bluetooth;
                text = R.string.audio_source_option_bluetooth;
            } else {
                image = R.drawable.audio_source_dropdown_phone;
                text = R.string.audio_source_option_phone;
            }
        }
        if(getMediaManager().isCallOnSpeaker()) {
            mSpeakerButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.color_primary, null));
            Drawable newDrawableSpeaker = mSpeakerButton.getBackground();
            DrawableCompat.setTint(newDrawableSpeaker,Color.WHITE );
            mSpeakerButton.setBackground(newDrawableSpeaker);
        } else {
            mSpeakerButton.setAlpha(getMediaManager().isCallOnSpeaker() ? 1.0f : 0.5f);
        }

        mSpeakerLabel.setText(getString(text).toLowerCase());
        mSpeakerButton.setImageResource(image);
    }

    @OnClick(R.id.button_speaker)
    public void audioSourceButtonPressed(View view) {
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

        refreshAudioSourceButton();

        return false;
    }

    @OnClick(R.id.button_dialpad)
    void onDialpadButtonClick(View view) {
        mCallActions.setVisibility(View.GONE);
        mDialer.setListener(this);
        mDialer.setVisibility(View.VISIBLE);
    }

    @Override
    public void exitButtonWasPressed() {
        mCallActions.setVisibility(View.VISIBLE);
        mDialer.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            return;
        }

        String dialedNumber = data.getStringExtra("DIALED_NUMBER");

        if (dialedNumber == null || dialedNumber.isEmpty()) {
            return;
        }

        callTransferMakeSecondCall(dialedNumber);

        Map<String, String> map = new HashMap<>();
        map.put(MAP_ORIGINAL_CALLER_ID, mSipServiceConnection.get().getFirstCall().getCallerId());
        map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipServiceConnection.get().getFirstCall().getPhoneNumber());
        map.put(MAP_SECOND_CALL_IS_CONNECTED, "" + false);
        swapFragment(TAG_CALL_TRANSFER_FRAGMENT, map);
    }

    @Override
    public void numberWasChanged(String number) {

    }

    private void swapFragment(String tag, Map extraInfo) {
        Fragment newFragment = null;

        switch (tag) {
            case TAG_CALL_TRANSFER_FRAGMENT:
                String originalCallerId;
                if (extraInfo.get(MAP_ORIGINAL_CALLER_ID) != null) {
                    originalCallerId = extraInfo.get(MAP_ORIGINAL_CALLER_ID).toString();
                } else {
                    originalCallerId = "";
                }

                Bundle callTransferFragmentBundle = new Bundle();
                callTransferFragmentBundle.putString(CallTransferFragment.ORIGINAL_CALLER_ID, originalCallerId);
                callTransferFragmentBundle.putString(CallTransferFragment.ORIGINAL_CALLER_PHONE_NUMBER, extraInfo.get(MAP_ORIGINAL_CALLER_PHONE_NUMBER).toString());
                callTransferFragmentBundle.putString(CallTransferFragment.SECOND_CALL_IS_CONNECTED, extraInfo.get(MAP_SECOND_CALL_IS_CONNECTED).toString());

                newFragment = new CallTransferFragment();
                newFragment.setArguments(callTransferFragmentBundle);
                break;

            case TAG_CALL_TRANSFER_COMPLETE_FRAGMENT:
                if (extraInfo.get(MAP_ORIGINAL_CALLER_ID) != null) {
                    originalCallerId = extraInfo.get(MAP_ORIGINAL_CALLER_ID).toString();
                } else {
                    originalCallerId = "";
                }
                Bundle callTransferCompleteFragment = new Bundle();
                callTransferCompleteFragment.putString(CallTransferCompleteFragment.ORIGINAL_CALLER_ID, originalCallerId);
                callTransferCompleteFragment.putString(CallTransferCompleteFragment.ORIGINAL_CALLER_PHONE_NUMBER, extraInfo.get(MAP_ORIGINAL_CALLER_PHONE_NUMBER).toString());
                callTransferCompleteFragment.putString(CallTransferCompleteFragment.TRANSFERRED_PHONE_NUMBER, extraInfo.get(MAP_TRANSFERRED_PHONE_NUMBER).toString());

                newFragment = new CallTransferCompleteFragment();
                newFragment.setArguments(callTransferCompleteFragment);
                break;
        }

        if (newFragment != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.addToBackStack(null);

            mFragmentContainer.setVisibility(View.VISIBLE);
            mCallActions.setVisibility(View.GONE);
            transaction.replace(R.id.fragment_container, newFragment, tag).commitAllowingStateLoss();
        }
    }

    @Override
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

    @Override
    public void callTransferConnectTheCalls() {
        try {
            mTransferredNumber = mSipServiceConnection.get().getCurrentCall().getPhoneNumber();
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
}
