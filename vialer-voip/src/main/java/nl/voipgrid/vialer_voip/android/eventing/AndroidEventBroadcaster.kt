package nl.voipgrid.vialer_voip.android.eventing

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.voipgrid.vialer_voip.core.eventing.*

class AndroidEventBroadcaster(private val broadcastManager: LocalBroadcastManager, private val eventConverter: EventConverter) : EventBroadcaster {

    /**
     * Broadcast an event so it can be received by the rest of the system.
     *
     */
    override fun broadcast(event: Event) {
        val (name, payload) = eventConverter.convertFromEvent(event)

        broadcastManager.sendBroadcast(Intent(name).apply {
            putExtra("payload", payload)
        })
    }
}