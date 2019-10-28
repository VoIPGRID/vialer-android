package nl.voipgrid.vialer_voip.core.eventing

import nl.voipgrid.vialer_voip.core.call.State

sealed class Event

data class CallStateDidChange(val state: State.TelephonyState, val previousState: State.TelephonyState) : Event()

object IncomingCallStartedRinging : Event()

object OutgoingCallWasSetup : Event()