package com.voipgrid.vialer.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipConstants

class CallStatusReceiver(private val mListener: Listener) : BroadcastReceiver() {

    private val mLogger: Logger = Logger(this)

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getStringExtra(SipConstants.CALL_STATUS_KEY) ?: return
        val callId = intent.getStringExtra(SipConstants.CALL_IDENTIFIER_KEY) ?: return

        when (status) {
            SipConstants.CALL_CONNECTED_MESSAGE -> mListener.onCallConnected()
            SipConstants.CALL_DISCONNECTED_MESSAGE -> mListener.onCallDisconnected()
            SipConstants.CALL_PUT_ON_HOLD_ACTION -> mListener.onCallHold()
            SipConstants.CALL_UNHOLD_ACTION -> mListener.onCallUnhold()
            SipConstants.CALL_RINGING_OUT_MESSAGE -> mListener.onCallRingingOut()
            SipConstants.CALL_RINGING_IN_MESSAGE -> mListener.onCallRingingIn()
            SipConstants.SERVICE_STOPPED -> mListener.onServiceStopped()
        }

        mListener.onCallStatusChanged(status, callId)
    }

    interface Listener {
        fun onCallStatusChanged(status: String, callId: String) {}
        fun onCallConnected() {}
        fun onCallDisconnected() {}
        fun onCallHold() {}
        fun onCallUnhold() {}
        fun onCallRingingOut() {}
        fun onCallRingingIn() {}
        fun onServiceStopped() {}
    }
}