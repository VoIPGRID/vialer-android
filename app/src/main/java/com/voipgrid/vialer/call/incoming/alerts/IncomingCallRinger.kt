package com.voipgrid.vialer.call.incoming.alerts

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.voipgrid.vialer.User
import com.voipgrid.vialer.audio.AudioFocus
import com.voipgrid.vialer.logging.Logger

class IncomingCallRinger(private val context : Context, private val focus: AudioFocus) : IncomingCallAlert {

    private var logger = Logger(this)



    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val ringtone: Uri
        get() = when(User.userPreferences.usePhoneRingtone) {
            true -> Settings.System.DEFAULT_RINGTONE_URI
            false -> Uri.parse("android.resource://${context.packageName}/raw/ringtone")
        }

    /**
     * Starts playing the phone's ringtone if that is what the user has chosen.
     *
     */
    override fun start() {
        if (manager.ringerMode != AudioManager.RINGER_MODE_NORMAL || player != null) return

        player = MediaPlayer()

        focus.forRinger()

        logger.i("Starting ringer")

        player?.findRingtone()?.apply {
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
            logger.i("Attempting to use the ringtone uri: $ringtone")
            setDataSource(context, ringtone)
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
            player?.apply {
                logger.i("Stopping ringer")
                stop()
                release()
            }
            player = null
        } catch (e: Exception) {
            logger.e("Unable to stop ringer: " + e.message)
        }
    }

    override fun isStarted(): Boolean {
        return player?.isPlaying ?: false
    }

    companion object {
        private var player: MediaPlayer? = null
    }
}