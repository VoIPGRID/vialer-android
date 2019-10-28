package nl.voipgrid.vialer_voip.android.audio

import nl.voipgrid.vialer_voip.core.audio.AudioRouter

internal class AndroidAudioRouter : AudioRouter {
    override fun initialize() {

    }

    override fun destroy() {
    }

    override fun routeVia(route: AudioRouter.Route) {
    }

    override fun currentRoute(): AudioRouter.Route {
        return AudioRouter.Route.BLUETOOTH
    }

    override fun isCurrentlyRoutingVia(route: AudioRouter.Route): Boolean {
        return true
    }

    override fun isRouteAvailable(route: AudioRouter.Route): Boolean {
        return true
    }

    override fun prepareAudioFor(type: AudioRouter.AudioType) {
    }
}