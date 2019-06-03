package com.voipgrid.vialer.media;

import static com.voipgrid.vialer.media.BluetoothMediaSessionService
        .SHOULD_NOT_START_IN_FOREGROUND_EXTRA;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.util.BroadcastReceiverManager;

import javax.inject.Inject;

/**
 * Class is responsible for in call where the audio will be routed to.
 * Either earpiece / headset / bluetooth devices.
 */
public class AudioRouter {

    @Inject Context mContext;
    @Inject AudioManager mAudioManager;
    @Inject BroadcastReceiverManager broadcastReceiverManager;
    private Logger mLogger;

    // wired HS defines
    private static final int STATE_WIRED_HS_INVALID = -1;
    private static final int STATE_WIRED_HS_UNPLUGGED = 0;
    private static final int STATE_WIRED_HS_PLUGGED = 1;

    // Stores the audio states for a wired headset
    private int mWiredHsState = STATE_WIRED_HS_UNPLUGGED;

    // Broadcast receiver for Bluetooth SCO broadcasts.
    // Utilized to detect if BT SCO streaming is on or off.
    private BroadcastReceiver mBluetoothScoReceiver = new BluetoothScoReceiver();
    private BroadcastReceiver mBluetoothHeadsetReceiver = new BluetoothHeadsetReceiver();
    private BroadcastReceiver mWiredHeadsetReceiver = new WiredHeadsetBroadcastReceiver();

    private AudioRouterInterface mAudioRouterInterface;

    private boolean mAudioIsLost;

    AudioRouter(AudioRouterInterface audioRouterInterface) {
        VialerApplication.get().component().inject(this);

        mLogger = new Logger(AudioRouter.class);
        mLogger.d("AudioRouter()");

        broadcastReceiverManager.unregisterReceiver(mBluetoothScoReceiver, mBluetoothHeadsetReceiver, mWiredHeadsetReceiver);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mBluetoothScoReceiver, AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mBluetoothHeadsetReceiver, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mWiredHeadsetReceiver, Intent.ACTION_HEADSET_PLUG);

        mAudioRouterInterface = audioRouterInterface;

        startMediaButtonService();
    }

    void deInit() {
        mLogger.v("deInit()");
        stopBluetoothSco();
        broadcastReceiverManager.unregisterReceiver(mBluetoothScoReceiver, mBluetoothHeadsetReceiver, mWiredHeadsetReceiver);
        mContext.stopService(new Intent(mContext, BluetoothMediaSessionService.class));
    }

    void enableSpeaker(boolean on) {
        mLogger.d("enableSpeaker()");
        mLogger.d("==> " + on);

        int route = getAudioRoute();
        if (route == Constants.ROUTE_BT) {
            stopBluetoothSco();
        }

        configureSpeakerPhone(on);
    }

    void reconnectBluetoothSco() {
        mLogger.v("reconnectBluetoothSco()");
        if (isBluetoothCommunicationDevicePresent() && !isCurrentlyRoutingAudioViaBluetooth()) {
            startBluetoothSco();
        }
    }

    void enableBTSco() {
        mLogger.v("enableBSco()");

        if (isBluetoothCommunicationDevicePresent()) {
            startBluetoothSco();
        }
    }

    void enableEarpiece() {
        mLogger.v("enableEarpiece()");

        if (!hasEarpiece()) {
            mLogger.v("===> no earpiece");
            return;
        }

        int route = getAudioRoute();
        if (route == Constants.ROUTE_HEADSET) {
            mLogger.v("===> ROUTE_HEADSET");
            // Cannot use earpiece when a headset is plugged in.
            return;
        }

        if (route == Constants.ROUTE_BT) {
            mLogger.v("===> ROUTE_BT");
            stopBluetoothSco();
        }

        configureSpeakerPhone(false);
    }

    void configureSpeakerPhone(boolean on) {
        if (on && !mAudioManager.isSpeakerphoneOn()) {
            mAudioManager.setSpeakerphoneOn(true);
        } else if (!on && mAudioManager.isSpeakerphoneOn()) {
            mAudioManager.setSpeakerphoneOn(false);
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
        mLogger.v("hasBluetoothHeadset()");

        BluetoothAdapter bluetoothAdapter;
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            mLogger.e("BluetoothAdapter.getDefaultAdapter() exception: " + e.getMessage());
            return false;
        }

        if (bluetoothAdapter == null) {
            mLogger.e("There is no bluetoothAdapter!?");
            return false;
        }

        int profileConnectionState;
        try {
            profileConnectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        } catch (Exception e) {
            mLogger.e("BluetoothAdapter.getProfileConnectionState() exception: " + e.getMessage());
            profileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        }
        mLogger.i("Bluetooth profile connection state: " + profileConnectionState);
        return bluetoothAdapter.isEnabled() && profileConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    /**
     * Check if we are currently routing audio through a bluetooth device.
     *
     * @return TRUE if audio is being routed over bluetooth, otherwise FALSE.
     */
    public boolean isCurrentlyRoutingAudioViaBluetooth() {
        return mAudioManager.isBluetoothScoOn();
    }

    /**
     * Determine where the audio is currently being routed.
     *
     * @return
     */
    private int getAudioRoute() {
        mLogger.i("getAudioRoute()");

        if (mAudioManager.isBluetoothScoOn()) {
            mLogger.i("==> ROUTE_BT");
            mAudioRouterInterface.btAudioConnected(true);
            return Constants.ROUTE_BT;
        }

        if (mAudioManager.isSpeakerphoneOn()) {
            mLogger.i("==> ROUTE_SPEAKER");
            return Constants.ROUTE_SPEAKER;
        }

        if (mWiredHsState == STATE_WIRED_HS_PLUGGED) {
            mLogger.i("==> ROUTE_HEADSET");
            return Constants.ROUTE_HEADSET;
        }

        if (hasEarpiece()) {
            mLogger.i("==> ROUTE_EARPIECE");
            return Constants.ROUTE_EARPIECE;
        }

        return Constants.ROUTE_INVALID;
    }

    private void updateRoute() {
        mLogger.v("updateRoute()");
        int route = getAudioRoute();
        mAudioRouterInterface.audioRouteUpdate(route);
    }

    void onStartingCall() {
        mLogger.d("onStartingCall()");
        mLogger.d("==> Current Call State: " + MediaManager.CURRENT_CALL_STATE);

        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_INVALID) {
            MediaManager.CURRENT_CALL_STATE = Constants.CALL_RINGING;
            if (isBluetoothCommunicationDevicePresent()) {
                mAudioRouterInterface.btDeviceConnected(true);
                startBluetoothSco();
            } else {
                mAudioRouterInterface.btDeviceConnected(false);
            }

            updateRoute();
        }
    }

    void onAnsweredCall() {
        mLogger.v("onAnsweredCall()");
        MediaManager.CURRENT_CALL_STATE = Constants.CALL_ANSWERED;
    }

    void onOutgoingCall() {
        mLogger.v("onOutgoingCall()");
        mLogger.v("==> Current Call State: " + MediaManager.CURRENT_CALL_STATE);
        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_INVALID) {
            MediaManager.CURRENT_CALL_STATE = Constants.CALL_OUTGOING;

            if (isBluetoothCommunicationDevicePresent()) {
                mAudioRouterInterface.btDeviceConnected(true);
                startBluetoothSco();
            } else {
                mAudioRouterInterface.btDeviceConnected(false);
            }

            updateRoute();
        }
    }

    void onEndedCall() {
        mLogger.v("onEndedCall()");
        MediaManager.CURRENT_CALL_STATE = Constants.CALL_INVALID;
        if (getAudioRoute() == Constants.ROUTE_BT) {
            mLogger.v("Route is BT so try to stop the connection");
            stopBluetoothSco();
        }
    }

    private void startBluetoothSco() {
        mLogger.v("startBluetoothSco()");

        if (mAudioManager.isBluetoothScoOn()) {
            mLogger.i("==> Bluetooth already on!");
            return;
        }

        mLogger.i("Bluetooth is turning on.");

        mAudioManager.setBluetoothScoOn(true);
        mAudioManager.startBluetoothSco();
    }

    private void stopBluetoothSco() {
        mLogger.v("stopBluetoothSco()");

        if (!mAudioManager.isBluetoothScoOn()) {
            mLogger.i("==> No need to turn off Bluetooth in this state");
            return;
        }

        if (!mAudioManager.isBluetoothScoOn()) {
            mLogger.i("==> Unable to stop Bluetooth sco since it is already disabled!");
            return;
        }

        mLogger.d("==> turning Bluetooth sco off");
        mAudioManager.stopBluetoothSco();
        mAudioManager.setBluetoothScoOn(false);

        int retries = 0;
        while(mAudioManager.isBluetoothScoOn() && retries < 10) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();

            mLogger.i("Retry of stopping bluetooth sco: " + retries);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}

            if (!mAudioManager.isBluetoothScoOn()) {
                mAudioRouterInterface.btAudioConnected(false);
                return;
            }

            retries++;
        }

        mAudioRouterInterface.btAudioConnected(false);
    }

    private boolean hasEarpiece() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    void setAudioIsLost(boolean lost) {
        mAudioIsLost = lost;
    }

    private void startMediaButtonService() {
        Intent intent = new Intent(mContext, BluetoothMediaSessionService.class);
        intent.putExtra(SHOULD_NOT_START_IN_FOREGROUND_EXTRA, true);
        mContext.startService(intent);
    }

    interface AudioRouterInterface {
        void audioRouteUpdate(int newRoute);
        void btDeviceConnected(boolean connected);
        void btAudioConnected(boolean connected);
    }

    private class BluetoothHeadsetReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int profileState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
            mLogger.v("onReceive: ==> action: " + intent.getAction()
                    + "\n state: " + profileState
                    + "\n stickyBroadcast: " + isInitialStickyBroadcast()
            );

            switch (profileState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    mLogger.v("==> Bluetooth disconnected");
                    mAudioRouterInterface.btDeviceConnected(false);
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    mLogger.v("==> Bluetooth connected");
                    mAudioRouterInterface.btDeviceConnected(true);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    mLogger.v("==> Bluetooth connecting");
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    mLogger.v("==> Bluetooth disconnecting");
                    break;
                default:
                    mLogger.v("==> Bluetooth invalid state");
                    break;
            }
        }
    }

    private class BluetoothScoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);

            mLogger.v("==> onReceive() action: " + intent.getAction()
                    + "\n state: " + state
                    + "\n stickyBroadcast: " + isInitialStickyBroadcast()
                    + "\n isBluetoothScoOn: " + mAudioManager.isBluetoothScoOn()
            );

            // Little hack to catch a single click on the headset. This will sent an KeyEvent to the BluetoothMediaButtonReceiver.
            if (getAudioRoute() == Constants.ROUTE_BT && !mAudioIsLost) {
                if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED && isBluetoothCommunicationDevicePresent() && MediaManager.CURRENT_CALL_STATE != Constants.CALL_INVALID && isCurrentlyRoutingAudioViaBluetooth()) {
                    mLogger.i("SCO wants to disconnect but the device is still connected, maybe trigger button click?");
                    if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_ANSWERED) {
                        mLogger.i("Call already in progress end call");
                        KeyEvent hangupKeyEvent = new KeyEvent(
                                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENDCALL
                        );
                        BluetoothMediaButtonReceiver.handleKeyEvent(mContext, hangupKeyEvent);
                    } else {
                        mLogger.i("Call still ringing pick up call / answered hangup call");
                        KeyEvent pickupKeyEvent = new KeyEvent(
                                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL
                        );
                        BluetoothMediaButtonReceiver.handleKeyEvent(mContext, pickupKeyEvent);
                        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                            startBluetoothSco();
                            return;
                        }
                    }
                }
            }


            switch (state) {
                case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                    mLogger.v("==> Bluetooth sco audio connected");

                    mAudioRouterInterface.btAudioConnected(true);
                    break;
                case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                    mLogger.v("==> Bluetooth sco audio disconnected");
                    mAudioRouterInterface.btAudioConnected(false);
                    break;
                case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                    mLogger.v("==> Bluetooth sco audio connecting");
                    break;
                default:
                    mLogger.v("==> Bluetooth sco invalid state");
                    break;
            }

            updateRoute();
        }
    }

    private class WiredHeadsetBroadcastReceiver extends BroadcastReceiver {
        private static final int STATE_UNPLUGGED = 0;
        private static final int STATE_PLUGGED = 1;
        private static final int HAS_NO_MIC = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            mLogger.v("onReceive()");
            int state = intent.getIntExtra("state", STATE_UNPLUGGED);
            int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);

            String name = intent.getStringExtra("name");
            mLogger.v("==> action: " + intent.getAction()
                    + "\n state: " + state
                    + "\n microphone: " + microphone
                    + "\n name: " + name
                    + "\n stickyBroadcast: " + isInitialStickyBroadcast()
            );

            switch (state) {
                case STATE_UNPLUGGED:
                    mLogger.v("==> Headset unplugged");
                    mWiredHsState = STATE_WIRED_HS_UNPLUGGED;
                    break;
                case STATE_PLUGGED:
                    mLogger.v("==> Headset plugged");
                    mWiredHsState = STATE_WIRED_HS_PLUGGED;
                    break;
                default:
                    mLogger.v("==> Headset invalid state");
                    mWiredHsState = STATE_WIRED_HS_INVALID;
                    break;
            }
            updateRoute();
        }
    }
}
