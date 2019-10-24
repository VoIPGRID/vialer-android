package nl.voipgrid.vialer_voip.core.call

import nl.voipgrid.vialer_voip.core.Configuration

class State {

    /**
     * When set to true, the call is on hold.
     *
     */
    var isOnHold = false

    /**
     * When set to true, no audio is being transmitted to the third party of this call.
     *
     */
    var isMuted = false

    /**
     * A string of the dtmf that has been sent during this call.
     *
     */
    var dtmfDialed = ""

    /**
     * Is set to true when this call has been initiated as a transfer target, rather than as a fresh
     * call.
     *
     */
    var isTransferTarget = false

    /**
     * Get the current telephony state of this call.
     *
     */
    val telephonyState: TelephonyState get() = stateHistory.last()

    /**
     * Get the last unique state of this call, this will always be a different state to the [telephonyState] unless there is only
     * a single state.
     *
     */
    val previousTelephonyState get() = if (stateHistory.size >= 2) stateHistory[stateHistory.size - 2] else stateHistory.first()

    /**
     * Contains the entire list of states this call has been through.
     *
     */
    var stateHistory = mutableListOf(TelephonyState.INITIALIZING)

    var codec: Configuration.Codec? = null

    fun isConnected() = telephonyState == TelephonyState.CONNECTED

    enum class TelephonyState {
        INITIALIZING, OUTGOING_CALLING, INCOMING_RINGING, CONNECTED, DISCONNECTED
    }
}