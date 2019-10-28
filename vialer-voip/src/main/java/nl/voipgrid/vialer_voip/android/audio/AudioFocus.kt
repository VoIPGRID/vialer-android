package nl.voipgrid.vialer_voip.android.audio

import android.media.AudioManager

/**
 * The class is completely responsible for controlling audio focus, all audio focus
 * requests should be made via thia.
 *
 */
class AudioFocus(private val manager: AudioManager) {

//    private val logger = Logger(this)
    private val handler = AudioFocusHandler()

    /**
     * Focus audio for the incoming call ringer.
     *
     */
    fun forRinger() {
//        logger.i("Setting audio focus for RINGER")

        manager.apply {
            requestAudioFocus(handler, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN)
            mode = AudioManager.MODE_RINGTONE
        }
    }

    /**
     * Focus audio for calls.
     *
     */
    fun forCall() {
//        logger.i("Setting audio focus for CALL")

        manager.apply {
            requestAudioFocus(handler, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN)
            mode = AudioManager.MODE_IN_COMMUNICATION
        }
    }

    /**
     * Reset the audio focus back to its previous state.
     *
     */
    fun reset() {
//        logger.i("Resetting audio focus")

        manager.apply {
            mode = AudioManager.MODE_NORMAL
            isSpeakerphoneOn = false
            abandonAudioFocus(handler)
        }
    }

    private class AudioFocusHandler: AudioManager.OnAudioFocusChangeListener {

//        private val logger = Logger(this)

        override fun onAudioFocusChange(focusChange: Int) {
//            logger.i("Received audio focus change event: $focusChange")
        }
    }
}