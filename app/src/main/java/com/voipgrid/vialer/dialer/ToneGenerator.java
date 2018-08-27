package com.voipgrid.vialer.dialer;

import android.os.Handler;

import com.voipgrid.vialer.logging.RemoteLogger;

/**
 * This is a wrapper around Android's ToneGenerator class with the purpose of improving the stability of
 * Android's ToneGenerator by automatically releasing the ToneGenerator when it has not been used and
 * by preventing a crash in the situation where creating the ToneGenerator fails.
 */
public class ToneGenerator {

    /**
     * The stream type to pass directly to the Android ToneGenerator.
     */
    private int mStreamType;

    /**
     * The volume to pass directly to the Android ToneGenerator.
     */
    private int mVolume;

    /**
     * The native Android ToneGenerator that this class is wrapping, it will be created only
     * when needed and destroyed after it has been idle for {@value #DESTROY_TONE_GENERATOR_AFTER_MS}
     * milliseconds.
     */
    private android.media.ToneGenerator mAndroidToneGenerator;

    /**
     * Release and destroy the ToneGenerator this many MS after the tone has finished
     * playing.
     */
    private static final int DESTROY_TONE_GENERATOR_AFTER_MS = 5000;

    /**
     * Releases the resources from the ToneGenerator when the tone has finished playing.
     */
    private final Runnable mToneGeneratorDestroyer = new Runnable() {
        @Override
        public void run() {
            if(mAndroidToneGenerator == null) return;

            mAndroidToneGenerator.release();
            mAndroidToneGenerator = null;
        }
    };

    private RemoteLogger mRemoteLogger;

    private static Handler sHandler = new Handler();

    public ToneGenerator(int streamType, int volume) {
        mStreamType = streamType;
        mVolume = volume;
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
    }

    /**
     * Play a tone, recover without crash if the ToneGenerator cannot be created. If the ToneGenerator cannot
     * be created then the tone will not be played but an exception will be logged.
     *
     * @see android.media.ToneGenerator#startTone(int, int)
     * @return boolean TRUE if the tone was played successfully, otherwise FALSE.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean startTone(int toneType, int durationMs) {
        sHandler.removeCallbacks(mToneGeneratorDestroyer);

        android.media.ToneGenerator androidToneGenerator = getAndroidToneGenerator();

        if(androidToneGenerator == null) {
            mRemoteLogger.e("Attempted to play tone (" + toneType + ") with duration: " + durationMs + " but ToneGenerator could not be initialized, this tone was not played.");
            return false;
        }

        sHandler.postDelayed(mToneGeneratorDestroyer, durationMs + DESTROY_TONE_GENERATOR_AFTER_MS);

        return getAndroidToneGenerator().startTone(toneType, durationMs);
    }

    /**
     * Attempts to create the android tone generator, if it fails null will be returned.
     *
     * @return android.media.ToneGenerator or null on failure
     */
    private android.media.ToneGenerator getAndroidToneGenerator() {
        if(mAndroidToneGenerator != null) return mAndroidToneGenerator;

        try {
            return mAndroidToneGenerator = new android.media.ToneGenerator(mStreamType, mVolume);
        } catch (RuntimeException e) {
            mRemoteLogger.e("Failed to initialize Android ToneGenerator");
            return null;
        }
    }

    /**
     * Provides access to the constants from Android's ToneGenerator.
     */
    public static final class Constants extends android.media.ToneGenerator {

        private Constants(int streamType, int volume) {
            super(streamType, volume);
        }
    }
}
