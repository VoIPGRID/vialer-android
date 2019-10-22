package com.voipgrid.vialer.voip.core.call

import com.voipgrid.vialer.voip.core.Configuration

class State {
    var isOnHold = false
    var isMuted = false
    var dtmfDialed = ""
    var isTransferTarget = false
    var telephonyState = TelephonyState.INITIALIZING

    var codec: Configuration.Codec? = null

    fun isConnected() = telephonyState == TelephonyState.CONNECTED

    enum class TelephonyState {
        INITIALIZING, OUTGOING_CALLING, INCOMING_RINGING, CONNECTED, DISCONNECTED
    }
}