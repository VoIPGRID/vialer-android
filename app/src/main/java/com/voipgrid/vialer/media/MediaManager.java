package com.voipgrid.vialer.media;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.statistics.AppState;

import javax.inject.Inject;

/**
 * MediaManager manager class for the call activity.
 */
public class MediaManager implements AudioManager.OnAudioFocusChangeListener {

    @Inject AudioRouter audioRouter;
    private Context mContext;
    private AudioManager mAudioManager;
    private AudioLostListener audioLostListener;
    private Logger mLogger;

    static int CURRENT_CALL_STATE = Constants.CALL_INVALID;

    /**
     * When an other app has taken over the audio. Keep a boolean so we can recover later when
     * we get the audio back.
     */
    private boolean mAudioIsLost = false;

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
        VialerApplication.get().component().inject(this);
        mContext = context;

        mLogger = new Logger(MediaManager.class);

        setAudioManager();

        // Make sure the hardware volume buttons control the volume of the call.
        activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
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

        if(audioRouter != null) {
            audioRouter.deInit();
        }

        mMediaManager = null;
        resetAudioManager();

        audioRouter = null;
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
        audioRouter.provideAudioManager(mAudioManager);
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
                    audioRouter.routeAudioViaBluetooth();
                    audioLostListener.audioWasLost(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS:
                mLogger.i("Lost audio focus! Probably incoming native audio call.");
                mAudioIsLost = true;
                audioLostListener.audioWasLost(true);
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mLogger.i("We must lower our audio volume! Probably incoming notification / driving directions.");
                mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 1, 0);
                if (CURRENT_CALL_STATE == Constants.CALL_RINGING) {
                    audioLostListener.audioWasLost(true);
                }
                break;
        }
    }

    public AudioRouter getAudioRouter() {
        return audioRouter;
    }

    public interface AudioLostListener {
        void audioWasLost(boolean lost);
    }
}
