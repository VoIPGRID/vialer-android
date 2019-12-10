package com.voipgrid.vialer.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipCall
import com.voipgrid.vialer.sip.SipCall.TelephonyState.*
import com.voipgrid.vialer.sip.SipConstants

class CallStatusReceiver(private val listener: Listener) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = valueOf(intent.getStringExtra(SipConstants.CALL_STATUS_KEY))

        when (status) {
            CONNECTED -> listener.onCallConnected()
            DISCONNECTED -> listener.onCallDisconnected()
            else -> {}
        }

        listener.onCallStatusChanged(status)
    }

    interface Listener {
        fun onCallStatusChanged(status: SipCall.TelephonyState) {}
        fun onCallConnected() {}
        fun onCallDisconnected() {}
        fun onServiceStopped() {}
    }
}