package com.voipgrid.voip.android

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.telecom.*
import android.telecom.Connection
import android.telecom.Connection.*
import android.telecom.TelecomManager.PRESENTATION_ALLOWED
import android.util.Log
import android.widget.Toast
import com.voipgrid.voip.SoftPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

internal class VoIPService: ConnectionService(), KoinComponent {

    private val softPhone: SoftPhone by inject()

    private val defaultConnection: com.voipgrid.voip.android.Connection
        get() = run {
            com.voipgrid.voip.android.Connection().apply {
                connectionProperties = PROPERTY_SELF_MANAGED
                connectionCapabilities = CAPABILITY_HOLD and CAPABILITY_SUPPORT_HOLD and CAPABILITY_MUTE
            }
        }

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        val connection = defaultConnection.apply {
            setCallerDisplayName("Display Name", PRESENTATION_ALLOWED)
            setVideoState(request.videoState)
        }

        val intent = Intent(this, Class.forName("com.voipgrid.vialer.call.ui.CallActivity")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        GlobalScope.launch(Dispatchers.Main) {
            softPhone.call(request.address.schemeSpecificPart).apply {
                this.connection = connection
            }

            this@VoIPService.startActivity(intent)
        }

        return connection
    }

    override fun onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest) {
        Log.e("TEST123", "onCreateOutgoingConnectionFailed")
        Toast.makeText(this, "Unable to make outgoing call", Toast.LENGTH_LONG).show()
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        Log.e("TEST123", "onCreateIncomingConnection")
        val connection = defaultConnection.apply {
            setCallerDisplayName("Display Name", PRESENTATION_ALLOWED)
            setVideoState(request.videoState)
        }

        softPhone.call?.connection = connection

        return connection
    }

    override fun onCreateIncomingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest) {
        Log.e("TEST123", "onCreateIncomingConnectionFailed")
        Toast.makeText(this, "Missed call", Toast.LENGTH_LONG).show()
    }
}