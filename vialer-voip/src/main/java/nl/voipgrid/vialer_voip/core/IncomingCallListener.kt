package nl.voipgrid.vialer_voip.core

import nl.voipgrid.vialer_voip.core.call.Call
import nl.voipgrid.vialer_voip.core.call.State

interface VoipListener {

    /**
     * The provider is notifying of an incoming voip call.
     *
     */
    fun onIncomingCallFromVoipProvider(call: Call)

    /**
     * The state of this call has changed.
     *
     */
    fun onCallStateUpdate(call: Call, state: State)

    /**
     * Called when the voip provider has successfully registered with the
     * host.
     *
     */
    fun onVoipProviderRegistered()
}