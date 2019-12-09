package com.voipgrid.vialer.android.calling

import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipService

/**
 * A connection representing a call stream, used to hook into the Android call
 * system.
 *
 */
class AndroidCallConnection(val voip: SipService) : Connection() {

    private val logger = Logger(this)

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
    }

    override fun onShowIncomingCallUi() {
        logger.i("Showing incoming call ui")
        voip.showIncomingCallToUser()
    }

    override fun onCallAudioStateChanged(state: CallAudioState) {
        logger.i("Call audio state changed $state")
        voip.onCallAudioStateChanged(state)
    }

    override fun onHold() {
        logger.i("Putting call on hold")
        voip.currentCall!!.putOnHold()
        setOnHold()
    }

    override fun onUnhold() {
        logger.i("Taking call off hold")
        voip.currentCall!!.takeOffHold()
        setActive()
    }

    override fun onAnswer() {
        logger.i("Answering call")
        voip.currentCall!!.answer()
        setActive()
    }

    override fun onReject() {
        voip.currentCall!!.decline()
        destroy()
    }

    override fun onDisconnect() {
        logger.i("Disconnecting call")
        try {
            voip.currentCall!!.hangup(true)
        } catch (e: Exception) {}
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onSilence() {
        logger.i("Silencing call ringer")
        voip.silence()
    }

    fun isBluetoothRouteAvailable(): Boolean = callAudioState != null && callAudioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH == CallAudioState.ROUTE_BLUETOOTH
}