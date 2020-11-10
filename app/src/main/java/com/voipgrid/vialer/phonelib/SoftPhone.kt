package com.voipgrid.vialer.phonelib

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import org.linphone.core.Call as LinphoneCall
import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.AttendedTransferSession
import org.openvoipalliance.phonelib.model.CallState
import org.openvoipalliance.phonelib.model.Reason
import org.openvoipalliance.phonelib.model.Call
import org.openvoipalliance.phonelib.repository.initialise.CallListener
import java.lang.Exception

class SoftPhone(val nativeCallManager: NativeCallManager, val localBroadcastManager: LocalBroadcastManager) {

    var callInternal: Call? = null
    var phone: PhoneLib? = null

    var call: Call?
        get() = transferCall?.to ?: callInternal
        set(call) { callInternal = call }

    var transferCall: AttendedTransferSession? = null

    val hasCall: Boolean
        get() = call != null || transferCall != null

    val isOnTransfer: Boolean
        get() = transferCall != null

    fun cleanUp() {
        call = null
        transferCall = null
    }

    fun beginAttendedTransfer(number: String) {
        try {
            call?.let { transferCall = phone?.actions(it)?.beginAttendedTransfer(number) }
        } catch (e: SecurityException) {

        }
    }

    fun finishAttendedTransfer() {
        transferCall?.let { phone?.actions(it.from)?.finishAttendedTransfer(it) }
    }

    private val canHandleIncomingCall = call == null && !nativeCallManager.isBusyWithNativeCall

    fun getSessionCallback(sipService: SipService) = SessionCallback(sipService)

    inner class SessionCallback(private val sipService: SipService): CallListener {

        override fun incomingCallReceived(call: Call) {
            fireEvent(DEFAULT_EVENT, call)

            if (!canHandleIncomingCall) {
                try {
                    phone?.actions(call)?.decline(Reason.BUSY)
                } catch (e: SecurityException) {
                }
                return
            }

            this@SoftPhone.call = call
            sipService.informUserAboutIncomingCall(call.phoneNumber, call.displayName)
        }

        override fun outgoingCallCreated(call: Call) {
            fireEvent(DEFAULT_EVENT, call)
            if (!hasCall) {
                this@SoftPhone.call = call
            }
        }

        override fun callConnected(call: Call) {
            sipService.onCallConnected()
            fireEvent(SipConstants.CALL_CONNECTED_MESSAGE, call)
        }

        override fun callEnded(call: Call) {
            handleEndedCall(call)
        }

        override fun error(call: Call) {
            handleEndedCall(call)
        }

        private fun handleEndedCall(call: Call) {
            if (!isOnTransfer) {
                fireEvent(SipConstants.CALL_DISCONNECTED_MESSAGE, call)
                if (call.reason == Reason.BUSY) {
                    sipService.playBusyTone()
                }
                sipService.stop()
                this@SoftPhone.call = null
            }

            transferCall = null
            LogHelper.logCall(call)
        }

        override fun callUpdated(session: Call) {
            fireEvent(DEFAULT_EVENT, session)
        }

        fun fireEvent(event: String, call: Call?) {
            val intent = Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS)
            intent.putExtra(SipConstants.CALL_STATUS_KEY, event)
            intent.putExtra(SipConstants.CALL_IDENTIFIER_KEY, call?.callId ?: "")
            intent.putExtra(SipConstants.CALL_STATUS_CODE, call?.reason.toString() ?: "")
            localBroadcastManager.sendBroadcast(intent)
        }
    }

    companion object {
        const val DEFAULT_EVENT = "call-update"
    }
}