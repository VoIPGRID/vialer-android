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
import android.os.Handler;
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
    private final AudioFocus audioFocus;

    private BluetoothDevice connectedBluetoothHeadset;

    /**
     * This boolean is set to TRUE when the audio is being routed around
     * bluetooth despite bluetooth being available, this will be when the
     * user selects a different audio source. This is so we can properly handle
     * a single button input from the bluetooth headset without ending a call.
     *
     */
    private boolean bluetoothManuallyDisabled = false;

    private BluetoothHeadsetBroadcastReceiver bluetoothHeadsetReceiver = new BluetoothHeadsetBroadcastReceiver();

    public AudioRouter(Context context, AudioManager audioManager, BroadcastReceiverManager broadcastReceiverManager, AudioFocus audioFocus) {
        this.audioFocus = audioFocus;
        this.logger = new Logger(AudioRouter.class);
        this.context = context;
        this.broadcastReceiverManager = broadcastReceiverManager;
        this.audioManager = audioManager;
        this.bluetooth = new Bluetooth(audioManager);

        broadcastReceiverManager.unregisterReceiver(bluetoothHeadsetReceiver);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(bluetoothHeadsetReceiver, BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);

        BluetoothMediaSessionService.start(context);
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
        broadcastReceiverManager.unregisterReceiver(bluetoothHeadsetReceiver);
        context.stopService(new Intent(context, BluetoothMediaSessionService.class));
    }

    /**
     * Route the audio through the currently connected bluetooth device.
     *
     */
    public void routeAudioViaBluetooth() {
        logAudioRouteRequest("bluetooth");

        bluetoothManuallyDisabled = false;

        if (bluetooth.isOn()) {
            logger.e("Aborting request to route call audio via BLUETOOTH as bluetooth is currently enabled");
            return;
        }

        bluetooth.start();

        logAudioRouteHandled("bluetooth");
    }

    /**
     * Route the audio through the phone's loud speaker.
     *
     */
    public void routeAudioViaSpeaker() {
        logAudioRouteRequest("speaker");

        bluetoothManuallyDisabled = true;

        if (isCurrentlyRoutingAudioViaBluetooth()) {
            logger.i("Stopping bluetooth routing as speaker route was requested");
            bluetooth.stop();
        }

        audioManager.setSpeakerphoneOn(true);

        logAudioRouteHandled("speaker");
    }

    /**
     * Route the audio through the phone's earpiece, this is the standard method for making a call.
     *
     */
    public void routeAudioViaEarpiece() {
        logAudioRouteRequest("earpiece");

        bluetoothManuallyDisabled = true;

        if (!hasEarpiece()) {
            logger.e("Unable to route audio via earpiece as the current device does not have one, is this not a phone?");
            return;
        }

        if (isCurrentlyRoutingAudioViaWiredHeadset() || isCurrentlyRoutingAudioViaEarpiece()) {
            logger.e("Already routing audio via wired headset or earpiece");
            return;
        }

        if (isCurrentlyRoutingAudioViaBluetooth()) {
            logger.i("Stopping bluetooth routing as earpiece route was requested");
            bluetooth.stop();
        }

        audioManager.setSpeakerphoneOn(false);

        logAudioRouteHandled("earpiece");
    }

    /**
     * Helper method for logging audio requests.
     *
     * @param method
     */
    private void logAudioRouteRequest(String method) {
        logger.i("Received request to route the call audio via " + method.toUpperCase());
    }

    /**
     * Helper method for logging audio route completion.
     *
     * @param method
     */
    private void logAudioRouteHandled(String method) {
        logger.i("Handled request to route the call audio via " + method.toUpperCase());
    }

    /**
     * Check if we are currently routing audio through a bluetooth device.
     *
     * @return TRUE if audio is being routed over bluetooth, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaBluetooth() {
        return getCurrentRoute() == ROUTE_BT;
    }

    /**
     * Check if we are currently routing audio through the phone's speaker.
     *
     * @return TRUE if audio is being routed over the speaker, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaSpeaker() {
        return getCurrentRoute() == ROUTE_SPEAKER;
    }

    /**
     * Check if we are currently routing audio through the phone's earpiece.
     *
     * @return TRUE if audio is being routed over the earpiece, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaEarpiece() {
        return getCurrentRoute() == ROUTE_EARPIECE;
    }

    /**
     * Check if we are currently routing audio through a wired headset.
     *
     * @return TRUE if audio is being routed through a wired headset, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaWiredHeadset() {
        return getCurrentRoute() == ROUTE_HEADSET;
    }

    /**
     * Check if bluetooth if it is currently possible to route audio via bluetooth,
     * essentially determining if a bluetooth device is connected that is capable of
     * handling phone calls.
     *
     * @return TRUE if we can route via bluetooth, otherwise FALSE.
     */
    public boolean isBluetoothRouteAvailable() {
        return bluetooth.isBluetoothCommunicationDevicePresent();
    }

    /**
     * Determine where the audio is currently being routed.
     *
     * @return
     */
    private int getCurrentRoute() {
        if (bluetooth.isOn()) {
            return ROUTE_BT;
        }

        if (audioManager.isSpeakerphoneOn()) {
            return Constants.ROUTE_SPEAKER;
        }

        if (audioManager.isWiredHeadsetOn()) {
            return Constants.ROUTE_HEADSET;
        }

        if (hasEarpiece()) {
            return Constants.ROUTE_EARPIECE;
        }

        return Constants.ROUTE_INVALID;
    }

    /**
     * Return the most recently connected bluetooth device, this may return a valid object
     * even if the device is no longer connected.
     *
     * @return The last connected BluetoothDevice
     */
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
     * Resets the audio manager, setting back to default and stopping listening for events.
     *
     */
    private void resetAndroidAudioManager() {
        if(audioManager == null) return;
        audioFocus.reset();
    }

    /**
     * Make sure we have audio focus.
     *
     */
    public void focus() {
        audioFocus.forCall();
    }

    /**
     * This broadcast receiver handles events from bluetooth headsets, as these are the only
     * events we receive when a single button press is detected on a headset with a single button
     * (i.e. no designated call/end call buttons) we have to make some assumptions and perform
     * a call action if one occurs.
     *
     * This is a bit of a hack but there does not currently seem to be any alternative method
     * of implementing this functionality.
     *
     */
    private class BluetoothHeadsetBroadcastReceiver extends BroadcastReceiver {

        private Logger logger;

        public BluetoothHeadsetBroadcastReceiver() {
            logger = new Logger(this);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            int previousState = intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, -1);

            logger.i("Received a bluetooth headset state update. Transitioned from state: " + previousState + " to: " + state);

            if (bluetoothDevice != null) {
                connectedBluetoothHeadset = bluetoothDevice;
                logger.i("Bluetooth headset detected with name: " + bluetoothDevice.getName() + ", address: " + bluetoothDevice.getAddress() + ", and class: " + bluetoothDevice.getBluetoothClass().toString());
            }

            if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED && !bluetoothManuallyDisabled && isBluetoothRouteAvailable()) {
                logger.i("This state suggests the user has pressed a button, reconnecting bluetooth and performing a single button action on the current call");
                SipService.performActionOnSipService(context, SipService.Actions.ANSWER_OR_HANGUP);
                routeAudioViaBluetooth();
            }
        }
    }
}
