package com.voipgrid.vialer.sip.service

import android.bluetooth.BluetoothDevice
import android.os.Build
import android.telecom.CallAudioState
import com.voipgrid.vialer.android.calling.AndroidCallConnection

class Audio(private val connection: AndroidCallConnection) {

    var route: Route
        get() = Route.values().associateBy(Route::androidRoute)[connection.callAudioState.route] ?: throw Exception("Invalid route")
        set(route) = connection.setAudioRoute(route.androidRoute)

    val isBluetoothRouteAvailable: Boolean
        get() = connection.callAudioState != null && connection.callAudioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH == CallAudioState.ROUTE_BLUETOOTH

    val activeBluetoothDevice: BluetoothDevice?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) connection.callAudioState.activeBluetoothDevice else null

    enum class Route(val androidRoute: Int) {
        EARPIECE(CallAudioState.ROUTE_EARPIECE),
        BLUETOOTH(CallAudioState.ROUTE_BLUETOOTH),
        WIRED_HEADSET(CallAudioState.ROUTE_WIRED_HEADSET),
        SPEAKER(CallAudioState.ROUTE_SPEAKER),
        WIRED_OR_EARPIECE(CallAudioState.ROUTE_WIRED_OR_EARPIECE)
    }
}