package com.voipgrid.vialer.call.incoming.alerts

import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class IncomingCallVibration(private val audioManager: AudioManager, private val vibrator: Vibrator) : IncomingCallAlert {

    private val pattern = longArrayOf(0, 1000L, 1000L)

    override fun start() {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    override fun stop() {
        vibrator.cancel()
    }
}