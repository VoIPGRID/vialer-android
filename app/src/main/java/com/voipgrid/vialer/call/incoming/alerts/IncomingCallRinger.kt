package com.voipgrid.vialer.call.incoming.alerts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings
import com.voipgrid.vialer.logging.Logger
import java.lang.Exception

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

        player = MediaPlayer().apply{
            setAudioStreamType(AudioManager.STREAM_RING)
            setDataSource(context, Settings.System.DEFAULT_RINGTONE_URI)
            isLooping = true
            prepare()
            start()
        }
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

        }
    }
}