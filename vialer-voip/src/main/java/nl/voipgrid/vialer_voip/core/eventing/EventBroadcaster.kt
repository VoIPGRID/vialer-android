package nl.voipgrid.vialer_voip.core.eventing

interface EventBroadcaster {

    /**
     * Broadcast an event to the world.
     *
     */
    fun broadcast(event: Event)
}