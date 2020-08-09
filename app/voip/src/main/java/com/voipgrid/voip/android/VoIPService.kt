package com.voipgrid.voip.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telecom.*
import android.telecom.Connection
import android.telecom.Connection.*
import android.telecom.TelecomManager.PRESENTATION_ALLOWED
import android.util.Log
import android.widget.Toast
import com.voipgrid.voip.SoftPhone
import org.koin.core.KoinComponent
import org.koin.core.inject

internal class VoIPService: ConnectionService(), KoinComponent {

    private val softPhone: SoftPhone by inject()
    var connection: com.voipgrid.voip.android.Connection? = null

    private val defaultConnection: com.voipgrid.voip.android.Connection
        get() = run {
            com.voipgrid.voip.android.Connection().apply {
                connectionProperties = PROPERTY_SELF_MANAGED
                connectionCapabilities = CAPABILITY_HOLD and CAPABILITY_SUPPORT_HOLD and CAPABILITY_MUTE
            }
        }

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        Log.e("TEST123", "onCraeateOUtgias ${request.address}")

        val connection = defaultConnection.apply {
            setCallerDisplayName("Display Name", PRESENTATION_ALLOWED)
            setVideoState(request.videoState)
        }

        this.connection = connection

        startActivity(Intent(this, Class.forName("com.voipgrid.vialer.call.ui.CallActivity")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })

        return connection
    }

    override fun onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest) {
        Log.e("TEST123", "onCreateOutgoingConnectionFailed")
        Toast.makeText(this, "Unable to make outgoing call", Toast.LENGTH_LONG).show()
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        Log.e("TEST123", "onCreateIncomingConnection")
        val connection = defaultConnection.apply {
            setCallerDisplayName("Display Name", PRESENTATION_ALLOWED)
            setVideoState(request.videoState)
        }

        this.connection = connection

        return connection
    }

    override fun onCreateIncomingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest) {
        Log.e("TEST123", "onCreateIncomingConnectionFailed")
        Toast.makeText(this, "Missed call", Toast.LENGTH_LONG).show()
    }
}