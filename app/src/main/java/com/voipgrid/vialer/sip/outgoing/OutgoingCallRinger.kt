package com.voipgrid.vialer.sip.outgoing

import android.os.Handler
import androidx.core.os.postDelayed
import com.voipgrid.vialer.dialer.ToneGenerator
import com.voipgrid.vialer.logging.Logger

class OutgoingCallRinger(private val toneGenerator: ToneGenerator, private val handler: Handler) {

    private val logger = Logger(this)

    private val runnable = object : Runnable {
        override fun run() {
            toneGenerator.startTone(ToneGenerator.Constants.TONE_SUP_DIAL, 1000)
            handler.postDelayed(this, TIME_BETWEEN_RINGS)
        }
    }

    fun start() {
        logger.i("Starting outgoing call ringer")
        handler.postDelayed(runnable, TIME_BEFORE_FIRST_RING)
    }

    fun stop() {
        logger.i("Stopping outgoing call ringer")
        handler.removeCallbacks(runnable)
    }

    companion object {
        const val TIME_BEFORE_FIRST_RING: Long = 2000

        const val TIME_BETWEEN_RINGS: Long = 4000
    }
}