package com.voipgrid.vialer.call.incoming.alerts

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings

class IncomingCallRinger(val context : Context) : IncomingCallAlert {

    private val player : MediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI)
    private val manager : AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun start() {
        if (manager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        manager.requestAudioFocus(null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN)
        manager.mode = AudioManager.MODE_RINGTONE

        player.isLooping = true
        player.start()
    }

    override fun stop() {
        player.stop()
    }
}