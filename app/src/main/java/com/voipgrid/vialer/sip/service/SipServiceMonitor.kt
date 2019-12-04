package com.voipgrid.vialer.sip.service

import android.os.Handler
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipService

/**
 * Monitors the SipService to make sure it is not hanging, if there is no call it is shutdown.
 *
 */
class SipServiceMonitor(private val handler: Handler) {

    private lateinit var sipService: SipService
    private val logger = Logger(this)

    /**
     * Our runnable that will check every x seconds to see if there is an active call, if not it will tell
     * the SipService to shut down.
     *
     */
    private val runnable = object : Runnable {
        override fun run() {
            if (!sipService.hasCall) {
                logger.w("The SipService does not have an active call, shutting it down.")
                sipService.stopSelf()
            } else {
                handler.postDelayed(this, CHECK_SERVICE_USER_INTERVAL_MS)
            }
        }
    }

    /**
     * Start monitoring the SipService for an active call.
     *
     */
    fun start(sipService: SipService) {
        this.sipService = sipService
        handler.postDelayed(runnable, CHECK_SERVICE_USER_INTERVAL_MS)
    }

    companion object {

        /**
         * The timeout between checks to determine the service is still running.
         *
         */
        const val CHECK_SERVICE_USER_INTERVAL_MS: Long = 20000
    }
}