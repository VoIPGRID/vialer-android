package com.voipgrid.vialer.android.calling

import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import com.voipgrid.vialer.sip.SipService

/**
 * A connection representing a call stream, used to hook into the Android call
 * system.
 *
 */
class AndroidCallConnection(val voip: SipService) : Connection() {

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
    }

    override fun onShowIncomingCallUi() {
        voip.showIncomingCallToUser()
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        voip.onCallAudioStateChanged(state)
    }

    override fun onHold() {
        voip.currentCall.putOnHold()
        setOnHold()
    }

    override fun onUnhold() {
        voip.currentCall.takeOffHold()
        setActive()
    }

    override fun onAnswer() {
        voip.currentCall.answer()
        setActive()
    }

    override fun onReject() {
        voip.currentCall.decline()
        destroy()
    }

    override fun onDisconnect() {
        try {
            voip.currentCall.hangup(true)
        } catch (e: Exception) {}
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onSilence() {
        voip.silence()
        Log.e("TEST123", "Silence called!");
    }

    fun isBluetoothRouteAvailable(): Boolean = callAudioState != null && callAudioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH == CallAudioState.ROUTE_BLUETOOTH
}