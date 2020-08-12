package com.voipgrid.vialer.call.ui.buttons

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.telecom.CallAudioState
import android.util.AttributeSet
import com.voipgrid.vialer.R
import com.voipgrid.voip.android.Connection

class AudioRouteButton(context: Context, attrs: AttributeSet) : CallActionButton(context, attrs) {

    fun update(route: Int, supportedRoutes: Int, activeBluetoothDevice: BluetoothDevice?) : String {
        if (!isBluetoothRouteAvailable(supportedRoutes)) {
            setImageDrawable(context.getDrawable(R.drawable.ic_speaker))
            activate(route == CallAudioState.ROUTE_SPEAKER)
            return context.getString(R.string.audio_source_option_speaker)
        }

        setImageDrawable(context.getDrawable(when (route) {
            CallAudioState.ROUTE_SPEAKER -> R.drawable.ic_speaker
            CallAudioState.ROUTE_BLUETOOTH -> R.drawable.ic_bluetooth
            else -> R.drawable.ic_phone
        }))

        return when (route) {
            CallAudioState.ROUTE_SPEAKER -> context.getString(R.string.audio_source_option_speaker)
            CallAudioState.ROUTE_BLUETOOTH -> if (activeBluetoothDevice != null) activeBluetoothDevice.name else context.getString(R.string.audio_source_option_bluetooth)
            else -> context.getString(R.string.audio_source_option_phone)
        }
    }

    fun handleClick(connection: Connection, activity: Activity) {
        if (!isBluetoothRouteAvailable(connection.callAudioState.supportedRouteMask)) {
            if (connection.callAudioState.route == CallAudioState.ROUTE_SPEAKER) {
                connection.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            } else {
                connection.setAudioRoute(CallAudioState.ROUTE_WIRED_OR_EARPIECE)
            }
            return
        }

        val activeBluetoothDevice = connection.callAudioState.activeBluetoothDevice

        val options = listOf(
                context.getString(R.string.audio_source_option_phone),
                context.getString(R.string.audio_source_option_speaker),
                if (activeBluetoothDevice != null) activeBluetoothDevice.name else context.getString(R.string.audio_source_option_bluetooth)
        )

        AlertDialog.Builder(activity)
                .setItems(options.toTypedArray()) { _: DialogInterface, which: Int ->
                    when (which) {
                        0 -> connection.setAudioRoute(CallAudioState.ROUTE_WIRED_OR_EARPIECE)
                        1 -> connection.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                        else -> connection.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
                    }
                }
                .show()
    }

    private fun isBluetoothRouteAvailable(supportedRoutes: Int) =
        supportedRoutes and CallAudioState.ROUTE_BLUETOOTH == CallAudioState.ROUTE_BLUETOOTH
}