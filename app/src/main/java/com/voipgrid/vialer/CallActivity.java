package com.voipgrid.vialer;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.dialer.NumberInputView;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.ProximitySensorHelper;
import com.voipgrid.vialer.util.ProximitySensorHelper.ProximitySensorInterface;
import com.voipgrid.vialer.util.RemoteLogger;
import com.wearespindle.spindlelockring.library.LockRing;
import com.wearespindle.spindlelockring.library.OnTriggerListener;


/**
 * CallActivity for incoming or outgoing call.
 */
public class CallActivity extends AppCompatActivity
        implements View.OnClickListener, SipConstants, ProximitySensorInterface,
        AudioManager.OnAudioFocusChangeListener, OnTriggerListener {
    private final static String TAG = CallActivity.class.getSimpleName();
    public static final String TYPE_OUTGOING_CALL = "type-outgoing-call";
    public static final String TYPE_INCOMING_CALL = "type-incoming-call";
    public static final String TYPE_CONNECTED_CALL = "type-connected-call";
    public static final String CONTACT_NAME = "contact-name";
    public static final String PHONE_NUMBER = "phone-number";
    private static final long[] VIBRATOR_PATTERN = {1000L, 1000L};
    // Manager for "on speaker" action.
    private AudioManager mAudioManager;
    private ProximitySensorHelper mProximityHelper;
    private TextView mCallDurationView;
    private TextView mStateView;
    private boolean mIsIncomingCall;
    private boolean mIncomingCallIsRinging = false;

    private boolean mConnected = false;
    private boolean mMute = false;
    private boolean mOnHold = false;
    private boolean mKeyPadVisible = false;
    private boolean mOnSpeaker = false;
    private ViewGroup mKeyPadViewContainer;
    private AnalyticsHelper mAnalyticsHelper;
    private Ringtone mRingtone;
    private Vibrator mVibrator;

    // Keep track of the start time of a call, so we can keep track of its duration.
    long mCallStartTime = 0;
    private RemoteLogger mRemoteLogger;
    private SipService mSipService;
    private boolean mServiceBound = false;

    // Runs without a timer by re-posting this handler at the end of the runnable.
    Handler mCallHandler = new Handler();
    Runnable mCallDurationRunnable = new Runnable() {
        @Override
        public void run() {
            long seconds = (System.currentTimeMillis() - mCallStartTime) / 1000;
            mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));
            // Keep timer running for as long as possible.
            mCallHandler.postDelayed(mCallDurationRunnable, 1000);
        }
    };

    // Broadcast manager to notify all call status listeners and listen for call interactions.
    private LocalBroadcastManager mBroadcastManager;

    // Broadcast receiver for presenting changes in call state to user.
    private BroadcastReceiver mCallStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onCallStatusUpdate(intent.getStringExtra(CALL_STATUS_KEY));
            onCallStatesUpdateButtons(intent.getStringExtra(CALL_STATUS_KEY));
        }
    };

    private BroadcastReceiver mDTMFButtonPressed = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleDTMFButtonPressed(intent.getStringExtra(SipConstants.KEY_PAD_DTMF_TONE));
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            SipService.SipServiceBinder binder = (SipService.SipServiceBinder) service;
            mSipService = binder.getService();
            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };

    private int mPreviousVolume = -1;

    private LockRing mLockRing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRemoteLogger = new RemoteLogger(this);

        mRemoteLogger.d(TAG + " onCreate");

        // Check if we have permission to use the microphone. If not, request it.
        if (!MicrophonePermission.hasPermission(this)) {
            MicrophonePermission.askForPermission(this);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_call);

        // Set the AnalyticsHelper.
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );

        mKeyPadViewContainer = (ViewGroup) findViewById(R.id.key_pad_container);

        // Manager that handles the 'put on speaker' feature
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setSpeakerphoneOn(false); // Set default speaker usage to NO/Off.

        // Make sure what ever audio source is playing by other apps is paused.
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        // Make sure the hardware volume buttons control the volume of the call.
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mProximityHelper = new ProximitySensorHelper(this, this, findViewById(R.id.screen_off));

        mRingtone = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_RINGTONE_URI);

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // fetch a broadcast manager for communication to the sip service
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        mStateView = (TextView) findViewById(R.id.state_text_view);
        mCallDurationView = (TextView) findViewById(R.id.duration_text_view);

        mConnected = false;

        onCallStatesUpdateButtons(SERVICE_STOPPED);

        Intent intent = getIntent();

        /**
         * We are being asked to present account data from a contact entry,
         * but heck: Let's call the contact :D
         */
        String type = intent.getType();
        if (type.equals(TYPE_INCOMING_CALL) || type.equals(TYPE_OUTGOING_CALL)) {
            // Update the textView with a number URI.
            String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
            String contactName = intent.getStringExtra(CONTACT_NAME);
            displayCallInfo(phoneNumber, contactName);

            // Start the SipService which manages all SIP traffic.
            mIsIncomingCall = type.equals(TYPE_INCOMING_CALL);

            toggleCallStateButtonVisibility(type);

            if(mIsIncomingCall) {
                mRemoteLogger.d(TAG + " inComingCall");

                // Ringing event.
                mAnalyticsHelper.sendEvent(
                        getString(R.string.analytics_event_category_call),
                        getString(R.string.analytics_event_action_inbound),
                        getString(R.string.analytics_event_label_ringing)
                );

                mIncomingCallIsRinging = true;
                switch (mAudioManager.getRingerMode()) {
                    case AudioManager.RINGER_MODE_NORMAL:
                        playRingtone(true);
                        break;

                    case AudioManager.RINGER_MODE_VIBRATE:
                        vibrate(true);
                        break;

                    case AudioManager.RINGER_MODE_SILENT:
                        playRingtone(false);
                        vibrate(false);
                        break;
                }
            } else {
                mRemoteLogger.d(TAG + " outgoingCall");
            }
        }
        mProximityHelper.startSensor();
    }

    @Override
    public void onStart() {
        super.onStart();
        mRemoteLogger.d(TAG + " onStart");
        // Register for updates.
        IntentFilter intentFilter = new IntentFilter(ACTION_BROADCAST_CALL_STATUS);
        mBroadcastManager.registerReceiver(mCallStatusReceiver, intentFilter);

        intentFilter = new IntentFilter(
                SipConstants.ACTION_BROADCAST_KEY_PAD_INTERACTION);
        mBroadcastManager.registerReceiver(mDTMFButtonPressed, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRemoteLogger.d(TAG + " onResume");
        // Bind the SipService to the activity.
        bindService(new Intent(this, SipService.class), mConnection, Context.BIND_AUTO_CREATE);

        // Make sure service is bound before updating status.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mServiceBound) {
                    finishWithDelay();
                } else if(mSipService.getCurrentCall() == null) {
                    onCallStatusUpdate(CALL_DISCONNECTED_MESSAGE);
                }
            }
        }, 500);
    }

    @Override
    public void onStop() {
        super.onStop();
        mRemoteLogger.d(TAG + " onStop");
        // Unregister the SipService BroadcastReceiver when the activity pauses.
        mBroadcastManager.unregisterReceiver(mCallStatusReceiver);
        mBroadcastManager.unregisterReceiver(mDTMFButtonPressed);

        if (mServiceBound && (mSipService != null && mSipService.getCurrentCall() == null)) {
            unbindService(mConnection);
            mServiceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRemoteLogger.d(TAG + " onDestroy");
        // Abandon focus so other apps can continue playing.
        mAudioManager.abandonAudioFocus(this);
        mProximityHelper.stopSensor();
    }

    private void displayCallInfo(String phoneNumber, String contactName) {
        if(contactName != null && !contactName.isEmpty()) {
            ((TextView) findViewById(R.id.name_text_view)).setText(contactName);
            ((TextView) findViewById(R.id.number_text_view)).setText(phoneNumber);
        } else {
            ((TextView) findViewById(R.id.name_text_view)).setText(phoneNumber);
        }
    }

    /**
     * Based on type show/hide and position the buttons to answer/decline a call.
     * @param type a string containing a call type (INCOMING or OUTGOING)
     */
    private void toggleCallStateButtonVisibility(String type) {
        View ringingView = findViewById(R.id.ringing);
        View ringingLockScreenView = findViewById(R.id.ringing_lock_screen);
        View connectedView = findViewById(R.id.connected);
        if (type.equals(TYPE_OUTGOING_CALL) || type.equals(TYPE_CONNECTED_CALL)) {
            // Hide answer, decline = decline.
            ringingView.setVisibility(View.GONE);
            ringingLockScreenView.setVisibility(View.GONE);
            connectedView.setVisibility(View.VISIBLE);
            findViewById(R.id.ringing).setVisibility(View.GONE);
            findViewById(R.id.connected).setVisibility(View.VISIBLE);
        } else if (type.equals(TYPE_INCOMING_CALL)) {
            // Hide the connected view.
            connectedView.setVisibility(View.GONE);

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            // Depended on if the user has the screen locked show the slide to answer view or
            // the two buttons to accept / decline a call.
            if (keyguardManager.inKeyguardRestrictedInputMode()) {
                ringingLockScreenView.setVisibility(View.VISIBLE);
                ringingView.setVisibility(View.GONE);
                findViewById(R.id.call_buttons_container).setVisibility(View.INVISIBLE);

                mLockRing = (LockRing) findViewById(R.id.google_lock_ring);
                mLockRing.setOnTriggerListener(this);
                mLockRing.setShowTargetsOnIdle(false);
            } else {
                ringingLockScreenView.setVisibility(View.GONE);
                ringingView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * The call has transitioned to another state. We can now visually update the view with
     * extra info or perform required actions.
     * @param newStatus the new interaction state to which we should act.
     * @see SipConstants for the possible states.
     */
    private void onCallStatusUpdate(String newStatus) {
        mRemoteLogger.d(TAG + " onCallStatusUpdate: " + newStatus);
        switch (newStatus) {
            case CALL_MEDIA_AVAILABLE_MESSAGE:
                onCallStatusUpdate(CALL_STOP_RINGBACK_MESSAGE);
                break;

            case CALL_CONNECTED_MESSAGE:
                toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);
                mStateView.setText(R.string.call_connected);
                mCallStartTime = System.currentTimeMillis();
                mCallHandler.postDelayed(mCallDurationRunnable, 0);
                mConnected = true;
                mIncomingCallIsRinging = false;

                mProximityHelper.updateWakeLock();
                break;

            case CALL_DISCONNECTED_MESSAGE:
                // We got a DISCONNECT. Probably save to stop ring back since it's over.
                onCallStatusUpdate(CALL_STOP_RINGBACK_MESSAGE);

                mAudioManager.setSpeakerphoneOn(false);
                mStateView.setText(R.string.call_ended);

                // Stop duration timer.
                mCallHandler.removeCallbacks(mCallDurationRunnable);
                mConnected = false;
                mIncomingCallIsRinging = false;

                // Stop the ringtone and vibrator when the call has been disconnected.
                playRingtone(false);
                vibrate(false);

                finishWithDelay();
                break;

            case CALL_PUT_ON_HOLD_ACTION:
                // Remove a running timer which shows call duration.
                mCallHandler.removeCallbacks(mCallDurationRunnable);
                mCallDurationView.setVisibility(View.INVISIBLE);
                mStateView.setText(R.string.call_on_hold);
                break;

            case CALL_UNHOLD_ACTION:
                // Start the running timer which shows call duration.
                mCallHandler.postDelayed(mCallDurationRunnable, 0);
                mCallDurationView.setVisibility(View.VISIBLE);
                mStateView.setText(R.string.call_connected);
                break;

            case CALL_RINGING_OUT_MESSAGE:
                mStateView.setText(R.string.call_outgoing);
                findViewById(R.id.button_speaker).setEnabled(false);
                findViewById(R.id.button_microphone).setEnabled(false);
                break;

            case CALL_RINGING_IN_MESSAGE:
                mStateView.setText(R.string.call_incoming);
                break;

            case SERVICE_STOPPED :
                mConnected = false;
                mIncomingCallIsRinging = false;
                finishWithDelay();
                break;
        }
    }



    @Override
    public void onBackPressed() {
        mRemoteLogger.d(TAG + " onBackPressed");
        View hangupButton = findViewById(R.id.button_hangup);
        View declineButton = findViewById(R.id.button_reject);

        if(hangupButton != null && hangupButton.getVisibility() == View.VISIBLE) {
            onClick(hangupButton);
        } else if(declineButton != null && declineButton.getVisibility() == View.VISIBLE) {
            onClick(declineButton);
        } else {
            super.onBackPressed();
        }
    }

    // Toggle the call on speaker when the user presses the button.
    private void toggleSpeaker() {
        mRemoteLogger.d(TAG + " toggleSpeaker");
        mOnSpeaker = !mOnSpeaker;
        mAudioManager.setSpeakerphoneOn(!mAudioManager.isSpeakerphoneOn());
    }

    // Mute or un-mute a call when the user presses the button.
    private void toggleMute() {
        mRemoteLogger.d(TAG + " toggleMute");
        mMute = !mMute;
        long newVolume = (mMute ?               // get new volume based on selection value
                    getResources().getInteger(R.integer.mute_microphone_volume_value) :   // muted
                    getResources().getInteger(R.integer.unmute_microphone_volume_value)); // un-muted
        updateMicrophoneVolume(newVolume);
    }

    // Show or hide the dialPad when the user presses the button.
    private void toggleDialPad() {
        mRemoteLogger.d(TAG + " toggleDialPad");
        mKeyPadVisible = !mKeyPadVisible;
        boolean visible = mKeyPadViewContainer.getVisibility() == View.VISIBLE;
        mKeyPadViewContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        mProximityHelper.updateWakeLock();

        if (!visible) {
            NumberInputView numberInputView = (NumberInputView) findViewById(R.id.number_input_edit_text);
            numberInputView.clear();
        }
    }

    // Toggle the hold the call when the user presses the button.
    private void toggleOnHold() {
        mRemoteLogger.d(TAG + " toggleOnHold");
        mOnHold = !mOnHold;
        if (mServiceBound) {
            mSipService.putOnHold(mSipService.getCurrentCall());
        }
    }

    /**
     * Update the state of a single call button.
     *
     * @param viewId Integer The id of the button to change.
     * @param buttonEnabled Boolean Whether the button needs to be enabled or disabled.
     */
    private void updateCallButton(Integer viewId, boolean buttonEnabled) {
        View speakerButton;
        View microphoneButton;
        View keypadButton;
        View onHoldButton;
        View hangupButton;

        switch (viewId) {
            case R.id.button_speaker:
                speakerButton = findViewById(viewId);
                speakerButton.setActivated(mOnSpeaker);
                speakerButton.setAlpha(
                        buttonEnabled ? mOnSpeaker ? 1.0f : 0.5f : 1.0f
                );
                break;

            case R.id.button_microphone:
                microphoneButton = findViewById(viewId);
                microphoneButton.setActivated(mMute);
                microphoneButton.setAlpha(
                        buttonEnabled ? mMute ? 1.0f : 0.5f : 1.0f
                );
                break;

            case R.id.button_keypad:
                keypadButton = findViewById(viewId);
                keypadButton.setActivated(mKeyPadVisible);
                keypadButton.setAlpha(
                        buttonEnabled ? mKeyPadVisible ? 1.0f : 0.5f : 1.0f
                );
                break;

            case R.id.button_onhold:
                onHoldButton = findViewById(viewId);
                onHoldButton.setActivated(mOnHold);
                onHoldButton.setAlpha(
                        buttonEnabled ? mOnHold ? 1.0f : 0.5f : 1.0f
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
        }
    }

    private void onCallStatesUpdateButtons(String callState) {
        Integer speakerButtonId = R.id.button_speaker;
        Integer microphoneButtonId = R.id.button_microphone;
        Integer keypadButtonId = R.id.button_keypad;
        Integer onHoldButtonId = R.id.button_onhold;

        View declineButton = findViewById(R.id.button_reject);
        View acceptButton = findViewById(R.id.button_pickup);

        switch (callState) {
            case CALL_CONNECTED_MESSAGE:
                updateCallButton(speakerButtonId, true);
                updateCallButton(microphoneButtonId, true);
                updateCallButton(keypadButtonId, true);
                updateCallButton(onHoldButtonId, true);
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

                if (mKeyPadVisible) {
                    toggleDialPad();
                    updateCallButton(keypadButtonId, false);
                }

                break;

            case SERVICE_STOPPED:
                updateCallButton(microphoneButtonId, false);
                updateCallButton(keypadButtonId, false);
                updateCallButton(onHoldButtonId, false);
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
        mRemoteLogger.d(TAG + " updateMicrophoneVolume");
        if (mServiceBound) {
            mSipService.updateMicrophoneVolume(mSipService.getCurrentCall(), newVolume);
        }
    }

    @Override
    public void onClick(View view) {
        Integer viewId = view.getId();

        switch (viewId) {
            case R.id.button_speaker:
                toggleSpeaker();
                updateCallButton(viewId, true);
                break;

            case R.id.button_microphone:
                if (mConnected) {
                    toggleMute();
                    updateCallButton(viewId, true);
                }
                break;

            case R.id.button_keypad:
                if (mConnected) {
                    toggleDialPad();
                    updateCallButton(viewId, true);
                }
                break;

            case R.id.button_onhold:
                if (mConnected) {
                    toggleOnHold();
                    updateCallButton(viewId, true);
                }
                break;

            case R.id.button_hangup:
                if (mServiceBound) {
                    updateCallButton(viewId, false);
                    mSipService.hangUp(mSipService.getCurrentCall(), true);
                    mStateView.setText(R.string.call_hangup);
                    finishWithDelay();
                }
                break;

            case R.id.button_reject:
                decline();
                break;

            case R.id.button_pickup:
                answer();
                break;
        }
    }

    private void answer() {
        mRemoteLogger.d(TAG + " answer");
        playRingtone(false);
        vibrate(false);
        View callButtonsContainer = findViewById(R.id.call_buttons_container);
        if (callButtonsContainer.getVisibility() == View.INVISIBLE) {
            callButtonsContainer.setVisibility(View.VISIBLE);
        }

        if (MicrophonePermission.hasPermission(this)) {
            if (mServiceBound) {
                mSipService.answer(mSipService.getCurrentCall());
                mAnalyticsHelper.sendEvent(
                        getString(R.string.analytics_event_category_call),
                        getString(R.string.analytics_event_action_inbound),
                        getString(R.string.analytics_event_label_accepted)
                );
            }
        } else {
            Toast.makeText(CallActivity.this,
                    getString(R.string.permission_microphone_missing_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void decline() {
        mRemoteLogger.d(TAG + " decline");
        playRingtone(false);
        vibrate(false);
        if (mServiceBound) {
            mSipService.decline(mSipService.getCurrentCall());
            mAnalyticsHelper.sendEvent(
                    getString(R.string.analytics_event_category_call),
                    getString(R.string.analytics_event_action_inbound),
                    getString(R.string.analytics_event_label_declined)
            );

            finishWithDelay();
        }
    }

    private void playRingtone(boolean play) {
        if(mRingtone != null) {
            if(play && !mRingtone.isPlaying()) {
                mRingtone.play();
            } else {
                mRingtone.stop();
            }
        }
    }

    private void vibrate(boolean vibrate) {
        if(mVibrator != null) {
            if(vibrate) {
                mVibrator.vibrate(VIBRATOR_PATTERN, 0);
            } else {
                mVibrator.cancel();
            }
        }
    }

    private void finishWithDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();  // Close this activity after 3 seconds.
            }
        }, 3000);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        mRemoteLogger.d(TAG + " onAudioFocusChange");
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // Check if we have a previous volume on audio gain.
                if (mPreviousVolume != -1) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mPreviousVolume, 0);
                    mPreviousVolume = -1;
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // TODO VIALA-463: Handle the complete loss of audio. Eg: incoming GSM call.
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // TODO VIALA-463: Handle temporary loss of audio. Eg: incoming GSM call.
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Steam voice call max volume = 5. Set it to 1 while ducking.
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, 0);
                break;
        }
    }

    @Override
    public boolean activateProximitySensor() {
        return !mIncomingCallIsRinging && !mKeyPadVisible;
    }

    @Override
    public void onGrabbed(View view, int handle) {}

    @Override
    public void onReleased(View view, int handle) {}

    @Override
    public void onTrigger(View view, int target) {
        final int resId = mLockRing.getResourceIdForTarget(target);
        switch (resId) {
            case R.drawable.ic_lock_ring_answer:
                answer();
                break;

            case R.drawable.ic_lock_ring_decline:
                decline();
                break;
        }

        mLockRing.reset(true);
    }

    @Override
    public void onGrabbedStateChange(View view, int handle) {}

    @Override
    public void onFinishFinalAnimation() {}

    private void handleDTMFButtonPressed(String key) {
        NumberInputView numberInputView = (NumberInputView) findViewById(R.id.number_input_edit_text);
        String currentDTMF = numberInputView.getNumber();
        numberInputView.setNumber(currentDTMF + key);
    }
}
