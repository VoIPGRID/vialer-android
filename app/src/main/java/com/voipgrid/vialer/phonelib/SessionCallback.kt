package com.voipgrid.vialer.phonelib

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipConstants.*
import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.Reason
import org.openvoipalliance.phonelib.model.Session
import org.openvoipalliance.phonelib.repository.initialise.SessionCallback

class SessionCallback(private val broadcastManager: LocalBroadcastManager) : SessionCallback() {

    override fun error(session: Session) {
    }

    override fun incomingCall(incomingSession: Session) {
        fireEvent(DEFAULT_EVENT, incomingSession)
    }

    override fun outgoingInit(session: Session) {
        fireEvent(DEFAULT_EVENT, session)
    }

    override fun sessionConnected(session: Session) {
        fireEvent(CALL_CONNECTED_MESSAGE, session)
    }

    override fun sessionEnded(session: Session) {
        fireEvent(CALL_DISCONNECTED_MESSAGE, session)
    }

    override fun sessionReleased(session: Session) {
        fireEvent(CALL_DISCONNECTED_MESSAGE, session)
    }

    override fun sessionUpdated(session: Session) {
        fireEvent(DEFAULT_EVENT, session)
    }

    private fun fireEvent(event: String, session: Session) {
        broadcastManager.sendBroadcast(Intent().apply {
            putExtra(CALL_STATUS_KEY, event)
            putExtra(CALL_IDENTIFIER_KEY, session.callId)
            putExtra(CALL_STATUS_CODE, session.reason.name)
        })
    }

    companion object {
        const val DEFAULT_EVENT = "call-update"
    }
}