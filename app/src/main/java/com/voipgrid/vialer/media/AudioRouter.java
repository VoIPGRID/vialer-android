package com.voipgrid.vialer.media;

import static com.voipgrid.vialer.media.BluetoothMediaSessionService
        .SHOULD_NOT_START_IN_FOREGROUND_EXTRA;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BroadcastReceiverManager;

/**
 * Class is responsible for in call where the audio will be routed to.
 * Either earpiece / headset / bluetooth devices.
 */
public class AudioRouter {

    private Context context;
    private AudioManager audioManager;
    private BroadcastReceiverManager broadcastReceiverManager;
    private Logger logger;

    // wired HS defines
    private static final int STATE_WIRED_HS_INVALID = -1;
    private static final int STATE_WIRED_HS_UNPLUGGED = 0;
    private static final int STATE_WIRED_HS_PLUGGED = 1;

    // Stores the audio states for a wired headset
    private int mWiredHsState = STATE_WIRED_HS_UNPLUGGED;

    // Broadcast receiver for Bluetooth SCO broadcasts.
    // Utilized to detect if BT SCO streaming is on or off.
    private BroadcastReceiver mBluetoothScoReceiver = new BluetoothScoReceiver();
    private BroadcastReceiver mWiredHeadsetReceiver = new WiredHeadsetBroadcastReceiver();

    public AudioRouter(Context context, BroadcastReceiverManager broadcastReceiverManager) {
        this.logger = new Logger(AudioRouter.class);
        this.context = context;
        this.broadcastReceiverManager = broadcastReceiverManager;

        broadcastReceiverManager.unregisterReceiver(mBluetoothScoReceiver, mWiredHeadsetReceiver);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mBluetoothScoReceiver, AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mWiredHeadsetReceiver, Intent.ACTION_HEADSET_PLUG);

        BluetoothMediaSessionService.start(context);
    }

    public void provideAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    void deInit() {
        logger.v("deInit()");
        stopBluetoothSco();
        broadcastReceiverManager.unregisterReceiver(mBluetoothScoReceiver, mWiredHeadsetReceiver);
        context.stopService(new Intent(context, BluetoothMediaSessionService.class));
    }

    public void routeAudioViaBluetooth() {
        if (audioManager.isBluetoothScoOn()) return;

        audioManager.setBluetoothScoOn(true);
        audioManager.startBluetoothSco();
    }

    public void routeAudioViaSpeaker() {
        logger.d("enableSpeaker()");

        if (getCurrentRoute() == Constants.ROUTE_BT) {
            stopBluetoothSco();
        }

        configureSpeakerPhone(true);
    }

    public void routeAudioViaEarpiece() {
        logger.v("enableEarpiece()");

        if (!hasEarpiece()) {
            logger.v("===> no earpiece");
            return;
        }

        if (getCurrentRoute() == Constants.ROUTE_HEADSET) {
            logger.v("===> ROUTE_HEADSET");
            return;
        }

        if (getCurrentRoute() == Constants.ROUTE_BT) {
            logger.v("===> ROUTE_BT");
            stopBluetoothSco();
        }

        configureSpeakerPhone(false);
    }

    /**
     * Configures the speaker phone, only enabling it when it is
     * disabled to avoid unnecessary errors.
     *
     * @param on
     */
    private void configureSpeakerPhone(boolean on) {
        if (on && !audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(true);
        } else if (!on && audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
        }
    }

    /**
     * Check if there is a communication device currently connected that we could route
     * audio through.
     *
     * @return TRUE if there is a bluetooth communication device present, not necessarily if audio is being routed
     * over bluetooth otherwise FALSE.
     */
    public boolean isBluetoothCommunicationDevicePresent() {
        logger.v("hasBluetoothHeadset()");

        BluetoothAdapter bluetoothAdapter;
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            logger.e("BluetoothAdapter.getDefaultAdapter() exception: " + e.getMessage());
            return false;
        }

        if (bluetoothAdapter == null) {
            logger.e("There is no bluetoothAdapter!?");
            return false;
        }

        int profileConnectionState;
        try {
            profileConnectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        } catch (Exception e) {
            logger.e("BluetoothAdapter.getProfileConnectionState() exception: " + e.getMessage());
            profileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        }
        logger.i("Bluetooth profile connection state: " + profileConnectionState);
        return bluetoothAdapter.isEnabled() && profileConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    /**
     * Check if we are currently routing audio through a bluetooth device.
     *
     * @return TRUE if audio is being routed over bluetooth, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaBluetooth() {
        return audioManager.isBluetoothScoOn();
    }

    /**
     * Determine where the audio is currently being routed.
     *
     * @return
     */
    private int getCurrentRoute() {
        logger.i("getCurrentRoute()");

        if (audioManager.isBluetoothScoOn()) {
            logger.i("==> ROUTE_BT");
            return Constants.ROUTE_BT;
        }

        if (audioManager.isSpeakerphoneOn()) {
            logger.i("==> ROUTE_SPEAKER");
            return Constants.ROUTE_SPEAKER;
        }

        if (mWiredHsState == STATE_WIRED_HS_PLUGGED) {
            logger.i("==> ROUTE_HEADSET");
            return Constants.ROUTE_HEADSET;
        }

        if (hasEarpiece()) {
            logger.i("==> ROUTE_EARPIECE");
            return Constants.ROUTE_EARPIECE;
        }

        return Constants.ROUTE_INVALID;
    }

    void onStartingCall() {
        logger.d("onStartingCall()");
        logger.d("==> Current Call State: " + MediaManager.CURRENT_CALL_STATE);

        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_INVALID) {
            MediaManager.CURRENT_CALL_STATE = Constants.CALL_RINGING;
            if (isBluetoothCommunicationDevicePresent()) {
                routeAudioViaBluetooth();
            }
        }
    }

    void onAnsweredCall() {
        logger.v("onAnsweredCall()");
        MediaManager.CURRENT_CALL_STATE = Constants.CALL_ANSWERED;
    }

    void onOutgoingCall() {
        logger.v("onOutgoingCall()");
        logger.v("==> Current Call State: " + MediaManager.CURRENT_CALL_STATE);
        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_INVALID) {
            MediaManager.CURRENT_CALL_STATE = Constants.CALL_OUTGOING;
            if (isBluetoothCommunicationDevicePresent()) {
                routeAudioViaBluetooth();
            }
        }
    }

    void onEndedCall() {
        logger.v("onEndedCall()");
        MediaManager.CURRENT_CALL_STATE = Constants.CALL_INVALID;
        stopBluetoothSco();
    }

    private void stopBluetoothSco() {
        logger.v("stopBluetoothSco()");

        if (!audioManager.isBluetoothScoOn()) {
            logger.i("==> Unable to stop Bluetooth sco since it is already disabled!");
            return;
        }

        logger.d("==> turning Bluetooth sco off");
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);

        int retries = 0;
        while(audioManager.isBluetoothScoOn() && retries < 10) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);

            logger.i("Retry of stopping bluetooth sco: " + retries);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}

            if (!audioManager.isBluetoothScoOn()) {
                return;
            }

            retries++;
        }
    }

    /**
     * Check if the device actually has an earpiece, this will only be relevant on Android
     * devices that are not phones.
     *
     * @return
     */
    private boolean hasEarpiece() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    /**
     * The BluetoothScoReceiver allows us to intercept button clicks on devices with just a single
     * button, allowing us to answer/decline the call.
     *
     */
    private class BluetoothScoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    Log.d("TEST123", String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
                }
            }
            if (isInitialStickyBroadcast()) return;


            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);

            logger.v("==> onReceive() action: " + intent.getAction()
                    + "\n state: " + state
                    + "\n stickyBroadcast: " + isInitialStickyBroadcast()
                    + "\n isBluetoothScoOn: " + audioManager.isBluetoothScoOn()
            );

            if (isBluetoothCommunicationDevicePresent() && !audioManager.isBluetoothScoOn() && state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                routeAudioViaBluetooth();
                SipService.performActionOnSipService(context, SipService.Actions.SMART_SINGLE_BUTTON_ACTION);
            }
        }
    }

    private class WiredHeadsetBroadcastReceiver extends BroadcastReceiver {
        private static final int STATE_UNPLUGGED = 0;
        private static final int STATE_PLUGGED = 1;
        private static final int HAS_NO_MIC = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            logger.v("onReceive()");
            int state = intent.getIntExtra("state", STATE_UNPLUGGED);
            int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);

            String name = intent.getStringExtra("name");
            logger.v("==> action: " + intent.getAction()
                    + "\n state: " + state
                    + "\n microphone: " + microphone
                    + "\n name: " + name
                    + "\n stickyBroadcast: " + isInitialStickyBroadcast()
            );

            switch (state) {
                case STATE_UNPLUGGED:
                    logger.v("==> Headset unplugged");
                    mWiredHsState = STATE_WIRED_HS_UNPLUGGED;
                    break;
                case STATE_PLUGGED:
                    logger.v("==> Headset plugged");
                    mWiredHsState = STATE_WIRED_HS_PLUGGED;
                    break;
                default:
                    logger.v("==> Headset invalid state");
                    mWiredHsState = STATE_WIRED_HS_INVALID;
                    break;
            }
        }
    }
}
