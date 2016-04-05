package com.voipgrid.vialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.sip.SipConstants;

/**
 * CallActivity for incoming or outgoing call
 */
public class CallActivity extends AppCompatActivity
        implements View.OnClickListener, SensorEventListener, SipConstants {

    public static final String TYPE_OUTGOING_CALL = "type-outgoing-call";
    public static final String TYPE_INCOMING_CALL = "type-incoming-call";
    public static final String TYPE_CONNECTED_CALL = "type-connected-call";
    public static final String CONTACT_NAME = "contact-name";
    public static final String PHONE_NUMBER = "phone-number";
    private static final long[] VIBRATOR_PATTERN = {1000L, 1000L};
    // Manager for "on speaker" action.
    private AudioManager mAudioManager;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;

    private TextView mCallDurationView;
    private TextView mStateView;

    // Keep track of the start time of a call, so we can keep track of its duration.
    long mCallStartTime = 0;

    // runs without a timer by reposting this handler at the end of the runnable
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

    // Broadcast manager to notify all call status listeners and listen for call interactions
    private LocalBroadcastManager mBroadcastManager;
    // Broadcast receiver for presenting changes in call state to user.
    private BroadcastReceiver mCallStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onCallStatusUpdate(intent.getStringExtra(CALL_STATUS_KEY));
        }
    };

    private boolean mIsIncomingCall;

    private boolean mConnected;

    private boolean mMute = false;

    private ViewGroup mKeyPadViewContainer;
    private AnalyticsHelper mAnalyticsHelper;

    private Ringtone mRingtone;

    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_call);

        /* set the AnalyticsHelper */
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );

        mKeyPadViewContainer = (ViewGroup) findViewById(R.id.key_pad_container);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        // Manager that handles the 'put on speaker' feature
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setSpeakerphoneOn(false); // Set default speaker usage to NO/Off.

        // Make sure the hardware volume buttons control the volume of the call.
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mSensorManager   = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mRingtone = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_RINGTONE_URI);

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // fetch a broadcast manager for communication to the sip service
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        mStateView = (TextView) findViewById(R.id.state_text_view);
        mCallDurationView = (TextView) findViewById(R.id.duration_text_view);

        mConnected = false;

        Intent intent = getIntent();

        /**
         * We are being asked to present account data from a contact entry,
         * but heck: Let's call the contact :D
         */
        String type = intent.getType();
        if (type.equals(TYPE_INCOMING_CALL) ||
                type.equals(TYPE_OUTGOING_CALL)) {

            // Update the textview with a number URI
            String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
            String contactName = intent.getStringExtra(CONTACT_NAME);
            displayCallInfo(phoneNumber, contactName);

            // Start the SipService which manages all SIP traffic.
            mIsIncomingCall = type.equals(TYPE_INCOMING_CALL);

            toggleCallStateButtonVisibility(type);

            if(mIsIncomingCall) {

                switch (mAudioManager.getRingerMode()) {
                    case AudioManager.RINGER_MODE_NORMAL : playRingtone(true); break;
                    case AudioManager.RINGER_MODE_VIBRATE : vibrate(true); break;
                    case AudioManager.RINGER_MODE_SILENT :
                        playRingtone(false);
                        vibrate(false);
                        break;
                }
            }

        }
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
     * Based on typeshow/hide and position the buttons to answer/hangup a call.
     * @param type a string containing a call type (INCOMING or OUTGOING)
     */
    private void toggleCallStateButtonVisibility(String type) {
        if (type.equals(TYPE_OUTGOING_CALL) || type.equals(TYPE_CONNECTED_CALL)) {
            // Hide answer, decline = hangup
            findViewById(R.id.ringing).setVisibility(View.GONE);
            findViewById(R.id.connected).setVisibility(View.VISIBLE);
        } else if (type.equals(TYPE_INCOMING_CALL)) {
            // Answer and hangup
            findViewById(R.id.connected).setVisibility(View.GONE);
            findViewById(R.id.ringing).setVisibility(View.VISIBLE);
        }
    }

    /**
     * The call has transitioned to another state. We can now visually update the view with
     * extra info or perform required actions.
     * @param newStatus the new interaction state to which we should act.
     * @see SipConstants for the possible states.
     */
    private void onCallStatusUpdate(String newStatus) {
        switch (newStatus) {
            case CALL_MEDIA_AVAILABLE_MESSAGE:
                onCallStatusUpdate(CALL_STOP_RINGBACK_MESSAGE);
                break;
            case CALL_MEDIA_UNAVAILABLE_MESSAGE:
                /* NOTYETIMPLEMENTED */
                break;
            case CALL_CONNECTED_MESSAGE:
                toggleCallStateButtonVisibility(TYPE_CONNECTED_CALL);
                mStateView.setText(R.string.call_connected);

                mCallStartTime = System.currentTimeMillis();
                mCallHandler.postDelayed(mCallDurationRunnable, 0);

                mConnected = true;

                playRingtone(false);
                vibrate(false);

                break;
            case CALL_DISCONNECTED_MESSAGE:
                // We got a DISCONNECT. Probably save to stop Ringback since it's over
                onCallStatusUpdate(CALL_STOP_RINGBACK_MESSAGE);

                mAudioManager.setSpeakerphoneOn(false);
                mStateView.setText(R.string.call_ended);

                // Stop duration timer
                mCallHandler.removeCallbacks(mCallDurationRunnable);

                playRingtone(false);
                vibrate(false);

                finishWithDelay();

                break;
            case CALL_PUT_ON_HOLD_ACTION:
                // Remove a running timer which shows call duration
                mCallHandler.removeCallbacks(mCallDurationRunnable);
                mCallDurationView.setVisibility(View.INVISIBLE);

                mStateView.setText(R.string.call_on_hold);
                break;
            case CALL_UNHOLD_ACTION:

                // Start the running timer which shows call duration
                mCallHandler.postDelayed(mCallDurationRunnable, 0);
                mCallDurationView.setVisibility(View.VISIBLE);

                mStateView.setText(R.string.call_connected);
                break;
            case CALL_RINGING_OUT_MESSAGE:
                mStateView.setText(R.string.call_outgoing);
                break;
            case CALL_RINGING_IN_MESSAGE:
                mStateView.setText(R.string.call_incoming);
                break;
            case SERVICE_STOPPED :

                finishWithDelay();

                break;
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onStart() {
        super.onStart();
        /* Register for updates */
        IntentFilter intentFilter = new IntentFilter(ACTION_BROADCAST_CALL_STATUS);
        mBroadcastManager.registerReceiver(mCallStatusReceiver, intentFilter);
        if (mProximitySensor != null) {
            mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void onStop() {
        super.onStop();

        // unRegister the SipService BroadcastReceiver when the activity pauses
        mBroadcastManager.unregisterReceiver(mCallStatusReceiver);
        if (mProximitySensor != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onBackPressed() {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_call, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem speakerItem = menu.findItem(R.id.speaker);
        MenuItem microphoneItem = menu.findItem(R.id.microphone);
        if(mIsIncomingCall && !mConnected) {
            speakerItem.setEnabled(false);
            microphoneItem.setEnabled(false);
        } else {
            speakerItem.setEnabled(true);
            microphoneItem.setEnabled(true);
        }

        // Toggle speaker item
        if(mAudioManager.isSpeakerphoneOn()) {
            speakerItem.setIcon(speakerItem.isEnabled() ?
                    R.drawable.ic_volume_off_enabled : R.drawable.ic_volume_off_disabled);
        } else {
            speakerItem.setIcon(speakerItem.isEnabled() ?
                    R.drawable.ic_volume_on_enabled : R.drawable.ic_volume_on_disabled);
        }

        // Toggle microphone item
        if(mMute) {
            microphoneItem.setIcon(microphoneItem.isEnabled() ?
                    R.drawable.ic_mic_on_enabled : R.drawable.ic_mic_on_disabled);
        } else {
            microphoneItem.setIcon(microphoneItem.isEnabled() ?
                    R.drawable.ic_mic_off_enabled: R.drawable.ic_mic_off_disabled);
        }

        // Set dialpad item
        MenuItem dialpadItem = menu.findItem(R.id.dialpad);
        dialpadItem.setIcon(dialpadItem.isEnabled() ?
                R.drawable.ic_dialer_enabled : R.drawable.ic_dialer_disabled);

        // Set onhold item
        MenuItem onhold = menu.findItem(R.id.onhold);
        onhold.setIcon(onhold.isEnabled() ?
                R.drawable.ic_pause_enabled : R.drawable.ic_pause_disabled);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.speaker : toggleSpeaker(); break;
            case R.id.microphone : toggleMicrophone(); break;
            case R.id.dialpad : toggleDialpad(); break;
            case R.id.onhold: onPutHold(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSpeaker() {
        mAudioManager.setSpeakerphoneOn(!mAudioManager.isSpeakerphoneOn());
        invalidateOptionsMenu();
    }

    private void toggleMicrophone() {
        mMute = !mMute;
        long newVolume = (mMute ?               // get new volume based on selection value
                    getResources().getInteger(R.integer.mute_microphone_volume_value) :   // muted
                    getResources().getInteger(R.integer.unmute_microphone_volume_value)); // unmuted
        updateMicrophoneVolume(newVolume);
        invalidateOptionsMenu();
    }

    private void toggleDialpad() {
        boolean visible = mKeyPadViewContainer.getVisibility() == View.VISIBLE;
        mKeyPadViewContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void onPutHold() {
        Bundle onHoldExtras = new Bundle();
        onHoldExtras.putString(CALL_STATUS_ACTION, CALL_PUT_ON_HOLD_ACTION);
        broadcast(onHoldExtras);
        // put on hold (UI) -> https://jira.peperzaken.nl/browse/VIALA-104
    }

    /**
     * Send a broadcast with Action ACTION_BROADCAST_CALL_STATUS and a STATUS String extra to a set
     * of receivers.
     *
     * @param extras add data Bundle to CAll interaction broadcast intent.'
     */
    private void broadcast(Bundle extras) {
        Intent intent =  new Intent(ACTION_BROADCAST_CALL_INTERACTION);
        intent.putExtras(extras);
        mBroadcastManager.sendBroadcast(intent);
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
        Bundle extras = new Bundle();
        extras.putString(CALL_STATUS_ACTION, CALL_UPDATE_MICROPHONE_VOLUME_ACTION);
        extras.putLong(MICROPHONE_VOLUME_KEY, newVolume);
        broadcast(extras);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_hangup:
                Bundle hangupExtras = new Bundle();
                hangupExtras.putString(CALL_STATUS_ACTION, CALL_HANG_UP_ACTION);
                broadcast(hangupExtras);  // Broadcast to service to decline or end call.
                mStateView.setText(R.string.call_hangup);
                finishWithDelay();
                break;
            case R.id.button_reject:
                Bundle rejectExtras = new Bundle();
                rejectExtras.putString(CALL_STATUS_ACTION, CALL_DECLINE_ACTION);
                broadcast(rejectExtras);  // Broadcast to service to decline or end call.

                mAnalyticsHelper.send(
                        getString(R.string.analytics_dimension),
                        getString(R.string.analytics_event_category_call),
                        getString(R.string.analytics_event_action_inbound),
                        getString(R.string.analytics_event_label_declined)
                );

                finishWithDelay();              // Close this activity.
                break;
            case R.id.button_pickup:
                Bundle pickupExtras = new Bundle();
                pickupExtras.putString(CALL_STATUS_ACTION, CALL_PICK_UP_ACTION);
                broadcast(pickupExtras); // Broadcast to service to accept call.

                mAnalyticsHelper.send(
                        getString(R.string.analytics_dimension),
                        getString(R.string.analytics_event_category_call),
                        getString(R.string.analytics_event_action_inbound),
                        getString(R.string.analytics_event_label_accepted)
                );

                break;
            case R.id.screen_off : break; // Screen is off so ignore click events
        }
    }

    /**
     * Function to set the screen to ON (visible) or OFF (dimmed and disabled).
     * @param on Wether or not the screen needs to be on or off.
     */
    private void toggleScreen(boolean on) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        View view = findViewById(R.id.screen_off);
        if (on) {
            // Reset screen brightness.
            params.screenBrightness = -1;

            // Set the OFF version of the screen to gone.
            view.setVisibility(View.GONE);

            // Remove the listener for the OFF screen state.
            view.setOnClickListener(null);

            // Show statusbar and navigation.
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            // Set screen brightness to 0.
            params.screenBrightness = 0;

            // Set the OFF version of the screen to visible.
            view.setVisibility(View.VISIBLE);

            // Set the listener for the OFF screen stat.
            view.setOnClickListener(this);

            // Hide statusbar and navigation.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
        getWindow().setAttributes(params);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        // Leave the screen on if the measured distance is the max distance.
        if (distance >= event.sensor.getMaximumRange() || distance >= 10.0f) {
            toggleScreen(true);
        } else {
            toggleScreen(false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
                finish();  // Close this activity after 2 seconds.
            }
        }, 3000);
    }

}
