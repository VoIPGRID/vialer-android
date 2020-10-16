package com.voipgrid.vialer.phonelib

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.call.NativeCallManager
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.statistics.VialerStatistics
import org.linphone.core.Call
import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.AttendedTransferSession
import org.openvoipalliance.phonelib.model.CallState
import org.openvoipalliance.phonelib.model.Reason
import org.openvoipalliance.phonelib.model.Session
import java.lang.Exception
import org.openvoipalliance.phonelib.repository.initialise.SessionCallback as SessionCallbackLib

class SoftPhone(val nativeCallManager: NativeCallManager, val localBroadcastManager: LocalBroadcastManager) {

    var callInternal: Session? = null
    var phone: PhoneLib? = null

    var call: Session?
        get() = transferSession?.to ?: callInternal
        set(call) { callInternal = call }

    var transferSession: AttendedTransferSession? = null

    val hasCall: Boolean
        get() = call != null || transferSession != null

    val isOnTransfer: Boolean
        get() = transferSession != null

    fun cleanUp() {
        call = null
        transferSession = null
    }

    fun beginAttendedTransfer(number: String) {
        try {
            call?.let { transferSession = phone?.beginAttendedTransfer(it, number) }
        } catch (e: SecurityException) {

        }
    }

    fun finishAttendedTransfer() {
        transferSession?.let { phone?.finishAttendedTransfer(it) }
    }

    private val canHandleIncomingCall = call == null && !nativeCallManager.isBusyWithNativeCall

    fun getSessionCallback(sipService: SipService) = SessionCallback(sipService)

    inner class SessionCallback(private val sipService: SipService): SessionCallbackLib() {

        override fun incomingCall(incomingSession: Session) {
            fireEvent(DEFAULT_EVENT, incomingSession)
Log.e("TEST123", "INCOMING!!!")
            if (!canHandleIncomingCall) {
                try {
                    phone?.declineIncoming(incomingSession, Reason.BUSY)
                } catch (e: SecurityException) {
                }
                return
            }

            if (call != null) {
                VialerStatistics.incomingCallFailedDueToOngoingVialerCall(incomingSession)
            }

            if (nativeCallManager.isBusyWithNativeCall) {
                VialerStatistics.incomingCallFailedDueToOngoingGsmCall(incomingSession)
            }

            call = incomingSession
            sipService.informUserAboutIncomingCall(incomingSession.phoneNumber, incomingSession.displayName)
        }

        override fun outgoingInit(session: Session) {
            fireEvent(DEFAULT_EVENT, session)
            if (!hasCall) {
                call = session
            }
        }

        override fun sessionConnected(session: Session) {
            sipService.onCallConnected()
            fireEvent(SipConstants.CALL_CONNECTED_MESSAGE, session)
        }

        override fun sessionEnded(session: Session) {
            handleEndedCall(session)
        }

        override fun sessionReleased(session: Session) {
            handleEndedCall(session)
        }

        override fun error(session: Session) {
            handleEndedCall(session)
        }

        private fun handleEndedCall(session: Session) {
            if (!isOnTransfer) {
                fireEvent(SipConstants.CALL_DISCONNECTED_MESSAGE, session)
                if (session.reason == Reason.BUSY) {
                    sipService.playBusyTone()
                }
                sipService.stop()
                call = null
            }

            transferSession = null
            LogHelper.logCall(session)
        }

        override fun sessionUpdated(session: Session) {
            fireEvent(DEFAULT_EVENT, session)
        }

        private fun fireEvent(event: String, call: Session) {
            val intent = Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS)
            intent.putExtra(SipConstants.CALL_STATUS_KEY, event)
            intent.putExtra(SipConstants.CALL_IDENTIFIER_KEY, call.callId ?: "")
            intent.putExtra(SipConstants.CALL_STATUS_CODE, call.reason.toString())
            localBroadcastManager.sendBroadcast(intent)
        }
    }

    companion object {
        const val DEFAULT_EVENT = "call-update"
    }
}