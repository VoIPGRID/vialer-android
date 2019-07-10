package com.voipgrid.vialer.call.incoming.alerts

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.provider.Settings
import com.voipgrid.vialer.logging.Logger

class IncomingCallRinger(val context : Context) : IncomingCallAlert {

    private var logger = Logger(this)

    private var player: MediaPlayer? = null

    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Starts playing the phone's ringtone if that is what the user has chosen.
     *
     */
    override fun start() {
        if (manager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        manager.apply {
            requestAudioFocus(null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN)
            mode = AudioManager.MODE_RINGTONE
        }

        logger.i("Starting ringer")

        player = MediaPlayer().findRingtone()?.apply {
            setAudioStreamType(AudioManager.STREAM_RING)
            isLooping = true
            prepare()
            start()
        }
    }

    /**
     * Attempts to intelligently find the correct ringtone to be playing, if none is found,
     * null will be returned.
     *
     */
    private fun MediaPlayer.findRingtone() : MediaPlayer? {
        try {
            logger.i("Attempting to use the DEFAULT_RINGTONE_URI")
            setDataSource(context, Settings.System.DEFAULT_RINGTONE_URI)
            return this
        } catch (e: Exception) {
        }

        try {
            logger.w("Unable to use the DEFAULT_RINGTONE_URI, falling back to finding the first stored ringtone")
            RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_RINGTONE)
            }.let {
                it.cursor
                setDataSource(context, it.getRingtoneUri(1))
            }
            return this
        } catch (e: Exception) {
            logger.e("Unable to find any usable ringtone, we will not be playing anything")
        }

        return null
    }

    /**
     * Stop playing the phone's ringtone.
     *
     */
    override fun stop() {
        try {
            logger.i("Stopping ringer")

            player?.apply {
                stop()
                release()
            }
            player = null
        } catch (e: Exception) {
            logger.e("Unable to stop ringer: " + e.message)
        }
    }
}