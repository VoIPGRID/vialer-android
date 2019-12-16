package com.voipgrid.vialer.sip

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.sip.SipConstants.CallMissedReason
import org.pjsip.pjsua2.pjsip_status_code

/**
 * Helper class for sending broadcasts from background SIP classes to listening activities.
 */
class SipBroadcaster(sipService: SipService) {
    private val broadcastManager = LocalBroadcastManager.getInstance(sipService)

    fun broadcastServiceInfo(info: String) {
        val intent = Intent(SipConstants.ACTION_BROADCAST_SERVICE_INFO).apply {
            putExtra(SipConstants.SERVICE_INFO_KEY, info)
        }

        broadcastManager.sendBroadcast(intent)
    }

    @JvmOverloads
    fun broadcastCallStatus(identifier: String, status: String, code: pjsip_status_code? = null) {
        val intent = Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS).apply {
            putExtra(SipConstants.CALL_IDENTIFIER_KEY, identifier)
            putExtra(SipConstants.CALL_STATUS_KEY, status)

            if (code != null) {
                putExtra(SipConstants.CALL_STATUS_CODE, code.swigValue())
            }

        }

        broadcastManager.sendBroadcast(intent)
    }

    fun broadcastMissedCalls(reason: CallMissedReason?) {
        val intent = Intent(SipConstants.ACTION_BROADCAST_CALL_MISSED).apply {
            putExtra(SipConstants.CALL_MISSED_KEY, reason)
        }

        broadcastManager.sendBroadcast(intent)
    }
}