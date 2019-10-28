package nl.voipgrid.vialer_voip.core.audio

interface AudioRouter {

    fun initialize()

    fun destroy()

    fun routeVia(route: Route)

    fun currentRoute(): Route

    fun isCurrentlyRoutingVia(route: Route): Boolean

    fun isRouteAvailable(route: Route): Boolean

    fun prepareAudioFor(type: AudioType)

    enum class Route {
        BLUETOOTH, SPEAKER, HEASDSET, EARPIECE, INVALID
    }

    enum class AudioType {
        RINGER, CALL
    }
}