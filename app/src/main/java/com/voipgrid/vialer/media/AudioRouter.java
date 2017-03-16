package com.voipgrid.vialer.media;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.view.KeyEvent;

import com.voipgrid.vialer.logging.RemoteLogger;

/**
 * Class is responsible for in call where the audio will be routed to.
 * Either earpiece / headset / bluetooth devices.
 */
class AudioRouter {
    private static final String TAG = AudioRouter.class.getSimpleName();

    private Context mContext;
    private AudioManager mAudioManager;
    private RemoteLogger mRemoteLogger;

    // Bluetooth audio SCO states. Example of valid state sequence:
    // SCO_INVALID -> SCO_TURNING_ON -> SCO_ON -> SCO_TURNING_OFF -> SCO_OFF.
    private static final int STATE_BLUETOOTH_SCO_INVALID = -1;
    private static final int STATE_BLUETOOTH_SCO_OFF = 0;
    private static final int STATE_BLUETOOTH_SCO_ON = 1;
    private static final int STATE_BLUETOOTH_SCO_TURNING_ON = 2;
    private static final int STATE_BLUETOOTH_SCO_TURNING_OFF = 3;

    // wired HS defines
    private static final int STATE_WIRED_HS_INVALID = -1;
    private static final int STATE_WIRED_HS_UNPLUGGED = 0;
    private static final int STATE_WIRED_HS_PLUGGED = 1;

    private static final int STATE_EARPIECE_INVALID = -1;
    private static final int STATE_EARPIECE_OFF = 0;
    private static final int STATE_EARPIECE_ON = 1;

    // The current route the audio is flowing through
    static int CURRENT_ROUTE;

    // Enabled during initialization if BLUETOOTH permission is granted.
    private boolean mHasBluetoothPermission = true;

    // Stores the audio states for a wired headset
    private int mWiredHsState = STATE_WIRED_HS_UNPLUGGED;

    // Broadcast receiver for Bluetooth SCO broadcasts.
    // Utilized to detect if BT SCO streaming is on or off.
    private BroadcastReceiver mBluetoothScoReceiver = null;
    private BroadcastReceiver mBluetoothHeadsetReceiver = null;
    private BroadcastReceiver mWiredHeadsetReceiver = null;

    // Stores the audio states related to Bluetooth SCO audio, where some
    // states are needed to keep track of intermediate states while the SCO
    // channel is enabled or disabled (switching state can take a few seconds).
    private int mBluetoothScoState = STATE_BLUETOOTH_SCO_INVALID;
    private int mBluetoothScoStateBeforeSpeakerOn = STATE_BLUETOOTH_SCO_INVALID;

    private boolean mHasBluetoothHeadset = false;
    private boolean mSelfDisabledBluetooth = false;

    private AudioRouterInterface mAudioRouterInterface;

    private boolean mAudioIsLost;

    AudioRouter(Context context, AudioRouterInterface audioRouterInterface, AudioManager audioManager) {
        mContext = context;
        mAudioManager = audioManager;

        mRemoteLogger = new RemoteLogger(context, AudioRouter.class, 1);
        mRemoteLogger.d("AudioRouter()");

        registerForBluetoothScoIntentBroadcast();
        registerForBluetoothHeadsetIntentBroadcast();
        registerForWiredHeadsetIntentBroadcast();

        mAudioRouterInterface = audioRouterInterface;

        mContext.startService(new Intent(mContext, BluetoothMediaSessionService.class));
    }

    void deInit() {
        mRemoteLogger.v("deInit()");
        stopBluetoothSco();
        unregisterForBluetoothHeadsetIntentBroadcast();
        unregisterForBluetoothScoIntentBroadcast();
        unregisterForWiredHeadsetIntentBroadcast();
        mContext.stopService(new Intent(mContext, BluetoothMediaSessionService.class));
    }

    int enableSpeaker(boolean on) {
        mRemoteLogger.d("enableSpeaker()");
        mRemoteLogger.d("==> " + on);

        int route = getAudioRoute();
        if (route == Constants.ROUTE_BT) {
            stopBluetoothSco();
        }
        mAudioManager.setSpeakerphoneOn(on);

        return 0;
    }

    int enableHeadset() {
        mRemoteLogger.v("enableHeadset()");
        if (!hasEarpiece()) {
            mRemoteLogger.v("==> There is no earpiece..");
            return STATE_WIRED_HS_INVALID;
        }

        int curRoute = getAudioRoute();
        if (curRoute == Constants.ROUTE_HEADSET) {
            mRemoteLogger.v("==> Can't use earpiece when there is an headset connected");
            // Cannot use earpiece when a headset is plugged in.
            return STATE_WIRED_HS_INVALID;
        }

        if (curRoute == Constants.ROUTE_BT) {
            mRemoteLogger.v("==> There is a bluetooth connection so disconnect this");
            stopBluetoothSco();
        }

        mAudioManager.setSpeakerphoneOn(false);
        return STATE_WIRED_HS_UNPLUGGED;
    }

    void reconnectBluetoothSco() {
        mRemoteLogger.v("reconnectBluetoothSco()");
        if (hasBluetoothHeadset() && !mSelfDisabledBluetooth) {
            mBluetoothScoState = mAudioManager.isBluetoothScoOn() ? STATE_BLUETOOTH_SCO_ON : STATE_BLUETOOTH_SCO_INVALID;
            startBluetoothSco();
        }
    }

    int enableBTSco() {
        mRemoteLogger.v("enableBSco()");

        mSelfDisabledBluetooth = !mSelfDisabledBluetooth;
        if (hasBluetoothHeadset()) {
            startBluetoothSco();
            return 0;
        } else {
            return -1;
        }
    }

    int enableEarpiece() {
        mRemoteLogger.v("enableEarpiece()");

        if (!hasEarpiece()) {
            mRemoteLogger.d("===> no earpiece");
            return STATE_EARPIECE_INVALID;
        }

        int route = getAudioRoute();
        if (route == Constants.ROUTE_HEADSET) {
            mRemoteLogger.d("===> ROUTE_HEADSET");
            // Cannot use earpiece when a headset is plugged in.
            return STATE_EARPIECE_INVALID;
        }

        if (route == Constants.ROUTE_BT) {
            mRemoteLogger.d("===> ROUTE_BT");
            stopBluetoothSco();
        }

        mSelfDisabledBluetooth = true;
        mAudioManager.setSpeakerphoneOn(false);
        return STATE_EARPIECE_ON;
    }

    private int getAudioRoute() {
        mRemoteLogger.i("getAudioRoute()");
        int route = Constants.ROUTE_INVALID;
        if (mBluetoothScoState == STATE_BLUETOOTH_SCO_ON) {
            route = Constants.ROUTE_BT;
            mRemoteLogger.i("==> ROUTE_BT");
            mAudioRouterInterface.btAudioConnected(true);
        } else if (mAudioManager.isSpeakerphoneOn()) {
            route = Constants.ROUTE_SPEAKER;
            mRemoteLogger.i("==> ROUTE_SPEAKER");
        } else if (mWiredHsState == STATE_WIRED_HS_PLUGGED) {
            route = Constants.ROUTE_HEADSET;
            mRemoteLogger.i("==> ROUTE_HEADSET");
        } else {
            if (hasEarpiece()) {
                route = Constants.ROUTE_EARPIECE;
                mRemoteLogger.i("==> ROUTE_EARPIECE");
            }
        }
        CURRENT_ROUTE = route;
        return route;
    }

    private void updateRoute() {
        mRemoteLogger.v("updateRoute()");
        int route = getAudioRoute();
        mAudioRouterInterface.audioRouteUpdate(route);
    }

    void onStartingCall() {
        mRemoteLogger.d("onStartingCall()");
        mRemoteLogger.d("==> Current Call State: " + MediaManager.CURRENT_CALL_STATE);

        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_INVALID) {
            MediaManager.CURRENT_CALL_STATE = Constants.CALL_RINGING;
            mHasBluetoothHeadset = hasBluetoothHeadset();
            if (mHasBluetoothHeadset) {
                mAudioRouterInterface.btDeviceConnected(true);
                startBluetoothSco();
            } else {
                mAudioRouterInterface.btDeviceConnected(false);
            }

            mBluetoothScoStateBeforeSpeakerOn = STATE_BLUETOOTH_SCO_INVALID;
            updateRoute();
        }
    }

    void onAnsweredCall() {
        mRemoteLogger.v("onAnsweredCall()");
        MediaManager.CURRENT_CALL_STATE = Constants.CALL_ANSWERED;
    }

    void onOutgoingCall() {
        mRemoteLogger.v("onOutgoingCall()");
        mRemoteLogger.v("==> Current Call State: " + MediaManager.CURRENT_CALL_STATE);
        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_INVALID) {
            MediaManager.CURRENT_CALL_STATE = Constants.CALL_OUTGOING;

            mHasBluetoothHeadset = hasBluetoothHeadset();
            if (mHasBluetoothHeadset) {
                mAudioRouterInterface.btDeviceConnected(true);
                startBluetoothSco();
            } else {
                mAudioRouterInterface.btDeviceConnected(false);
            }

            mBluetoothScoStateBeforeSpeakerOn = STATE_BLUETOOTH_SCO_INVALID;
            updateRoute();
        }
    }

    void onEndedCall() {
        mRemoteLogger.v("onEndedCall()");
        MediaManager.CURRENT_CALL_STATE = Constants.CALL_INVALID;
        if (getAudioRoute() == Constants.ROUTE_BT) {
            mRemoteLogger.v("Route is BT so try to stop the connection");
            stopBluetoothSco();
        }
    }

    private void registerForWiredHeadsetIntentBroadcast() {
        mRemoteLogger.v("registerForWiredHeadsetIntentBroadcast()");

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

        mWiredHeadsetReceiver = new BroadcastReceiver() {
            private static final int STATE_UNPLUGGED = 0;
            private static final int STATE_PLUGGED = 1;
            private static final int HAS_NO_MIC = 0;
            private static final int HAS_MIC = 1;

            @Override
            public void onReceive(Context context, Intent intent) {
                mRemoteLogger.v("onReceive()");
                int state = intent.getIntExtra("state", STATE_UNPLUGGED);
                int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);

                String name = intent.getStringExtra("name");
                mRemoteLogger.v("==> action: " + intent.getAction()
                        + "\n state: " + state
                        + "\n microphone: " + microphone
                        + "\n name: " + name
                        + "\n stickyBroadcast: " + isInitialStickyBroadcast()
                );

                switch (state) {
                    case STATE_UNPLUGGED:
                        mRemoteLogger.v("==> Headset unplugged");
                        mWiredHsState = STATE_WIRED_HS_UNPLUGGED;
                        break;
                    case STATE_PLUGGED:
                        mRemoteLogger.v("==> Headset plugged");
                        mWiredHsState = STATE_WIRED_HS_PLUGGED;
                        break;
                    default:
                        mRemoteLogger.v("==> Headset invalid state");
                        mWiredHsState = STATE_WIRED_HS_INVALID;
                        break;
                }
                updateRoute();
            }
        };

        mContext.registerReceiver(mWiredHeadsetReceiver, intentFilter);
    }

    private void unregisterForWiredHeadsetIntentBroadcast() {
        mRemoteLogger.v("unregisterForWiredHeadsetIntentBroadcast()");
        mContext.unregisterReceiver(mWiredHeadsetReceiver);
        mWiredHeadsetReceiver = null;
    }

    private void registerForBluetoothHeadsetIntentBroadcast() {
        mRemoteLogger.v("registerForBluetoothHeadsetIntentBroadcast()");

        IntentFilter intentFilter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

        mBluetoothHeadsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int profileState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                mRemoteLogger.v("onReceive: ==> action: " + intent.getAction()
                        + "\n state: " + profileState
                        + "\n stickyBroadcast: " + isInitialStickyBroadcast()
                );

                switch (profileState) {
                    case BluetoothProfile.STATE_DISCONNECTED:
                        mRemoteLogger.v("==> Bluetooth disconnected");
                        mAudioRouterInterface.btDeviceConnected(false);
                        break;
                    case BluetoothProfile.STATE_CONNECTED:
                        mRemoteLogger.v("==> Bluetooth connected");
                        mAudioRouterInterface.btDeviceConnected(true);
                        break;
                    case BluetoothProfile.STATE_CONNECTING:
                        mRemoteLogger.v("==> Bluetooth connecting");
                        break;
                    case BluetoothProfile.STATE_DISCONNECTING:
                        mRemoteLogger.v("==> Bluetooth disconnecting");
                        break;
                    default:
                        mRemoteLogger.v("==> Bluetooth invalid state");
                        break;
                }
            }
        };

        mContext.registerReceiver(mBluetoothHeadsetReceiver, intentFilter);
    }

    private void unregisterForBluetoothHeadsetIntentBroadcast() {
        mRemoteLogger.v("unregisterForBluetoothHeadsetIntentBroadcast()");
        mContext.unregisterReceiver(mBluetoothHeadsetReceiver);
        mBluetoothHeadsetReceiver = null;
    }

    private void registerForBluetoothScoIntentBroadcast() {
        mRemoteLogger.v("registerForBluetoothScoIntentBroadcast()");

        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);

        mBluetoothScoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_DISCONNECTED);

                mRemoteLogger.v("==> onReceive() action: " + intent.getAction()
                        + "\n state: " + state
                        + "\n stickyBroadcast: " + isInitialStickyBroadcast()
                        + "\n isBluetoothScoOn: " + mAudioManager.isBluetoothScoOn()
                );

                // Little hack to catch a single click on the headset. This will sent an KeyEvent to the BluetoothMediaButtonReceiver.
                if (getAudioRoute() == Constants.ROUTE_BT && !mAudioIsLost) {
                    if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED && hasBluetoothHeadset() && MediaManager.CURRENT_CALL_STATE != Constants.CALL_INVALID && !mSelfDisabledBluetooth) {
                        mRemoteLogger.i("SCO wants to disconnect but the device is still connected, maybe trigger button click?");
                        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CALL);
                        BluetoothMediaButtonReceiver.handleKeyEvent(mContext, keyEvent);
                        if (MediaManager.CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                            mBluetoothScoState = STATE_BLUETOOTH_SCO_OFF;
                            startBluetoothSco();
                        }
                        return;
                    }
                }

                switch (state) {
                    case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                        mRemoteLogger.v("==> Bluetooth sco audio connected");

                        mBluetoothScoState = STATE_BLUETOOTH_SCO_ON;
                        mAudioRouterInterface.btAudioConnected(true);
                        break;
                    case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                        mRemoteLogger.v("==> Bluetooth sco audio disconnected");
                        mBluetoothScoState = STATE_BLUETOOTH_SCO_OFF;
                        mAudioRouterInterface.btAudioConnected(false);
                        break;
                    case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                        if (mAudioManager.isBluetoothScoOn()) {
                            mBluetoothScoState = STATE_BLUETOOTH_SCO_ON;
                        }
                        mRemoteLogger.v("==> Bluetooth sco audio connecting");
                        break;
                    default:
                        mBluetoothScoState = STATE_BLUETOOTH_SCO_INVALID;
                        mRemoteLogger.v("==> Bluetooth sco invalid state");
                        break;
                }

                updateRoute();
            }
        };

        mContext.registerReceiver(mBluetoothScoReceiver, intentFilter);
    }

    private void unregisterForBluetoothScoIntentBroadcast() {
        mRemoteLogger.v("unregisterForBluetoothScoIntentBroadcast()");
        mContext.unregisterReceiver(mBluetoothScoReceiver);
        mBluetoothScoReceiver = null;
    }

    private boolean hasBluetoothHeadset() {
        mRemoteLogger.v("hasBluetoothHeadset()");
        if (!mHasBluetoothPermission) {
            mRemoteLogger.e("No bluetooth permission!?");
            return false;
        }

        BluetoothAdapter bluetoothAdapter;
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            mRemoteLogger.e("BluetoothAdapter.getDefaultAdapter() exception: " + e.getMessage());
            return false;
        }

        if (bluetoothAdapter == null) {
            mRemoteLogger.e("There is no bluetoothAdapter!?");
            return false;
        }

        int profileConnectionState;
        try {
            profileConnectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        } catch (Exception e) {
            mRemoteLogger.e("BluetoothAdapter.getProfileConnectionState() exception: " + e.getMessage());
            profileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        }
        mRemoteLogger.i("Bluetooth profile connection state: " + profileConnectionState);
        return bluetoothAdapter.isEnabled() && profileConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    private void startBluetoothSco() {
        mRemoteLogger.v("startBluetoothSco()");
        if (!mHasBluetoothPermission) {
            mRemoteLogger.e("No bluetooth permission?");
            return;
        }

        if (mBluetoothScoState == STATE_BLUETOOTH_SCO_ON || mBluetoothScoState == STATE_BLUETOOTH_SCO_TURNING_ON) {
            mRemoteLogger.i("==> Bluetooth already turned on or turning on!");
            return;
        }

        if (mAudioManager.isBluetoothScoOn()) {
            mBluetoothScoState = STATE_BLUETOOTH_SCO_ON;
            mRemoteLogger.i("==> Bluetooth already on!");
            return;
        }

        mRemoteLogger.i("Bluetooth is turning on.");

        mBluetoothScoState = STATE_BLUETOOTH_SCO_TURNING_ON;
        mAudioManager.setBluetoothScoOn(true);
        mAudioManager.startBluetoothSco();
    }

    private void stopBluetoothSco() {
        mRemoteLogger.v("stopBluetoothSco()");
        if (!mHasBluetoothPermission) {
            mRemoteLogger.e("==> No bluetooth permission!?");
            return;
        }

        mRemoteLogger.d("BluetoothState: " + mBluetoothScoState);
        mRemoteLogger.d("Statecheck: " + (!((mBluetoothScoState == STATE_BLUETOOTH_SCO_ON) || (mBluetoothScoState == STATE_BLUETOOTH_SCO_TURNING_ON))));
        if (!((mBluetoothScoState == STATE_BLUETOOTH_SCO_ON) || (mBluetoothScoState == STATE_BLUETOOTH_SCO_TURNING_ON))) {
            mRemoteLogger.i("==> No need to turn off Bluetooth in this state");
            return;
        }

        if (!mAudioManager.isBluetoothScoOn()) {
            mRemoteLogger.i("==> Unable to stop Bluetooth sco since it is already disabled!");
            return;
        }

        mRemoteLogger.d("==> turning Bluetooth sco off");
        mBluetoothScoState = STATE_BLUETOOTH_SCO_TURNING_OFF;
        mAudioManager.stopBluetoothSco();
        mAudioManager.setBluetoothScoOn(false);

        int retries = 0;
        while(mBluetoothScoState != STATE_BLUETOOTH_SCO_OFF && retries < 10) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();

            mRemoteLogger.i("Retry of stopping bluetooth sco: " + retries);

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

    public void setAudioIsLost(boolean lost) {
        mAudioIsLost = lost;
    }

    interface AudioRouterInterface {
        void audioRouteUpdate(int newRoute);
        void btDeviceConnected(boolean connected);
        void btAudioConnected(boolean connected);
    }
}
