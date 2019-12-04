package com.voipgrid.vialer.sip.utils

import android.os.Handler
import com.voipgrid.vialer.dialer.ToneGenerator
import com.voipgrid.vialer.sip.SipConstants

class BusyTone(private val toneGenerator: ToneGenerator) {

    /**
     * Play a busy tone when the user hangs up.
     *
     */
    fun play() {
        try {
            toneGenerator.startTone(ToneGenerator.Constants.TONE_CDMA_NETWORK_BUSY, BUSY_TONE_DURATION.toInt())
            Thread.sleep(BUSY_TONE_DURATION)
        } catch (ignored: InterruptedException) {
        }
    }

    companion object {
        var BUSY_TONE_DURATION: Long = 2000
    }
}