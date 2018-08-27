package com.voipgrid.vialer.media;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.statistics.AppState;

/**
 * MediaManager manager class for the call activity.
 */
public class MediaManager implements
        AudioManager.OnAudioFocusChangeListener, AudioRouter.AudioRouterInterface {

    private static MediaManager mMediaManager;

    private static int mCurrentAudioRoute;

    private Context mContext;
    private AudioManager mAudioManager;
    private AudioChangedInterface mAudioChangedInterfaceListener;
    private RemoteLogger mRemoteLogger;
    private IncomingRinger mIncomingRinger;

    static int CURRENT_CALL_STATE = Constants.CALL_INVALID;

    private AudioRouter mAudioRouter;
    /**
     * When an other app has taken over the audio. Keep a boolean so we can recover later when
     * we get the audio back.
     */
    private boolean mAudioIsLost = false;

    /**
     * Whether the call is on speaker.
     */
    private boolean mCallIsOnSpeaker = false;

    /**
     * When the app can duck for audio loss. To keep reference to the previous volume level.
     */
    private int mPreviousVolume = -1;

    /**
     * Private constructor for the class.
     *
     * @param context Reference to the Context object from where the class is created.
     */
    private MediaManager(Activity activity, Context context, AudioChangedInterface audioChangedInterface) {
        mContext = context;
        mAudioChangedInterfaceListener = audioChangedInterface;

        mRemoteLogger = new RemoteLogger(MediaManager.class).enableConsoleLogging();

        setAudioManager();

        // Make sure the hardware volume buttons control the volume of the call.
        activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mAudioRouter = new AudioRouter(context, this, mAudioManager);
        mIncomingRinger = new IncomingRinger(activity, context, mAudioManager);
    }

    /**
     * Singleton creation for this class.
     *
     * @param context Reference to the context from where the class is created.
     * @param audioChangedInterface Callback reference so can sent updates from this class.
     *
     * @return this MediaManager class.
     */
    public static synchronized MediaManager init(Activity activity, Context context, AudioChangedInterface audioChangedInterface) {
        if (mMediaManager == null) {
            mMediaManager = new MediaManager(activity, context, audioChangedInterface);
        }
        return mMediaManager;
    }

    public void deInit() {
        mRemoteLogger.v("deInit()");

        if(mAudioRouter != null) {
            mAudioRouter.deInit();
        }

        if(mIncomingRinger != null) {
            mIncomingRinger.stop();
        }

        resetAudioManager();

        mMediaManager = null;
        mAudioRouter = null;
        mIncomingRinger = null;
    }

    public void callStarted() {
        mRemoteLogger.v("callStarted()");
        mAudioRouter.onStartingCall();
    }

    /**
     * Function for the call activity when a call has been answered
     */
    public void callAnswered() {
        mRemoteLogger.v("callAnswered()");
        if(mAudioRouter == null) {
            mAudioRouter = new AudioRouter(mContext, this, mAudioManager);
            mAudioRouter.onAnsweredCall();
        } else {
            mAudioRouter.onAnsweredCall();
        }
        BluetoothMediaButtonReceiver.setCallAnswered(true);
    }

    /**
     * Function for the call activity when a call has been ended.
     */
    public void callEnded() {
        mRemoteLogger.v("callEnded()");
        if (mAudioRouter != null) {
            mAudioRouter.onEndedCall();
        }
        BluetoothMediaButtonReceiver.setCallAnswered(false);
    }

    /**
     *
     */
    public void callOutgoing() {
        mRemoteLogger.v("callOutgoing");
        mAudioRouter.onOutgoingCall();
    }

    /**
     * Will activate or deactivate the bluetooth.
     *
     * @param activate boolean true will activate bluetooth; false will deactivate bluetooth
     */
    public void useBluetoothAudio(boolean activate) {
        mRemoteLogger.i("userBluetoothAudio()");
        mRemoteLogger.i("==> " + activate);

        if (activate) {
            mAudioRouter.enableBTSco();
        } else {
            mAudioRouter.enableEarpiece();
        }
    }

    /**
     * Initialize the Android AudioManager.
     */
    private void setAudioManager() {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
        );
        mAudioManager.setMode(Constants.DEFAULT_AUDIO_MODE);
    }

    /**
     * Get the ringer mode for when there is an incoming call.
     */
    public void startIncomingCallRinger() {
        mRemoteLogger.d("startIncomingCallRinger()");
        if (mIncomingRinger != null) {
            mIncomingRinger.start();
        }
    }

    /**
     * Stop the incoming call ringer.
     */
    public void stopIncomingCallRinger() {
        mRemoteLogger.d("stopIncomingCallRinger");
        if (mIncomingRinger != null) {
            mIncomingRinger.stop();
        }
    }

    /**
     * When the user wants to enable the speaker function.
     *
     * @param onSpeaker Whether to turn the speaker mode on or off.
     */
    public void setCallOnSpeaker(boolean onSpeaker) {
        mRemoteLogger.i("setCallOnSpeaker()");
        mRemoteLogger.i("==> " + onSpeaker);

        mCallIsOnSpeaker = onSpeaker;
        mAudioRouter.enableSpeaker(onSpeaker);
    }

    /**
     * Reset the AudioManger back to default.
     * and release the audio focus for the app.
     */
    private void resetAudioManager() {
        mRemoteLogger.v("resetAudioManager()...");

        if(mAudioManager == null) return;

        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mAudioManager.abandonAudioFocus(this);
    }

    /**
     * MediaManager focus change listener
     *
     * @param focusChange int The focus change value.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        mRemoteLogger.v("onAudioFocusChange()...");
        mRemoteLogger.v("====> " + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mRemoteLogger.i("We gained audio focus!");
                if (mPreviousVolume != -1) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mPreviousVolume, 0);
                    mPreviousVolume = -1;
                }

                mRemoteLogger.i("Was the audio lost: " + mAudioIsLost);
                if (mAudioIsLost) {
                    mAudioIsLost = false;
                    mAudioRouter.setAudioIsLost(false);
                    mRemoteLogger.e("CURRENT_ROUTE: " + AudioRouter.CURRENT_ROUTE);
//                    if (AudioRouter.CURRENT_ROUTE == Constants.ROUTE_BT) {
                        mAudioRouter.reconnectBluetoothSco();
//                    }

                    if (CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                        mIncomingRinger.start();
                    }
                    mAudioChangedInterfaceListener.audioLost(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS:
                mRemoteLogger.i("Lost audio focus! Probably incoming native audio call.");
                mAudioIsLost = true;
                mAudioChangedInterfaceListener.audioLost(true);
                mIncomingRinger.pause();
                mAudioRouter.setAudioIsLost(true);
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mRemoteLogger.i("We must lower our audio volume! Probably incoming notification / driving directions.");
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, 0);
                if (CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                    mIncomingRinger.pause();
                    mAudioRouter.setAudioIsLost(true);
                    mAudioChangedInterfaceListener.audioLost(true);
                }
                break;
        }
    }

    @Override
    public void audioRouteUpdate(int newRoute) {
        mRemoteLogger.d("audioRouteUpdate()");
        mRemoteLogger.d("==> newRoute: " + newRoute + " oldRoute: " + mCurrentAudioRoute);
        if (newRoute != mCurrentAudioRoute) {
            mCurrentAudioRoute = newRoute;
            if (mCurrentAudioRoute == Constants.ROUTE_BT) {
                if (CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                    if (mIncomingRinger != null) {
                        mIncomingRinger.restart();
                    }
                }
            }
        }
    }

    @Override
    public void btDeviceConnected(boolean connected) {
        mRemoteLogger.d("btDeviceConnected()");
        mRemoteLogger.d("==> " + connected);
        AppState.isUsingBluetoothAudio = connected;
        mAudioChangedInterfaceListener.bluetoothDeviceConnected(connected);
    }

    @Override
    public void btAudioConnected(boolean connected) {
        mRemoteLogger.d("btAudioConnected()");
        mRemoteLogger.d("==> " + connected);

        if (!connected) {
            mAudioManager.setSpeakerphoneOn(mCallIsOnSpeaker);
        }
        mAudioChangedInterfaceListener.bluetoothAudioAvailable(connected);
    }

    /**
     * Interface for when a class implements the MediaManager.
     */
    public interface AudioChangedInterface {
        void bluetoothDeviceConnected(boolean connected);
        void bluetoothAudioAvailable(boolean available);
        void audioLost(boolean lost);
    }
}
