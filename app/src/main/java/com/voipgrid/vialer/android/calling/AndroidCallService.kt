package com.voipgrid.vialer.android.calling

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.telecom.*
import android.util.Log
import android.widget.Toast
import com.voipgrid.vialer.sip.SipService

typealias OnBoundCallback = (service: SipService) -> Unit

/**
 * This is the service used to interact with the Android calling system.
 *
 */
class AndroidCallService : ConnectionService() {

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        val connection = AndroidCallConnection()

        bind(connection) { it.androidCallManagerIsReadyForOutgoingCall() }

        return connection
    }

    override fun onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?) {
        Toast.makeText(applicationContext, "Outgoing call failed", Toast.LENGTH_LONG).show()
    }

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        return AndroidCallConnection().also { bind(it) }
    }

    override fun onCreateIncomingConnectionFailed(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?) {
        Toast.makeText(applicationContext, "Incoming call failed", Toast.LENGTH_LONG).show()
    }

    private fun bind(androidCallConnection: AndroidCallConnection, onBound: OnBoundCallback? = null) {
        bindService(Intent(applicationContext, SipService::class.java), SipServiceConnection(androidCallConnection, onBound), BIND_AUTO_CREATE)
    }

    private inner class SipServiceConnection(private val androidCallConnection: AndroidCallConnection, private val onBound: OnBoundCallback?) : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val binder = binder as SipService.SipServiceBinder
            binder.service.connection = androidCallConnection
            onBound?.invoke(binder.service)
            unbindService(this)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

}