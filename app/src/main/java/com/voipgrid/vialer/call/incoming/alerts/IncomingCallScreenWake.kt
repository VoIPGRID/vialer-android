package com.voipgrid.vialer.call.incoming.alerts

import android.os.PowerManager

class IncomingCallScreenWake(private val powerManager: PowerManager) : IncomingCallAlert {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun start() {
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG)
        wakeLock?.acquire(30000)
    }

    override fun stop() {
        val wakeLock = this.wakeLock ?: return

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        this.wakeLock = null
    }

    override fun isStarted(): Boolean = powerManager.isInteractive

    companion object {
        const val TAG = "vialer:call-incoming"
    }
}