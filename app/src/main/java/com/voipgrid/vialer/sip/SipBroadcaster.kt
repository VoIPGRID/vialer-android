package com.voipgrid.vialer.sip

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Helper class for sending broadcasts from background SIP classes to listening activities.
 */
class SipBroadcaster internal constructor(private val broadcastManager: LocalBroadcastManager) {

    fun broadcastServiceInfo(info: String) {
        broadcastManager.sendBroadcast(Intent(SipConstants.ACTION_BROADCAST_SERVICE_INFO).apply {
            putExtra(SipConstants.SERVICE_INFO_KEY, info)
        })
    }
}