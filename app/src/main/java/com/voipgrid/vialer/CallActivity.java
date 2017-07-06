package com.voipgrid.vialer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.call.CallConnectedFragment;
import com.voipgrid.vialer.call.CallIncomingFragment;
import com.voipgrid.vialer.call.CallKeyPadFragment;
import com.voipgrid.vialer.call.CallLockRingFragment;
import com.voipgrid.vialer.call.CallTransferCompleteFragment;
import com.voipgrid.vialer.call.CallTransferFragment;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.permissions.MicrophonePermission;
import com.voipgrid.vialer.permissions.ReadExternalStoragePermission;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.util.CustomReceiver;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.ProximitySensorHelper;
import com.voipgrid.vialer.util.ProximitySensorHelper.ProximitySensorInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.voipgrid.vialer.call.CallTransferCompleteFragment.CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_ID;
import static com.voipgrid.vialer.call.CallTransferCompleteFragment.CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_PHONE_NUMBER;
import static com.voipgrid.vialer.call.CallTransferCompleteFragment.CALL_TRANSFER_COMPLETE_FRAGMENT_TRANSFERRED_PHONE_NUMBER;
import static com.voipgrid.vialer.call.CallTransferFragment.CALL_TRANSFER_FRAGMENT_ORIGINAL_CALLER_ID;
import static com.voipgrid.vialer.call.CallTransferFragment.CALL_TRANSFER_FRAGMENT_ORIGINAL_CALLER_PHONE_NUMBER;
import static com.voipgrid.vialer.call.CallTransferFragment.CALL_TRANSFER_FRAGMENT_SECOND_CALL_IS_CONNECTED;


/**
 * CallActivity for incoming or outgoing call.
 */
public class CallActivity extends AppCompatActivity
        implements View.OnClickListener, SipConstants, ProximitySensorInterface,
        AudioManager.OnAudioFocusChangeListener, CallKeyPadFragment.CallKeyPadFragmentListener,
        CallTransferFragment.CallTransferFragmentListener {
    private final static String TAG = CallActivity.class.getSimpleName();
    public static final String TYPE_OUTGOING_CALL = "type-outgoing-call";
    public static final String TYPE_INCOMING_CALL = "type-incoming-call";
    public static final String TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL = "type-incoming-accept-call-notification";
    public static final String TYPE_CONNECTED_CALL = "type-connected-call";
    public static final String CONTACT_NAME = "contact-name";
    public static final String PHONE_NUMBER = "phone-number";
    private static final long[] VIBRATOR_PATTERN = {1000L, 1000L};

    private static final String TAG_CALL_CONNECTED_FRAGMENT = "callConnectedFragment";
    private static final String TAG_CALL_INCOMING_FRAGMENT = "callIncomingFragment";
    private static final String TAG_CALL_LOCK_RING_FRAGMENT = "callLockRingFragment";
    private static final String TAG_CALL_KEY_PAD_FRAGMENT = "callKeyPadFragment";
    private static final String TAG_CALL_TRANSFER_FRAGMENT = "callTransferFragment";
    private static final String TAG_CALL_TRANSFER_COMPLETE_FRAGMENT = "callTransferCompleteFragment";

    private static final String MAP_ORIGINAL_CALLER_PHONE_NUMBER = "originalCallerPhoneNumber";
    private static final String MAP_ORIGINAL_CALLER_ID = "originalCallerId";
    private static final String MAP_TRANSFERRED_PHONE_NUMBER = "transferredNumber";
    private static final String MAP_SECOND_CALL_IS_CONNECTED = "secondCallIsConnected";

    // Manager for "on speaker" action.
    private AudioManager mAudioManager;
    private ProximitySensorHelper mProximityHelper;
    private TextView mCallDurationView;
    private TextView mStateView;
    private boolean mIsIncomingCall;
    private boolean mIncomingCallIsRinging = false;

    private boolean mConnected = false;
    private boolean mHasConnected = false;
    private boolean mMute = false;
    private boolean mOnHold = false;
    private boolean mKeyPadVisible = false;
    private boolean mOnSpeaker = false;
    private boolean mOnTransfer = false;
    private boolean mBluetoothAudio = false;
    private boolean mBluetoothEnabled = false;
    private boolean mBluetoothDeviceConnected = false;
    private boolean mSelfHangup = false;
    private boolean mPausedRinging = false;
    private AnalyticsHelper mAnalyticsHelper;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private BroadcastReceiver mBroadcastAnswerReceiver;
    private BroadcastReceiver mBroadcastDeclineReceiver;
    private String mCurrentCallId;

    public String mPhoneNumberToDisplay;
    public String mType;
    public String mCallerIdToDisplay;
    private String mTransferredNumber;
    private boolean mCallIsTransferred = false;

    // Keep track of the start time of a call, so we can keep track of its duration.
    long mCallStartTime = 0;
    private RemoteLogger mRemoteLogger;
    private SipService mSipService;
    private boolean mServiceBound = false;

    private NotificationHelper mNotificationHelper;
    private int mNotificationId;

    // Runs without a timer by re-posting this handler at the end of the runnable.
    Handler mCallHandler = new Handler();
    Runnable mCallDurationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSipService != null) {
                if (mSipService.getCurrentCall() != null && mSipService.getFirstCall() != null) {
                    String firstCallIdentifier = mSipService.getFirstCall().getIdentifier();
                    String currentCallIdentifier = mSipService.getCurrentCall().getIdentifier();
                    long seconds;

                    if (firstCallIdentifier.equals(currentCallIdentifier)) {
                        seconds = mSipService.getFirstCall().getCallDuration();
                    } else {
                        seconds = mSipService.getCurrentCall().getCallDuration();
                    }
                    mCallDurationView.setText(DateUtils.formatElapsedTime(seconds));

                    // Keep timer running for as long as possible.
                    mCallHandler.postDelayed(mCallDurationRunnable, 1000);
                }
            }
        }
    };

    // Broadcast manager to notify all call status listeners and listen for call interactions.
    private LocalBroadcastManager mBroadcastManager;

    // Broadcast receiver for presenting changes in call state to user.
    private BroadcastReceiver mCallStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(CALL_STATUS_KEY);

            if (mCurrentCallId != null) {
                if (!intent.getStringExtra(CALL_IDENTIFIER_KEY).equals(mCurrentCallId)) {
                    if (mOnTransfer) {
                        onCallStatusUpdate(status);
                        if (!mSipService.getFirstCall().getIsCallConnected()) {
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
    };

    // Broadcast receiver for the Bluetooth connectivity.
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean isBluetoothConnected = false;
            final String action = intent.getAction();
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    // Handle a Bluetooth on/off event.
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_TURNING_ON:
                            isBluetoothConnected = false;
                            break;
                        case BluetoothAdapter.STATE_ON:
                            isBluetoothConnected = true;
                            break;
                    }
                    mBluetoothEnabled = isBluetoothConnected;
                    break;
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    // Handle a headset device connected action.
                    switch (state) {
                        case BluetoothHeadset.STATE_CONNECTED:
                            isBluetoothConnected = true;
                            break;
                        case BluetoothHeadset.STATE_DISCONNECTED:
                            isBluetoothConnected = false;
                            break;
                    }
                    mBluetoothDeviceConnected = isBluetoothConnected;
                    break;
                default:
                    // Handle a audio device connected action.
                    switch (action) {
                        case BluetoothDevice.ACTION_ACL_CONNECTED:
                            isBluetoothConnected = true;
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            isBluetoothConnected = false;
                            break;
                    }
                    mBluetoothDeviceConnected = isBluetoothConnected;
                    break;
            }

            mBluetoothAudio = isBluetoothConnected;
            toggleAudioOutput();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            SipService.SipServiceBinder binder = (SipService.SipServiceBinder) service;
            mSipService = binder.getService();
            mServiceBound = true;
            if (mSipService.getFirstCall() != null) {
                mCurrentCallId = mSipService.getFirstCall().getIdentifier();
            }

            if (mIsIncomingCall) {
                // Wait one second before setting the callerId and phonenumber
                // so the SipService can set the currentcall.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SipCall firstCall = mSipService.getFirstCall();
                        if (firstCall != null) {
                            firstCall.setCallerId(mCallerIdToDisplay);
                            firstCall.setPhoneNumber(mPhoneNumberToDisplay);
                        }
                    }
                }, 1000);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };

    private int mPreviousVolume = -1;

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

        mNotificationHelper = NotificationHelper.getInstance(this);

        // Set the AnalyticsHelper.
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Make sure what ever audio source is playing by other apps is paused.
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        setInitialBluetoothStatus();

        // Make sure the hardware volume buttons control the volume of the call.
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        // Some devices block microphones that can be used for noise cancellation when
        // speaker is enabled. Since we are using this microphone for calling when the device
        // is on speaker. We want to make sure it does not get disabled. Mode in communication
        // unblocks any blocked microphone.
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mProximityHelper = new ProximitySensorHelper(this, this, findViewById(R.id.screen_off));

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // fetch a broadcast manager for communication to the sip service
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        mStateView = (TextView) findViewById(R.id.state_text_view);
        mCallDurationView = (TextView) findViewById(R.id.duration_text_view);

        mConnected = false;

        onCallStatesUpdateButtons(SERVICE_STOPPED);

        Intent intent = getIntent();

        // Get the intent to see if it's an outgoing or an incoming call.
        mType = intent.getType();
        if (mType.equals(TYPE_INCOMING_CALL) || mType.equals(TYPE_OUTGOING_CALL)) {
            // Update the textView with a number URI.
            mPhoneNumberToDisplay = intent.getStringExtra(PHONE_NUMBER);
            mCallerIdToDisplay = intent.getStringExtra(CONTACT_NAME);

            displayCallInfo();

            mIsIncomingCall = mType.equals(TYPE_INCOMING_CALL);
            Boolean openedFromNotification = intent.getBooleanExtra(NotificationHelper.TAG, false);
            if (openedFromNotification && !mIsIncomingCall) {
                mNotificationId = mNotificationHelper.displayCallProgressNotification(
                        getCallerInfo(), getString(R.string.callnotification_active_call), TYPE_CONNECTED_CALL,
                        mCallerIdToDisplay, mPhoneNumberToDisplay, NotificationHelper.mCallNotifyId
                );
                toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);
                // Keep timer running for as long as possible.
                mCallHandler.postDelayed(mCallDurationRunnable, 1000);
                mCallDurationView.setVisibility(View.VISIBLE);
            } else {
                toggleCallStateButtonVisibility(mType);

                if (mIsIncomingCall) {
                    mRemoteLogger.d(TAG + " inComingCall");

                    mNotificationHelper.removeAllNotifications();
                    mNotificationId = mNotificationHelper.displayCallProgressNotification(
                            getCallerInfo(), getString(R.string.callnotification_incoming_call), mType,
                            mCallerIdToDisplay, mPhoneNumberToDisplay, NotificationHelper.mCallNotifyId
                    );

                    // Ringing event.
                    mAnalyticsHelper.sendEvent(
                            getString(R.string.analytics_event_category_call),
                            getString(R.string.analytics_event_action_inbound),
                            getString(R.string.analytics_event_label_ringing)
                    );

                    if (!ReadExternalStoragePermission.hasPermission(this)) {
                        ReadExternalStoragePermission.askForPermission(this);
                    }

                    mIncomingCallIsRinging = true;

                    setRingtoneMediaPlayer();
                    setRingerMode();
                } else {
                    mRemoteLogger.d(TAG + " outgoingCall");
                    mNotificationId = mNotificationHelper.displayCallProgressNotification(
                            getCallerInfo(), getString(R.string.callnotification_dialing), mType,
                            mCallerIdToDisplay, mPhoneNumberToDisplay, NotificationHelper.mCallNotifyId
                    );
                }
            }
        }

        mProximityHelper.startSensor();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == this.getResources().getInteger(R.integer.read_external_storage_permission_request_code)) {
            boolean allPermissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                setRingtoneMediaPlayer();
            }
        }
    }

    private void setRingtoneMediaPlayer() {
        if (ReadExternalStoragePermission.hasPermission(this)) {
            try {
                Uri ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
                if (getResources().getIdentifier("ringtone", "raw", this.getPackageName()) != 0) {
                    String ringtoneLocation = String.format("android.resource://%s/%s/%s", this.getPackageName(), "raw", "ringtone");
                    ringtoneUri = Uri.parse(ringtoneLocation);
                }

                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(this, ringtoneUri);
                if (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.prepare();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createReceivers() {
        mBroadcastAnswerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mConnected || !mIncomingCallIsRinging) {
                    hangup(R.id.button_hangup);
                } else {
                    answer();
                }
            }
        };
        mBroadcastDeclineReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                decline();
            }
        };
    }

    private void registerReceivers() {
        ((AudioManager) getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(
                new ComponentName(this, CustomReceiver.class));
        registerReceiver(mBroadcastAnswerReceiver, new IntentFilter(CustomReceiver.CALL_BTN));
        registerReceiver(mBroadcastDeclineReceiver, new IntentFilter(CustomReceiver.DECLINE_BTN));
    }

    private void unRegisterReceivers() {
        try {
            unregisterReceiver(mBroadcastAnswerReceiver);
            unregisterReceiver(mBroadcastDeclineReceiver);
        } catch (Exception e) {
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mRemoteLogger.d(TAG + " onStart");
        // Register for updates.
        IntentFilter intentFilter = new IntentFilter(ACTION_BROADCAST_CALL_STATUS);
        mBroadcastManager.registerReceiver(mCallStatusReceiver, intentFilter);

        // Intent filter for the Bluetooth receiver.
        intentFilter = new IntentFilter();
        // When the user disables the entire bluetooth connection.
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // When the user connects/disconnects a bluetooth audio device.
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBluetoothReceiver, intentFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String intentType = intent.getType();

        if (intentType != null) {
            if (intentType.equals(CallActivity.TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL)) {
                mPhoneNumberToDisplay = intent.getStringExtra(PHONE_NUMBER);
                mCallerIdToDisplay = intent.getStringExtra(CONTACT_NAME);

                // Answer the call with a small delay to allow the activity to catch up.
                // So the UI will be updated correctly.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        answer();
                        displayCallInfo();
                    }
                }, 500);

                mNotificationHelper.removeNotification(mNotificationId);
                mNotificationId = mNotificationHelper.displayCallProgressNotification(
                        getCallerInfo(), getString(R.string.callnotification_active_call), TYPE_CONNECTED_CALL,
                        mCallerIdToDisplay, mPhoneNumberToDisplay, NotificationHelper.mCallNotifyId
                );
            } else if (intent.getType().equals(TYPE_INCOMING_CALL)) {
                setRingtoneMediaPlayer();
                setRingerMode();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBroadcastAnswerReceiver == null || mBroadcastDeclineReceiver == null) {
            createReceivers();
        }
        registerReceivers();
        mRemoteLogger.d(TAG + " onResume");
        if (mIncomingCallIsRinging && mPausedRinging) {
            if (mMediaPlayer == null) {
                setRingtoneMediaPlayer();
            }
            setRingerMode();
        }
        mPausedRinging = false;

        // Bind the SipService to the activity.
        bindService(new Intent(this, SipService.class), mConnection, Context.BIND_AUTO_CREATE);

        if (!mOnTransfer && mSipService != null && mSipService.getCurrentCall() != null) {
            if (mSipService.getCurrentCall().isOnHold() && !mOnHold) {
                mOnHold = true;
                updateCallButton(R.id.button_onhold, true);
                onCallStatusUpdate(CALL_PUT_ON_HOLD_ACTION);
            }
        }

        // Make sure service is bound before updating status.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mServiceBound) {
                    finishWithDelay();
                } else if (mSipService.getCurrentCall() == null) {
                    onCallStatusUpdate(CALL_DISCONNECTED_MESSAGE);
                }
            }
        }, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIncomingCallIsRinging) {
            mNotificationHelper.removeAllNotifications();
            mNotificationId = mNotificationHelper.displayNotificationWithCallActions(mCallerIdToDisplay, mPhoneNumberToDisplay);
        }
        unRegisterReceivers();
    }

    @Override
    public void onStop() {
        super.onStop();
        mRemoteLogger.d(TAG + " onStop");


        // Unregister the SipService BroadcastReceiver when the activity pauses.
        unregisterReceiver(mBluetoothReceiver);
        mBroadcastManager.unregisterReceiver(mCallStatusReceiver);

        if (mIncomingCallIsRinging) {
            vibrate(false);
            playRingtone(false);
            mPausedRinging = true;
        }

        if (mServiceBound && (mSipService != null && mSipService.getCurrentCall() == null)) {
            unbindService(mConnection);
            mServiceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRemoteLogger.d(TAG + " onDestroy");
        // Try to unbind. Might fail if onStop already unbound the service.
        try {
            unbindService(mConnection);
        } catch (Exception e) {
        }

        mNotificationHelper.removeAllNotifications();

        restoreAudioSettings();
        mProximityHelper.stopSensor();
    }

    /**
     * Function to restore the audio manage to it's original state.
     */
    private void restoreAudioSettings() {
        mAudioManager.setBluetoothScoOn(false);
        mAudioManager.stopBluetoothSco();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mAudioManager.abandonAudioFocus(this);
    }

    private void displayCallInfo() {
        TextView nameTextView = (TextView) findViewById(R.id.name_text_view);
        TextView numberTextView = (TextView) findViewById(R.id.number_text_view);

        if (mCallerIdToDisplay != null && !mCallerIdToDisplay.isEmpty()) {
            nameTextView.setText(mCallerIdToDisplay);
            numberTextView.setText(mPhoneNumberToDisplay);
        } else {
            nameTextView.setText(mPhoneNumberToDisplay);
            numberTextView.setText("");
        }
    }

    private String getCallerInfo() {
        if (mCallerIdToDisplay != null && !mCallerIdToDisplay.isEmpty()) {
            return mCallerIdToDisplay;
        }
        return mPhoneNumberToDisplay;
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
                swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);
            }

        } else if (type.equals(TYPE_INCOMING_CALL)) {
            // Hide the connected view.

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            // Depended on if the user has the screen locked show the slide to answer view or
            // the two buttons to accept / decline a call.
            if (keyguardManager.inKeyguardRestrictedInputMode()) {
                swapFragment(TAG_CALL_LOCK_RING_FRAGMENT, null);

                findViewById(R.id.call_buttons_container).setVisibility(View.INVISIBLE);
            } else {
                swapFragment(TAG_CALL_INCOMING_FRAGMENT, null);
            }
        }
    }

    /**
     * Get the initial state of the connected bluetooth device.
     */
    private void setInitialBluetoothStatus() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            mBluetoothEnabled = bluetoothAdapter.isEnabled();
            if (mBluetoothEnabled) {
                mBluetoothAudio = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED;
                if (mBluetoothAudio) {
                    mBluetoothDeviceConnected = true;
                    toggleBluetoothButtonVisibility(true);
                }
            }
        }
        toggleAudioOutput();
    }

    /**
     * The call has transitioned to another state. We can now visually update the view with
     * extra info or perform required actions.
     *
     * @param newStatus the new interaction state to which we should act.
     * @see SipConstants for the possible states.
     */
    private void onCallStatusUpdate(String newStatus) {
        mRemoteLogger.d(TAG + " onCallStatusUpdate: " + newStatus);

        switch (newStatus) {
            case CALL_CONNECTED_MESSAGE:
                toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);
                mStateView.setText(R.string.call_connected);
                mCallStartTime = System.currentTimeMillis();
                mCallHandler.postDelayed(mCallDurationRunnable, 0);
                mConnected = true;
                mHasConnected = true;
                mIncomingCallIsRinging = false;

                mProximityHelper.updateWakeLock();
                mNotificationHelper.updateNotification(getCallerInfo(), this.getString(R.string.callnotification_active_call), NotificationHelper.mCallNotifyId);

                if (mOnTransfer && mSipService.getCurrentCall() != null && mSipService.getFirstCall() != null) {
                    CallTransferFragment callTransferFragment = (CallTransferFragment)
                            getFragmentManager().findFragmentByTag(TAG_CALL_TRANSFER_FRAGMENT);
                    callTransferFragment.secondCallIsConnected();
                }
                mCallDurationView.setVisibility(View.VISIBLE);
                break;

            case CALL_DISCONNECTED_MESSAGE:
                if (!mHasConnected && !mSelfHangup) {
                    // Call has never been connected. Meaning the dialed number was unreachable.
                    mStateView.setText(R.string.call_unreachable);
                } else {
                    // Normal hangup.
                    mStateView.setText(R.string.call_ended);
                }
                mAudioManager.setSpeakerphoneOn(false);

                // Stop duration timer.
                mCallHandler.removeCallbacks(mCallDurationRunnable);
                mConnected = false;
                mIncomingCallIsRinging = false;

                // Stop the ringtone and vibrator when the call has been disconnected.
                playRingtone(false);
                vibrate(false);

                // When the user is transferring a call.
                if (mOnTransfer) {
                    // Transferring is successful done.
                    if (mCallIsTransferred) {
                        toggleVisibilityCallInfo(false);

                        Map<String, String> map = new HashMap<>();
                        map.put(MAP_ORIGINAL_CALLER_ID, mSipService.getFirstCall().getCallerId());
                        map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipService.getFirstCall().getPhoneNumber());
                        map.put(MAP_TRANSFERRED_PHONE_NUMBER, mTransferredNumber);

                        swapFragment(TAG_CALL_TRANSFER_COMPLETE_FRAGMENT, map);

                        mOnTransfer = false;
                        try {
                            mSipService.getFirstCall().hangup(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (mSipService.getCurrentCall() != null) {
                        // The "second" call has been disconnected. But there is still a call available.
                        String currentCallIdentifier = mSipService.getCurrentCall().getIdentifier();
                        String initialCallIdentifier = mSipService.getFirstCall().getIdentifier();

                        mConnected = true;
                        toggleVisibilityCallInfo(true);

                        findViewById(R.id.button_transfer).setVisibility(View.VISIBLE);
                        swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);

                        newStatus = mSipService.getCurrentCall().getCurrentCallState();

                        if (!currentCallIdentifier.equals(initialCallIdentifier)) {
                            mCurrentCallId = mSipService.getCurrentCall().getIdentifier();
                            mCallerIdToDisplay = mSipService.getCurrentCall().getCallerId();
                            mPhoneNumberToDisplay = mSipService.getCurrentCall().getPhoneNumber();
                        } else {
                            mCurrentCallId = mSipService.getFirstCall().getIdentifier();
                            newStatus = mSipService.getFirstCall().getCurrentCallState();
                            mCallerIdToDisplay = mSipService.getFirstCall().getCallerId();
                            mPhoneNumberToDisplay = mSipService.getFirstCall().getPhoneNumber();
                        }

                        displayCallInfo();

                        mOnHold = mSipService.getCurrentCall().isOnHold();
                        mOnTransfer = false;
                        updateCallButton(R.id.button_transfer, true);
                        updateCallButton(R.id.button_onhold, true);
                        updateCallButton(R.id.button_keypad, true);
                        updateCallButton(R.id.button_microphone, true);

                        onCallStatusUpdate(mSipService.getCurrentCall().getCurrentCallState());
                    } else {
                        if (mSipService != null && mSipService.getCurrentCall() == null && mSipService.getFirstCall() == null) {
                            toggleVisibilityCallInfo(true);
                            swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);
                            displayCallInfo();

                            mOnTransfer = false;
                            mOnHold = false;

                            updateCallButton(R.id.button_transfer, false);
                            updateCallButton(R.id.button_onhold, false);
                            updateCallButton(R.id.button_keypad, false);
                            updateCallButton(R.id.button_microphone, false);
                            updateCallButton(R.id.button_hangup, false);

                            onCallStatusUpdate(CALL_DISCONNECTED_MESSAGE);
                        }
                    }
                } else {
                    finishWithDelay();
                }

                onCallStatesUpdateButtons(newStatus);
                break;

            case CALL_PUT_ON_HOLD_ACTION:
                // Remove a running timer which shows call duration.
                mCallHandler.removeCallbacks(mCallDurationRunnable);
                mCallDurationView.setVisibility(View.GONE);
                mStateView.setText(R.string.call_on_hold);
                mNotificationHelper.updateNotification(getCallerInfo(), this.getString(R.string.callnotification_on_hold), NotificationHelper.mCallNotifyId);
                break;

            case CALL_UNHOLD_ACTION:
                // Start the running timer which shows call duration.
                mCallHandler.postDelayed(mCallDurationRunnable, 0);
                mCallDurationView.setVisibility(View.VISIBLE);
                mStateView.setText(R.string.call_connected);
                mNotificationHelper.updateNotification(getCallerInfo(), this.getString(R.string.callnotification_active_call), NotificationHelper.mCallNotifyId);
                break;

            case CALL_RINGING_OUT_MESSAGE:
                mStateView.setText(R.string.call_outgoing);
                findViewById(R.id.button_speaker).setEnabled(false);
                findViewById(R.id.button_microphone).setEnabled(false);
                break;

            case CALL_RINGING_IN_MESSAGE:
                mStateView.setText(R.string.call_incoming);
                break;

            case SERVICE_STOPPED:
                // TODO: This broadcast is not received anymore due to refactor! Solve with transition to fragments.
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
        View declineButton = findViewById(R.id.button_decline);
        View lockRingView = findViewById(R.id.lock_ring);

        int count = getFragmentManager().getBackStackEntryCount();

        if (count > 0 && mCallIsTransferred) {
            // When transfer is done.
            getFragmentManager().popBackStack();
            finishWithDelay();
        } else if (mOnTransfer) {
            // During a transfer.
            String currentCallIdentifier = mSipService.getCurrentCall().getIdentifier();
            String firstCallIdentifier = mSipService.getFirstCall().getIdentifier();

            if (!firstCallIdentifier.equals(currentCallIdentifier)) {
                try {
                    mSipService.getCurrentCall().hangup(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                mOnTransfer = false;
                toggleVisibilityCallInfo(true);
                swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);

                updateCallButton(R.id.button_onhold, true);
                updateCallButton(R.id.button_transfer, true);
            }
        } else if (hangupButton != null && hangupButton.getVisibility() == View.VISIBLE && mSipService.getCurrentCall() != null) {
            // In call dialog.
            hangup(R.id.button_hangup);
        } else if (declineButton != null && declineButton.getVisibility() == View.VISIBLE && mSipService.getCurrentCall() != null) {
            // Two button pickup visible.
            decline();
        } else if (lockRingView != null && lockRingView.getVisibility() == View.VISIBLE && mSipService.getCurrentCall() != null) {
            // Lock ring visible.
            decline();
        } else {
            super.onBackPressed();
        }
    }

    // Toggle the call on speaker when the user presses the button.
    private void toggleSpeaker() {
        mRemoteLogger.d(TAG + " toggleSpeaker");
        mOnSpeaker = !mOnSpeaker;
        if (!mBluetoothAudio) {
            mAudioManager.setSpeakerphoneOn(mOnSpeaker);
        }
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

        if (mKeyPadVisible) {
            swapFragment(TAG_CALL_KEY_PAD_FRAGMENT, null);
        } else {
            if (mOnTransfer) {
                Map<String, String> map = new HashMap<>();
                map.put(MAP_ORIGINAL_CALLER_ID, mSipService.getFirstCall().getCallerId());
                map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipService.getFirstCall().getPhoneNumber());
                map.put(MAP_SECOND_CALL_IS_CONNECTED, "" + true);

                swapFragment(TAG_CALL_TRANSFER_FRAGMENT, map);
            } else {
                swapFragment(TAG_CALL_CONNECTED_FRAGMENT, null);
            }
        }

        mProximityHelper.updateWakeLock();
    }

    // Toggle the hold the call when the user presses the button.
    private void toggleOnHold() {
        mRemoteLogger.d(TAG + " toggleOnHold");
        mOnHold = !mOnHold;
        if (mServiceBound) {
            try {
                if (mOnTransfer) {
                    mSipService.getCurrentCall().toggleHold();
                } else {
                    mSipService.getFirstCall().toggleHold();
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        View microphoneButton;
        View keypadButton;
        View onHoldButton;
        View bluetoothButton;
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
            case R.id.button_transfer:
                transferButton = findViewById(viewId);
                transferButton.setActivated(mOnTransfer);
                transferButton.setAlpha(
                        buttonEnabled ? mOnTransfer ? 1.0f : 0.5f : 1.0f
                );
                break;
            case R.id.button_bluetooth:
                bluetoothButton = findViewById(viewId);
                bluetoothButton.setActivated(mBluetoothAudio);
                bluetoothButton.setVisibility(mBluetoothDeviceConnected ? View.VISIBLE : View.GONE);
                bluetoothButton.setAlpha(
                        buttonEnabled ? mBluetoothAudio ? 1.0f : 0.5f : 0.5f
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
        if (mServiceBound) {
            updateCallButton(viewId, false);
            try {
                mSipService.getCurrentCall().hangup(true);
                mSelfHangup = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            mStateView.setText(R.string.call_hangup);
            finishWithDelay();
        }
    }

    private void onCallStatesUpdateButtons(String callState) {
        Integer speakerButtonId = R.id.button_speaker;
        Integer microphoneButtonId = R.id.button_microphone;
        Integer keypadButtonId = R.id.button_keypad;
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
        mRemoteLogger.d(TAG + " updateMicrophoneVolume");
        if (mServiceBound) {
            mSipService.getCurrentCall().updateMicrophoneVolume(newVolume);
        }
    }

    @Override
    public void onClick(View view) {
        Integer viewId = view.getId();

        switch (viewId) {
            case R.id.button_speaker:
                toggleSpeaker();
                updateCallButton(viewId, true);
                toggleAudioOutput();
                break;

            case R.id.button_microphone:
                if (mOnTransfer) {
                    if (mSipService.getCurrentCall().getIsCallConnected()) {
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

            case R.id.button_keypad:
                if (mOnTransfer) {
                    if (mSipService.getCurrentCall().getIsCallConnected()) {
                        toggleDialPad();
                        updateCallButton(viewId, true);
                    }
                } else {
                    if (mConnected) {
                        toggleDialPad();
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

                toggleVisibilityCallInfo(false);

                Map<String, String> map = new HashMap<>();
                map.put(MAP_ORIGINAL_CALLER_ID, mSipService.getFirstCall().getCallerId());
                map.put(MAP_ORIGINAL_CALLER_PHONE_NUMBER, mSipService.getFirstCall().getPhoneNumber());
                map.put(MAP_SECOND_CALL_IS_CONNECTED, "" + false);

                swapFragment(TAG_CALL_TRANSFER_FRAGMENT, map);
                mCallDurationView.setVisibility(View.GONE);
                updateCallButton(viewId, true);

                break;

            case R.id.button_onhold:
                if (mOnTransfer) {
                    if (mSipService.getCurrentCall().getIsCallConnected()) {
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

            case R.id.button_bluetooth:
                mBluetoothAudio = !mBluetoothAudio;
                toggleAudioOutput();
                updateCallButton(viewId, true);
                break;
        }
    }

    private void swapFragment(String tag, Map extraInfo) {
        Fragment newFragment = null;

        switch (tag) {
            case TAG_CALL_INCOMING_FRAGMENT:
                newFragment = new CallIncomingFragment();
                break;

            case TAG_CALL_KEY_PAD_FRAGMENT:
                newFragment = new CallKeyPadFragment();
                break;

            case TAG_CALL_LOCK_RING_FRAGMENT:
                newFragment = new CallLockRingFragment();
                break;

            case TAG_CALL_TRANSFER_FRAGMENT:
                String originalCallerId;
                if (extraInfo.get(MAP_ORIGINAL_CALLER_ID) != null) {
                    originalCallerId = extraInfo.get(MAP_ORIGINAL_CALLER_ID).toString();
                } else {
                    originalCallerId = "";
                }

                Bundle callTransferFragmentBundle = new Bundle();
                callTransferFragmentBundle.putString(CALL_TRANSFER_FRAGMENT_ORIGINAL_CALLER_ID, originalCallerId);
                callTransferFragmentBundle.putString(CALL_TRANSFER_FRAGMENT_ORIGINAL_CALLER_PHONE_NUMBER, extraInfo.get(MAP_ORIGINAL_CALLER_PHONE_NUMBER).toString());
                callTransferFragmentBundle.putString(CALL_TRANSFER_FRAGMENT_SECOND_CALL_IS_CONNECTED, extraInfo.get(MAP_SECOND_CALL_IS_CONNECTED).toString());

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
                callTransferCompleteFragment.putString(CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_ID, originalCallerId);
                callTransferCompleteFragment.putString(CALL_TRANSFER_COMPLETE_FRAGMENT_ORIGINAL_CALLER_PHONE_NUMBER, extraInfo.get(MAP_ORIGINAL_CALLER_PHONE_NUMBER).toString());
                callTransferCompleteFragment.putString(CALL_TRANSFER_COMPLETE_FRAGMENT_TRANSFERRED_PHONE_NUMBER, extraInfo.get(MAP_TRANSFERRED_PHONE_NUMBER).toString());

                newFragment = new CallTransferCompleteFragment();
                newFragment.setArguments(callTransferCompleteFragment);
                break;

            case TAG_CALL_CONNECTED_FRAGMENT:
                newFragment = new CallConnectedFragment();
                break;
        }

        if (newFragment != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.addToBackStack(null);

            transaction.replace(R.id.fragment_container, newFragment, tag).commitAllowingStateLoss();
        }
    }

    public void answer() {
        mRemoteLogger.d(TAG + " answer");
        playRingtone(false);
        vibrate(false);

        View callButtonsContainer = findViewById(R.id.call_buttons_container);
        if (callButtonsContainer.getVisibility() == View.INVISIBLE) {
            callButtonsContainer.setVisibility(View.VISIBLE);
        }

        if (MicrophonePermission.hasPermission(this)) {
            if (mServiceBound) {
                try {
                    mSipService.getCurrentCall().answer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mAnalyticsHelper.sendEvent(
                        getString(R.string.analytics_event_category_call),
                        getString(R.string.analytics_event_action_inbound),
                        getString(R.string.analytics_event_label_accepted)
                );

                mNotificationHelper.removeAllNotifications();
                mNotificationId = mNotificationHelper.displayCallProgressNotification(
                        getCallerInfo(), getString(R.string.callnotification_active_call), TYPE_CONNECTED_CALL,
                        mCallerIdToDisplay, mPhoneNumberToDisplay, NotificationHelper.mCallNotifyId
                );
                mIsIncomingCall = false;
            }
        } else {
            Toast.makeText(CallActivity.this,
                    getString(R.string.permission_microphone_missing_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void decline() {
        mRemoteLogger.d(TAG + " decline");
        playRingtone(false);
        vibrate(false);
        if (mServiceBound) {
            try {
                mSipService.getCurrentCall().decline();
                mSelfHangup = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            mAnalyticsHelper.sendEvent(
                    getString(R.string.analytics_event_category_call),
                    getString(R.string.analytics_event_action_inbound),
                    getString(R.string.analytics_event_label_declined)
            );

            finishWithDelay();
        }
    }

    private void playRingtone(boolean play) {
        if (mMediaPlayer != null) {
            if (play && !mMediaPlayer.isPlaying()) {
                setVolumeControlStream(AudioManager.STREAM_RING);
                mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                // Play the ringtone!
                mMediaPlayer.start();
            } else {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            }
        }
    }

    private void vibrate(boolean vibrate) {
        if (mVibrator != null) {
            if (vibrate) {
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
                mAudioManager.setSpeakerphoneOn(mOnSpeaker);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // TODO VIALA-463: Handle the complete loss of audio. Eg: incoming GSM call.
                mRemoteLogger.i(TAG + " Audiofocus loss");
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // TODO VIALA-463: Handle temporary loss of audio. Eg: incoming GSM call.
                mRemoteLogger.i(TAG + " Audiofocus loss transient");
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

    /**
     * Function to toggle the audio output to bluetooth or it's original state.
     */
    private void toggleAudioOutput() {
        // Send audio stream to BT device.
        if (mBluetoothAudio && mBluetoothEnabled) {
            toggleBluetoothButtonVisibility(true);
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            mAudioManager.setBluetoothScoOn(true);
            mAudioManager.startBluetoothSco();
            mAudioManager.setSpeakerphoneOn(false);
            updateCallButton(R.id.button_bluetooth, true);
        } else {
            // If device is not connected hide the BT button.
            if (!mBluetoothDeviceConnected) {
                toggleBluetoothButtonVisibility(false);
            }
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
            mAudioManager.setSpeakerphoneOn(mOnSpeaker);
            updateCallButton(R.id.button_bluetooth, false);
        }
    }

    /**
     * Function to toggle the visibility of the bluetooth button.
     *
     * @param visible
     */
    private void toggleBluetoothButtonVisibility(boolean visible) {
        View bluetoothButton = findViewById(R.id.button_bluetooth);

        bluetoothButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void callKeyPadButtonClicked(String dtmf) {
        if (mServiceBound) {
            SipCall call = mSipService.getCurrentCall();
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
        mSipService.makeCall(sipAddressUri, "", numberToCall);

        toggleVisibilityCallInfo(true);
        findViewById(R.id.button_transfer).setVisibility(View.GONE);

        mOnHold = mSipService.getCurrentCall().isOnHold();
        updateCallButton(R.id.button_onhold, false);
        updateCallButton(R.id.button_keypad, false);
        updateCallButton(R.id.button_microphone, false);

        mCurrentCallId = mSipService.getCurrentCall().getIdentifier();

        mStateView.setText(R.string.title_state_calling);

        mCallerIdToDisplay = mSipService.getCurrentCall().getCallerId();
        mPhoneNumberToDisplay = mSipService.getCurrentCall().getPhoneNumber();

        displayCallInfo();
    }

    @Override
    public void callTransferHangupSecondCall() {
        try {
            if (mSipService.getFirstCall().isOnHold()) {
                mSipService.getCurrentCall().hangup(true);
            } else {
                mSipService.getFirstCall().hangup(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void hangupFromKeypad() {
        try {
            mSipService.getCurrentCall().hangup(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void callTransferConnectTheCalls() {
        try {
            mTransferredNumber = mSipService.getCurrentCall().getPhoneNumber();
            mSipService.getFirstCall().xFerReplaces(mSipService.getCurrentCall());
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

    private void toggleVisibilityCallInfo(Boolean visible) {
        findViewById(R.id.call_info).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.call_buttons_container).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setRingerMode() {
        switch (mAudioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                playRingtone(true);
                boolean vibrateWhenRinging = Settings.System.getInt(getContentResolver(), "vibrate_when_ringing ", 0) != 0;
                if (vibrateWhenRinging) {
                    vibrate(true);
                }
                break;

            case AudioManager.RINGER_MODE_VIBRATE:
                vibrate(true);
                break;

            case AudioManager.RINGER_MODE_SILENT:
                playRingtone(false);
                vibrate(false);
                break;
        }
    }
}
