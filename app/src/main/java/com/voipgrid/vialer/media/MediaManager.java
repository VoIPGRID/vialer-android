package com.voipgrid.vialer.media;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.statistics.AppState;

/**
 * MediaManager manager class for the call activity.
 */
public class MediaManager implements
        AudioManager.OnAudioFocusChangeListener, AudioRouter.AudioRouterInterface {

    private Context mContext;
    private AudioManager mAudioManager;
    private AudioLostListener audioLostListener;
    private Logger mLogger;

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
    private static MediaManager mMediaManager;

    /**
     * Private constructor for the class.
     *
     * @param context Reference to the Context object from where the class is created.
     */
    private MediaManager(Activity activity, Context context) {
        mContext = context;

        mLogger = new Logger(MediaManager.class);

        setAudioManager();

        // Make sure the hardware volume buttons control the volume of the call.
        activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mAudioRouter = new AudioRouter(this);
    }

    /**
     * Get an instance reference to this class.
     *
     * @return this MediaManager class.
     */
    public MediaManager getInstance() {
        return mMediaManager;
    }


    public static synchronized MediaManager init(Activity activity, Context context) {
        if (mMediaManager == null) {
            mMediaManager = new MediaManager(activity, context);
        }
        return mMediaManager;
    }

    public void setAudioLostListener(AudioLostListener listener) {
        audioLostListener = listener;
    }

    public void deInit() {
        mLogger.v("deInit()");

        if(mAudioRouter != null) {
            mAudioRouter.deInit();
        }

        mMediaManager = null;
        resetAudioManager();

        mAudioRouter = null;
    }

    public void callStarted() {
        mLogger.v("callStarted()");
        mAudioRouter.onStartingCall();
    }

    /**
     * Function for the call activity when a call has been answered
     */
    public void callAnswered() {
        mLogger.v("callAnswered()");
        if(mAudioRouter == null) {
            mAudioRouter = new AudioRouter(this);
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
        mLogger.v("callEnded()");
        if (mAudioRouter != null) {
            mAudioRouter.onEndedCall();
        }
        BluetoothMediaButtonReceiver.setCallAnswered(false);
    }

    /**
     *
     */
    public void callOutgoing() {
        mLogger.v("callOutgoing");
        mAudioRouter.onOutgoingCall();
    }

    /**
     * Will activate or deactivate the bluetooth.
     *
     * @param activate boolean true will activate bluetooth; false will deactivate bluetooth
     */
    public void useBluetoothAudio(boolean activate) {
        mLogger.i("userBluetoothAudio()");
        mLogger.i("==> " + activate);

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
     * When the user wants to enable the speaker function.
     *
     * @param onSpeaker Whether to turn the speaker mode on or off.
     */
    public void setCallOnSpeaker(boolean onSpeaker) {
        mLogger.i("setCallOnSpeaker()");
        mLogger.i("==> " + onSpeaker);

        if (mAudioRouter == null) {
            mLogger.w("Attempted to change speaker setting after audio router has been deinited");
            return;
        }

        mCallIsOnSpeaker = onSpeaker;
        mAudioRouter.enableSpeaker(onSpeaker);
    }

    /**
     * Check if the audio is currently being played through the speaker.
     *
     * @return TRUE if audio is being played through speaker, otherwise FALSE.
     */
    public boolean isCallOnSpeaker() {
        return mAudioManager != null && mAudioManager.isSpeakerphoneOn();

    }

    /**
     * Reset the AudioManger back to default.
     * and release the audio focus for the app.
     */
    private void resetAudioManager() {
        mLogger.v("resetAudioManager()...");

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
        mLogger.v("onAudioFocusChange()...");
        mLogger.v("====> " + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mLogger.i("We gained audio focus!");
                if (mPreviousVolume != -1) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mPreviousVolume, 0);
                    mPreviousVolume = -1;
                }

                mLogger.i("Was the audio lost: " + mAudioIsLost);
                if (mAudioIsLost) {
                    mAudioIsLost = false;
                    mAudioRouter.setAudioIsLost(false);
                    mAudioRouter.reconnectBluetoothSco();
                    audioLostListener.audioWasLost(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS:
                mLogger.i("Lost audio focus! Probably incoming native audio call.");
                mAudioIsLost = true;
                audioLostListener.audioWasLost(true);
                mAudioRouter.setAudioIsLost(true);
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mLogger.i("We must lower our audio volume! Probably incoming notification / driving directions.");
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, 0);
                if (CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                    mAudioRouter.setAudioIsLost(true);
                    audioLostListener.audioWasLost(true);
                }
                break;
        }
    }

    @Override
    public void audioRouteUpdate(int newRoute) {
        mLogger.d("audioRouteUpdate()");
        mLogger.d("==> newRoute: " + newRoute + " oldRoute: ");
    }

    @Override
    public void btDeviceConnected(boolean connected) {
        mLogger.d("btDeviceConnected()");
        mLogger.d("==> " + connected);
        AppState.isUsingBluetoothAudio = connected;
    }

    @Override
    public void btAudioConnected(boolean connected) {
        mLogger.d("btAudioConnected()");
        mLogger.d("==> " + connected);

        if (!connected) {
            mAudioRouter.configureSpeakerPhone(mCallIsOnSpeaker);
        }
    }

    public AudioRouter getAudioRouter() {
        return mAudioRouter;
    }

    public interface AudioLostListener {
        void audioWasLost(boolean lost);
    }
}
