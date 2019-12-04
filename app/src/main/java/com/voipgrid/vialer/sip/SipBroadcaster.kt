package com.voipgrid.vialer.sip

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.sip.SipConstants.CallMissedReason

/**
 * Helper class for sending broadcasts from background SIP classes to listening activities.
 */
class SipBroadcaster internal constructor(private val broadcastManager: LocalBroadcastManager) {

    fun broadcastServiceInfo(info: String) {
        broadcastManager.sendBroadcast(Intent(SipConstants.ACTION_BROADCAST_SERVICE_INFO).apply {
            putExtra(SipConstants.SERVICE_INFO_KEY, info)
        })
    }

    fun broadcastCallStatus(identifier: String, status: String) {
        broadcastManager.sendBroadcast(Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS).apply {
            putExtra(SipConstants.CALL_IDENTIFIER_KEY, identifier)
            putExtra(SipConstants.CALL_STATUS_KEY, status)
        })
    }

    fun broadcastMissedCalls(reason: CallMissedReason) {
        broadcastManager.sendBroadcast(Intent(SipConstants.ACTION_BROADCAST_CALL_MISSED).apply {
            putExtra(SipConstants.CALL_MISSED_KEY, reason)
        })
    }
}