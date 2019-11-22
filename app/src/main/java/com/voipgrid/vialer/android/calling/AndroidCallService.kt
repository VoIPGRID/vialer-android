package com.voipgrid.vialer.android.calling

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.widget.Toast
import com.voipgrid.vialer.sip.SipService

/**
 * This is the service used to interact with the Android calling system.
 *
 */
class AndroidCallService : ConnectionService() {

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        return SipService.connection.apply {
            voip.androidCallManagerIsReadyForOutgoingCall()
        }
    }

    override fun onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?) {
        Toast.makeText(applicationContext, "Outgoing call failed", Toast.LENGTH_LONG).show()
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        return SipService.connection
    }

    override fun onCreateIncomingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?) {
        Toast.makeText(applicationContext, "Incoming call failed", Toast.LENGTH_LONG).show()
    }
}