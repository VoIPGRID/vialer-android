package com.voipgrid.vialer.sip.core

import android.widget.Toast
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.permissions.MicrophonePermission
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.core.Action.*

class SipActionHandler(private val sipService: SipService) {

    private val logger = Logger(this)

    /**
     * Handle a giving sip action.
     *
     */
    fun handle(action: Action): Unit = when(action) {
        HANDLE_INCOMING_CALL -> sipService.initialiseIncomingCall()
        HANDLE_OUTGOING_CALL -> sipService.initialiseOutgoingCall()
        DECLINE_INCOMING_CALL -> SipService.connection.onReject()
        ANSWER_INCOMING_CALL -> askForPermissionsThenAnswer()
        END_CALL -> SipService.connection.onDisconnect()
        DISPLAY_CALL_IF_AVAILABLE -> showCallIfAvailable()
    }.also {
        logger.i("Executing Sip Action: ${action.name}")
    }

    /**
     * Returns TRUE if the action given should prompt a foreground start.
     *
     */
    fun isForegroundAction(action: Action): Boolean = listOf(HANDLE_INCOMING_CALL, HANDLE_OUTGOING_CALL).contains(action)

    /**
     * Start the call activity if there is a call in progress.
     *
     */
    private fun showCallIfAvailable() {
        if (sipService.currentCall != null && sipService.currentCall.isConnected) {
            sipService.startCallActivityForCurrentCall()
        } else {
            sipService.stopSelf()
        }
    }

    /**
     * Prompt the user to accept the microphone permission if they do not have it, then answer the call.
     *
     */
    private fun askForPermissionsThenAnswer() {
        if (!MicrophonePermission.hasPermission(sipService))
        {
            Toast.makeText(sipService, sipService.getString(R.string.permission_microphone_missing_message), Toast.LENGTH_LONG).show()
            logger.e("Unable to answer incoming call as we do not have microphone permission")
            return
        }

        SipService.connection.onAnswer()
    }
}