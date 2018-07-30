package com.voipgrid.vialer.media;


import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;

import com.voipgrid.vialer.logging.Logger;

import java.io.IOException;

class IncomingRinger {
    private final static String TAG = IncomingRinger.class.getSimpleName();

    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private Activity mActivity;
    private Context mContext;
    private Logger mLogger;
    private Vibrator mVibrator;

    /**
     * Pattern for an incoming call when a phone is on vibrate mode.
     */
    private final long[] VIBRATOR_PATTERN = {0, 1000L, 1000L};

    IncomingRinger(Activity activity, Context context, AudioManager audioManager) {
        mActivity = activity;
        mContext = context;
        mAudioManager = audioManager;
        mLogger = new Logger(IncomingRinger.class);

        mLogger.d("IncomingRinger()");

        setRingtonePlayer();
        setVibrator();
    }

    void start() {
        mLogger.d("start()");

        if (mMediaPlayer == null) {
            mLogger.d("There is no MediaPlayer!");
            setRingtonePlayer();
        }
        if (mVibrator == null) {
            mLogger.d("There is no Vibrator!");
            setVibrator();
        }

        switch (mAudioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                mLogger.i("Play ringtone normal");
                playRingtone();
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                mLogger.i("Vibrate only");
                startVibrator();
                break;
            case AudioManager.RINGER_MODE_SILENT:
                if (AudioRouter.CURRENT_ROUTE == Constants.ROUTE_BT) {
                    mLogger.i("Bluetooth is connected, play ringtone over bluetooth headset");
                    playRingtone();
                } else {
                    mLogger.i("Silent mode so don't play anything");
                    stopRingtone();
                    stopVibrator();
                }

                break;
        }
    }

    void stop() {
        mLogger.v("stop()");
        stopRingtone();
        stopVibrator();
    }

    void pause() {
        mLogger.v("pause()");
        stopRingtone();
        stopVibrator();
    }

    void restart() {
        mLogger.v("restart()");
        stop();
        setRingtonePlayer();
        setVibrator();
        start();
    }

    private void setRingtonePlayer() {
        mLogger.v("setRingtonePlayer()");
        try {
            Uri ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
            if (mActivity.getResources().getIdentifier("ringtone", "raw", mActivity.getPackageName()) != 0) {
                mLogger.v("Custom ringtone is available");
                String ringtoneLocation = String.format("android.resource://%s/%s/%s", mActivity.getPackageName(), "raw", "ringtone");
                ringtoneUri = Uri.parse(ringtoneLocation);
            }

            // Prevent creating an extra MediaPlayer. So destroy the old one first.
            if (mMediaPlayer != null){
                mLogger.e("There was still an old MediaPlayer lingering around remove that first");
                if (mMediaPlayer.isPlaying() || mMediaPlayer.isLooping()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mContext, ringtoneUri);
            mMediaPlayer.setLooping(true);
            if (AudioRouter.CURRENT_ROUTE == Constants.ROUTE_BT) {
                // When the route is BT change the Stream type.
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                mMediaPlayer.prepare();
            } else if (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mMediaPlayer.prepare();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setVibrator() {
        mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Start to play the ringtone.
     */
    private void playRingtone() {
        mLogger.v("playRingtone()");
        if (!mMediaPlayer.isPlaying()) {
            mLogger.v("Current audio route: " + AudioRouter.CURRENT_ROUTE);
            if (AudioRouter.CURRENT_ROUTE == 3) {
                mAudioManager.setMode(Constants.DEFAULT_AUDIO_MODE);
                mActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            } else {
                mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                mActivity.setVolumeControlStream(AudioManager.STREAM_RING);
            }
            mMediaPlayer.start();
        }

        boolean vibrateWhenRinging = Settings.System.getInt(mContext.getContentResolver(), "vibrate_when_ringing", 0) == 1;
        mLogger.i("Is the option vibrate when ringing on? " + vibrateWhenRinging);
        if (vibrateWhenRinging) {
            startVibrator();
        }
    }

    /**
     * Stop playing the ringtone.
     */
    private void stopRingtone() {
        mLogger.v("stopRingtone()");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;

            mActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setMode(Constants.DEFAULT_AUDIO_MODE);
        }
    }

    /**
     * Start the vibrate for an incoming call
     */
    private void startVibrator() {
        mLogger.v("startVibrator()");
        if (mVibrator != null) {
            mVibrator.vibrate(VIBRATOR_PATTERN, 0);
        }
    }

    /**
     * Stop the vibrate for an incoming call
     */
    private void stopVibrator() {
        mLogger.v("stopVibrator()");
        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }
    }
}
