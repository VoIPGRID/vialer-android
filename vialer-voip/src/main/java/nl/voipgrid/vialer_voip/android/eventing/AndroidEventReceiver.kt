package nl.voipgrid.vialer_voip.android.eventing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.voipgrid.vialer_voip.core.eventing.*

abstract class AndroidEventReceiver(private val eventConverter: EventConverter) : BroadcastReceiver() {

    private val events = listOf(CallStateDidChange::javaClass, IncomingCallStartedRinging::javaClass, OutgoingCallWasSetup::javaClass)

    /**
     * Listen for an event from voip, if received then call the relevant abstract method.
     *
     */
    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action ?: return
        val payload = intent.getStringExtra("payload") ?: return

        try {
            onEvent(eventConverter.convertToEvent(action, payload))
        } catch (e: IllegalArgumentException) {}
    }

    /**
     * An event of the given type was received.
     *
     */
    abstract fun onEvent(event: Event)
}