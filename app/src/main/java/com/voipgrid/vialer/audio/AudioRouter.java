package com.voipgrid.vialer.audio;

import static com.voipgrid.vialer.audio.Constants.ROUTE_BT;
import static com.voipgrid.vialer.audio.Constants.ROUTE_EARPIECE;
import static com.voipgrid.vialer.audio.Constants.ROUTE_HEADSET;
import static com.voipgrid.vialer.audio.Constants.ROUTE_SPEAKER;
import static com.voipgrid.vialer.audio.Constants.STATE_WIRED_HS_INVALID;
import static com.voipgrid.vialer.audio.Constants.STATE_WIRED_HS_PLUGGED;
import static com.voipgrid.vialer.audio.Constants.STATE_WIRED_HS_UNPLUGGED;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.voipgrid.vialer.bluetooth.BluetoothMediaSessionService;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BroadcastReceiverManager;

/**
 * Responsible for setting up and managing the routing of audio for VoIP.
 */
public class AudioRouter {

    private final Bluetooth bluetooth;
    private final Context context;
    private final BroadcastReceiverManager broadcastReceiverManager;
    private final Logger logger;
    private final AudioManager audioManager;

    // Stores the audio states for a wired headset
    private int mWiredHsState = STATE_WIRED_HS_UNPLUGGED;

    private BluetoothDevice connectedBluetoothHeadset;

    /**
     * This boolean is set to TRUE when the audio is being routed around
     * bluetooth despite bluetooth being available, this will be when the
     * user selects a different audio source. This is so we can properly handle
     * a single button input from the bluetooth headset without ending a call.
     *
     */
    private boolean bluetoothManuallyDisabled = false;

    private WiredHeadsetBroadcastReceiver mWiredHeadsetReceiver = new WiredHeadsetBroadcastReceiver();
    private BluetoothHeadsetBroadcastReceiver bluetoothHeadsetReceiver = new BluetoothHeadsetBroadcastReceiver();
    private AudioFocusHandler audioFocusHandler = new AudioFocusHandler();

    public AudioRouter(Context context, AudioManager audioManager, BroadcastReceiverManager broadcastReceiverManager) {
        this.logger = new Logger(AudioRouter.class);
        this.context = context;
        this.broadcastReceiverManager = broadcastReceiverManager;
        this.audioManager = audioManager;
        this.bluetooth = new Bluetooth(audioManager);

        broadcastReceiverManager.unregisterReceiver(mWiredHeadsetReceiver, bluetoothHeadsetReceiver);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mWiredHeadsetReceiver, Intent.ACTION_HEADSET_PLUG);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(bluetoothHeadsetReceiver, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        BluetoothMediaSessionService.start(context);
        initializeAndroidAudioManager();

        if (bluetooth.isBluetoothCommunicationDevicePresent()) {
            routeAudioViaBluetooth();
        }
    }

    /**
     * Destroy the audio router, tearing down all listeners and resetting services back to their
     * defaults. Should always be called when the audio router is used.
     *
     */
    public void destroy() {
        logger.v("Destroying the audio router");
        bluetooth.stop();
        resetAndroidAudioManager();
        broadcastReceiverManager.unregisterReceiver(mWiredHeadsetReceiver, bluetoothHeadsetReceiver);
        context.stopService(new Intent(context, BluetoothMediaSessionService.class));
    }

    public void routeAudioViaBluetooth() {
        bluetoothManuallyDisabled = false;

        if (bluetooth.isOn()) return;

        bluetooth.start();
    }

    public void routeAudioViaSpeaker() {
        bluetoothManuallyDisabled = true;
        logger.d("enableSpeaker()");

        if (getCurrentRoute() == ROUTE_BT) {
            bluetooth.stop();
        }

        audioManager.setSpeakerphoneOn(true);
    }

    public void routeAudioViaEarpiece() {
        bluetoothManuallyDisabled = true;
        logger.v("enableEarpiece()");

        if (!hasEarpiece()) {
            logger.v("===> no earpiece");
            return;
        }

        if (getCurrentRoute() == Constants.ROUTE_HEADSET) {
            logger.v("===> ROUTE_HEADSET");
            return;
        }

        if (getCurrentRoute() == ROUTE_BT) {
            logger.v("===> ROUTE_BT");
            bluetooth.stop();
        }

        audioManager.setSpeakerphoneOn(false);
    }

    /**
     * Check if we are currently routing audio through a bluetooth device.
     *
     * @return TRUE if audio is being routed over bluetooth, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaBluetooth() {
        return getCurrentRoute() == ROUTE_BT;
    }

    public boolean isCurrentlyRoutingAudioViaSpeaker() {
        return getCurrentRoute() == ROUTE_SPEAKER;
    }

    public boolean isCurrentlyRoutingAudioViaEarpiece() {
        return getCurrentRoute() == ROUTE_EARPIECE;
    }

    public boolean isCurrentlyRoutingAudioViaWiredHeadset() {
        return getCurrentRoute() == ROUTE_HEADSET;
    }

    public boolean isBluetoothRouteAvailable() {
        return bluetooth.isBluetoothCommunicationDevicePresent();
    }

    /**
     * Determine where the audio is currently being routed.
     *
     * @return
     */
    private int getCurrentRoute() {
        logger.i("getCurrentRoute()");

        if (bluetooth.isOn()) {
            logger.i("==> ROUTE_BT");
            return ROUTE_BT;
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

    public BluetoothDevice getConnectedBluetoothHeadset() {
        return connectedBluetoothHeadset;
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
     * Initialize the audio manager, setting the correct stream and listener.
     *
     */
    private void initializeAndroidAudioManager() {
        audioManager.requestAudioFocus(
                audioFocusHandler,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
        );
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    /**
     * Resets the audio manager, setting back to default and stopping listening for events.
     *
     */
    private void resetAndroidAudioManager() {
        if(audioManager == null) return;

        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.abandonAudioFocus(audioFocusHandler);
    }

    private class BluetoothHeadsetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (bluetoothDevice != null) {
                connectedBluetoothHeadset = bluetoothDevice;
            }

            if (!bluetooth.isOn() && !bluetoothManuallyDisabled) {
                bluetooth.start();
                SipService.performActionOnSipService(context, SipService.Actions.SMART_SINGLE_BUTTON_ACTION);
            }
        }
    }

    private class AudioFocusHandler implements AudioManager.OnAudioFocusChangeListener {

        private int previousVolume = -1;

        private boolean audioWasLost = false;

        @Override
        public void onAudioFocusChange(final int focusChange) {
            logger.v("onAudioFocusChange()...");
            logger.v("====> " + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    logger.i("We gained audio focus!");
                    if (previousVolume != -1) {
                        audioManager.setStreamVolume(
                                AudioManager.STREAM_VOICE_CALL, previousVolume, 0);
                        previousVolume = -1;
                    }

                    logger.i("Was the audio lost: " + audioWasLost);
                    if (audioWasLost) {
                        audioWasLost = false;
                        routeAudioViaBluetooth();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    logger.i("Lost audio focus! Probably incoming native audio call.");
                    audioWasLost = true;
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    logger.i(
                            "We must lower our audio volume! Probably incoming notification / "
                                    + "driving directions.");
                    previousVolume = audioManager.getStreamVolume(
                            AudioManager.STREAM_VOICE_CALL);
                    audioManager.setStreamVolume(
                            AudioManager.STREAM_VOICE_CALL, 1,
                            0);
                    break;
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
