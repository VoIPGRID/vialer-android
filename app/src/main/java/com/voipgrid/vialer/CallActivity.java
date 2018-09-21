package com.voipgrid.vialer;

import static com.voipgrid.vialer.calling.CallingConstants.CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.CONTACT_NAME;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_ORIGINAL_CALLER_ID;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_ORIGINAL_CALLER_PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_SECOND_CALL_IS_CONNECTED;
import static com.voipgrid.vialer.calling.CallingConstants.MAP_TRANSFERRED_PHONE_NUMBER;
import static com.voipgrid.vialer.calling.CallingConstants.PHONE_NUMBER;
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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.call.CallKeyPadFragment;
import com.voipgrid.vialer.call.CallTransferFragment;
import com.voipgrid.vialer.calling.AbstractCallActivity;
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


/**
 * CallActivity for incoming or outgoing call.
 */
public class CallActivity extends AbstractCallActivity
        implements View.OnClickListener,CallKeyPadFragment.CallKeyPadFragmentListener, CallTransferFragment.CallTransferFragmentListener,
        MediaManager.AudioChangedInterface {

    @BindView(R.id.duration_text_view) TextView mCallDurationView;
    @BindView(R.id.button_mute) ImageView mMuteButton;
    @BindView(R.id.button_onhold) ImageView mOnHoldButton;

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
            getMediaManager().callStarted();
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
                } else if (!findViewById(R.id.button_onhold).isActivated()) {
                    mLogger.i("Call is on hold but the button is not active. Update the button");
                    updateCallButton(R.id.button_onhold, true);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCallNotifications.removeAll();
    }

    private void displayCallInfo() {
        TextView nameTextView = findViewById(R.id.name_text_view);
        TextView numberTextView = findViewById(R.id.incoming_caller_subtitle);

        if(nameTextView == null || numberTextView == null){
            return;
        }
        if (mCallerIdToDisplay != null && !mCallerIdToDisplay.isEmpty()) {
            nameTextView.setText(mCallerIdToDisplay);
            numberTextView.setText(mPhoneNumberToDisplay);
        } else {
            nameTextView.setText(mPhoneNumberToDisplay);
            numberTextView.setText("");
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
                //TODO When redesigning dialpad

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

                if (mOnTransfer && mSipServiceConnection.get().getCurrentCall() != null && mSipServiceConnection.get().getFirstCall() != null) {
                    CallTransferFragment callTransferFragment = (CallTransferFragment)
                            getFragmentManager().findFragmentByTag(TAG_CALL_TRANSFER_FRAGMENT);
                    callTransferFragment.secondCallIsConnected();
                }

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

                        //TODO When redesigning dialpad

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

                        findViewById(R.id.button_transfer).setVisibility(View.VISIBLE);
                        //TODO When redesigning dialpad

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
                        updateCallButton(R.id.button_dialpad, true);
                        updateCallButton(R.id.button_mute, true);

                        onCallStatusUpdate(mSipServiceConnection.get().getCurrentCall().getCurrentCallState());
                    } else {
                        if (mSipServiceConnection.get() != null && mSipServiceConnection.get().getCurrentCall() == null && mSipServiceConnection.get().getFirstCall() == null) {
                            //TODO When redesigning dialpad
                            displayCallInfo();

                            mOnTransfer = false;
                            mOnHold = false;

                            updateCallButton(R.id.button_transfer, false);
                            updateCallButton(R.id.button_onhold, false);
                            updateCallButton(R.id.button_dialpad, false);
                            updateCallButton(R.id.button_mute, false);
                            updateCallButton(R.id.button_hangup, false);

                            onCallStatusUpdate(CALL_DISCONNECTED_MESSAGE);
                        }
                    }
                } else {
                    getMediaManager().callEnded();
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
                findViewById(R.id.button_speaker).setEnabled(false);
                findViewById(R.id.button_mute).setEnabled(false);
                break;

            case CALL_RINGING_IN_MESSAGE:
                break;

            case SERVICE_STOPPED:
                // TODO: This broadcast is not received anymore due to refactor! Solve with transition to fragments.
                mConnected = false;
                finishAfterDelay();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        mLogger.d("onBackPressed");
        View hangupButton = findViewById(R.id.button_hangup);
        View declineButton = findViewById(R.id.button_decline);
        View lockRingView = findViewById(R.id.lock_ring);

        int count = getFragmentManager().getBackStackEntryCount();

        if (count > 0 && mCallIsTransferred) {
            // When transfer is done.
            getFragmentManager().popBackStack();
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

                    //TODO When redesigning dialpad

                    updateCallButton(R.id.button_onhold, true);
                    updateCallButton(R.id.button_transfer, true);
                }
            }
        } else if (hangupButton != null && hangupButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
            // In call dialog.
            hangup(R.id.button_hangup);
        } else if (declineButton != null && declineButton.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
        } else if (lockRingView != null && lockRingView.getVisibility() == View.VISIBLE && mSipServiceConnection.get().getCurrentCall() != null) {
        } else {
            super.onBackPressed();
        }
    }

    // Toggle the call on speaker when the user presses the button.
    private void toggleSpeaker() {
        mLogger.d("toggleSpeaker");
        mOnSpeaker = !mOnSpeaker;
        getMediaManager().setCallOnSpeaker(mOnSpeaker);
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

    // Show or hide the dialPad when the user presses the button.
    private void toggleDialPad() {
        mLogger.d("toggleDialPad");
        mKeyPadVisible = !mKeyPadVisible;

        if (mKeyPadVisible) {
            //TODO When redesigning dialpad
        } else {
            if (mOnTransfer) {
                Map<String, String> map = new HashMap<>();
                map.put(MAP_ORIGINAL_CALLER_ID, mSipServiceConnection.get().getFirstCall().getCallerId());
                map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipServiceConnection.get().getFirstCall().getPhoneNumber());
                map.put(MAP_SECOND_CALL_IS_CONNECTED, "" + true);

                //TODO When redesigning dialpad
            } else {
                //TODO When redesigning dialpad
            }
        }
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
        View speakerButton;
        View muteButton;
        View dialpadButton;
        View onHoldButton;
        View hangupButton;
        View transferButton;

        switch (viewId) {
            case R.id.button_speaker:
                speakerButton = findViewById(viewId);
                speakerButton.setActivated(mOnSpeaker);
                speakerButton.setAlpha(
                        buttonEnabled ? mOnSpeaker ? 1.0f : 0.5f : 1.0f
                );
                break;

            case R.id.button_mute:
                muteButton = findViewById(viewId);
                muteButton.setActivated(mMute);
                break;

            case R.id.button_dialpad:
                dialpadButton = findViewById(viewId);
                dialpadButton.setActivated(mKeyPadVisible);
                dialpadButton.setAlpha(
                        buttonEnabled ? mKeyPadVisible ? 1.0f : 0.5f : 1.0f
                );
                break;

            case R.id.button_onhold:
                onHoldButton = findViewById(viewId);
                onHoldButton.setActivated(mOnHold);
                break;

            case R.id.button_transfer:
                transferButton = findViewById(viewId);
                transferButton.setActivated(mOnTransfer);
                transferButton.setAlpha(
                        buttonEnabled ? mOnTransfer ? 1.0f : 0.5f : 1.0f
                );
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

        View declineButton = findViewById(R.id.button_decline);
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
                    if (declineButton != null && declineButton.getVisibility() == View.VISIBLE) {
                        declineButton.setEnabled(false);
                        declineButton.setClickable(false);
                        declineButton.setAlpha(0.5f);
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
                    toggleDialPad();
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
            case R.id.button_speaker:
                toggleSpeaker();
                updateCallButton(viewId, true);
                break;

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

            case R.id.button_dialpad:
                if (mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
                    toggleDialPad();
                    updateCallButton(viewId, true);
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

                Map<String, String> map = new HashMap<>();
                map.put(MAP_ORIGINAL_CALLER_ID, mSipServiceConnection.get().getFirstCall().getCallerId());
                map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipServiceConnection.get().getFirstCall().getPhoneNumber());
                map.put(MAP_SECOND_CALL_IS_CONNECTED, "" + false);

                //TODO When redesigning dialpad
                mCallDurationView.setVisibility(View.GONE);
                updateCallButton(viewId, true);

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
    public void callKeyPadButtonClicked(String dtmf) {
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

    @Override
    public void callTransferMakeSecondCall(String numberToCall) {
        Uri sipAddressUri = SipUri.sipAddressUri(
                getApplicationContext(),
                PhoneNumberUtils.format(numberToCall)
        );
        mSipServiceConnection.get().makeCall(sipAddressUri, "", numberToCall);

        findViewById(R.id.button_transfer).setVisibility(View.GONE);

        mOnHold = mSipServiceConnection.get().getCurrentCall().isOnHold();
        updateCallButton(R.id.button_onhold, false);
        updateCallButton(R.id.button_dialpad, false);
        updateCallButton(R.id.button_mute, false);

        mCurrentCallId = mSipServiceConnection.get().getCurrentCall().getIdentifier();


        mCallerIdToDisplay = mSipServiceConnection.get().getCurrentCall().getCallerId();
        mPhoneNumberToDisplay = mSipServiceConnection.get().getCurrentCall().getPhoneNumber();

        displayCallInfo();
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
    public void hangupFromKeypad() {
        try {
            mSipServiceConnection.get().getCurrentCall().hangup(true);
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

    @Override
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
                    if (!mSipServiceConnection.get().getFirstCall().getIsCallConnected()) {
                        //TODO When redesigning dialpad
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
        if (mConnected) {
            mLogger.i("Hangup the call");
            hangup(R.id.button_hangup);
        }
    }

    @Override
    public void onCallDurationUpdate(long seconds) {
        if (!mSipServiceConnection.get().getCurrentCall().getIsCallConnected()) {
            return;
        }

        mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));
    }
}
