package com.voipgrid.vialer.sip.service

import android.content.Intent
import android.telecom.CallAudioState
import android.util.Log
import android.widget.Toast
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.android.calling.AndroidCallManager
import com.voipgrid.vialer.logging.LogHelper
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.*
import com.voipgrid.vialer.sip.core.CallListener
import com.voipgrid.vialer.sip.pjsip.Pjsip
import org.pjsip.pjsua2.OnIncomingCallParam

class Handler(private val sip: SipService, private val notification: SipServiceNotificationManager, private val androidCallManager: AndroidCallManager, private val pjsip: Pjsip) : CallListener  {

    private val logger = Logger(this)

    override fun onTelephonyStateChange(call: SipCall, state: SipCall.TelephonyState): Unit = when(state) {
        SipCall.TelephonyState.INITIALIZING -> {}
        SipCall.TelephonyState.INCOMING_RINGING -> {}
        SipCall.TelephonyState.OUTGOING_RINGING -> sip.sounds.outgoingCallRinger.start()
        SipCall.TelephonyState.CONNECTED -> {
            sip.sounds.outgoingCallRinger.stop()
            sip.actions.silence()
            notification.change(SipServiceNotificationManager.Type.ACTIVE)
            when(call) {
                is OutgoingCall -> SipService.connection.setActive()
                is IncomingCall -> startCallActivityForCurrentCall()
            }
        }
        SipCall.TelephonyState.DISCONNECTED -> {
            if (!sip.isTransferring() && !call.state.wasUserHangup) {
                sip.sounds.busyTone.play()
            }

            sip.unregisterCall(call)
        }
    }.also {
        localBroadcastManager.sendBroadcast(Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS).apply {
            putExtra(SipConstants.CALL_STATUS_KEY, state.name)
        })
    }

    /**
     * When we receive an incoming call from out sip provider.
     *
     */
    fun onIncomingCall(incomingCallParam: OnIncomingCallParam, account: Pjsip.SipAccount) {
        val endpoint = pjsip.endpoint ?: throw Exception("No endpoint")

        val call = IncomingCall(this, endpoint, account, incomingCallParam.callId, SipInvite(incomingCallParam.rdata.wholeMsg))

        if (sip.currentCall != null) {
            LogHelper.using(logger).logBusyReason(sip)
            call.busy()
            return
        }

        call.beginIncomingRinging()

        sip.registerCall(call)

        androidCallManager.incomingCall()
    }


    /**
     * Android has reported that the call audio state has changed.
     *
     * @param state
     */
    fun onCallAudioStateChanged(state: CallAudioState) {
        Toast.makeText(VialerApplication.get(), state.toString(), Toast.LENGTH_LONG).show()
        sip.broadcast()
    }

    /**
     * Pjsip has successfully registered with the server.
     *
     */
    fun onRegister() {
        logger.d("onAccountRegistered")
        sip.middleware.respond()

        Log.e("TEST123", "Registered!")
        val call = sip.currentCall ?: return

        if (call.state.isIpChangeInProgress && call.state.telephonyState == SipCall.TelephonyState.INCOMING_RINGING) {
            try {
                call.reinvite(updateContact = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCallMissed(call: SipCall) {
        logger.i("A call was missed...")
    }
}