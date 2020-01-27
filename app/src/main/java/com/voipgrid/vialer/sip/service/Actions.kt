package com.voipgrid.vialer.sip.service

import android.util.Log
import com.voipgrid.vialer.android.calling.AndroidCallConnection
import com.voipgrid.vialer.android.calling.AndroidCallManager
import com.voipgrid.vialer.android.calling.AndroidCallService
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.OutgoingCall
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.sip.pjsip.Pjsip
import com.voipgrid.vialer.sip.transfer.CallTransferResult

class Actions(private val sip: SipService, private val connection: AndroidCallConnection, private val androidCallManager: AndroidCallManager, private val pjsip: Pjsip) {

    private val logger = Logger(this)

    fun hold() {
        connection.onHold()
    }

    fun unhold() {
        connection.onUnhold()
    }

    fun reject() {
        connection.onReject()
    }

    fun disconnect() {
        connection.onDisconnect()
    }

    fun answer() {
        connection.onAnswer()
    }

    /**
     * This will hangup the correct call.
     *
     */
    fun hangup() {
        if (sip.isTransferring()) {
            sip.currentCall?.hangup(true)
            return
        }

        connection.onDisconnect()
    }

    /**
     * Reinvite the current call.
     *
     */
    fun reinvite() {
        val call = sip.currentCall ?: return

        if (!call.state.isIpChangeInProgress) {
            try {
                call.reinvite()
            } catch (e: Exception) {
                logger.e("Unable to reinvite call")
            }
        }
    }

    /**
     * Start a transfer to the target number.
     *
     */
    fun startTransfer(target: String) {
        if (sip.isTransferring()) throw Exception("Unable to start transfer when there is already one in progress")

        makeOutgoingCall(target)
    }

    /**
     * Merge the current transfer.
     *
     */
    fun mergeTransfer(): CallTransferResult {
        if (!sip.isTransferring()) throw Exception("Unable to merge transfer that has not started")

        val firstCall = sip.firstCall ?: throw Exception("Must have a first call to transfer")
        val currentCall = sip.currentCall ?: throw Exception("Must have a current call to transfer")

        val result = CallTransferResult(firstCall.phoneNumber, currentCall.phoneNumber)

        firstCall.xFerReplaces(currentCall)

        return result
    }

    /**
     * Initialise an outgoing call and either route it via the android call manager or display it immediately
     *
     * @param number The phone number to call.
     *
     */
    fun placeOutgoingCall(number: String) {
        if (sip.currentCall != null) {
            logger.i("Attempting to initialise a second outgoing call but this is not currently supported")
            sip.startCallActivityForCurrentCall()
            return
        }

        AndroidCallService.outgoingCallback = {
            makeOutgoingCall(number)
            sip.showOutgoingCallToUser()
        }

        androidCallManager.call(number)
    }

    /**
     * Creates a call object for an outgoing call.
     *
     */
    private fun makeOutgoingCall(number: String) = OutgoingCall(
            sip.handler,
            pjsip.endpoint ?: throw Exception(),
            pjsip.account,
            number
    ).also { sip.registerCall(it) }
}