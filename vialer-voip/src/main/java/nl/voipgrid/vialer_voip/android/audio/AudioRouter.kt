package nl.voipgrid.vialer_voip.android.audio

interface AudioRouter {

    fun initialize()

    fun destroy()

    fun routeVia(route: Route)

    fun currentRoute(): Route

    fun isCurrentlyRoutingVia(route: Route): Boolean

    fun isRouteAvailable(route: Route): Boolean

    fun focus()

    enum class Route {
        BLUETOOTH, SPEAKER, HEASDSET, EARPIECE, INVALID
    }
}