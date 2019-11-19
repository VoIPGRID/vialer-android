package com.voipgrid.vialer.sip

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class TelecomService : ConnectionService() {

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {

        val connection = PjsipConnection()

        bind(connection) {
            Log.e("TEST123", "Making calll...")
            it.makeCall("244", "aaa")
            connection.setActive()
        }

        return connection
    }

    override fun onCreateOutgoingConnectionFailed(
            connectionManagerPhoneAccount: PhoneAccountHandle,
            request: ConnectionRequest) {
        Log.e("TEST123", "onCreateOutgoingConnectionFailed")
    }

    override fun onCreateIncomingConnection(
            connectionManagerPhoneAccount: PhoneAccountHandle,
            request: ConnectionRequest): Connection {
        return PjsipConnection()
    }

    override fun onCreateIncomingConnectionFailed(
            connectionManagerPhoneAccount: PhoneAccountHandle,
            request: ConnectionRequest) {
    }

    private fun bind(pjsipConnection: PjsipConnection, onBound: (service: SipService) -> Unit) {
        bindService(Intent(applicationContext, SipService::class.java), SipServiceConnection(pjsipConnection, onBound), 0)
    }

    private inner class SipServiceConnection(private val pjsipConnection: PjsipConnection, private val onBound: (service: SipService) -> Unit) : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val binder = binder as SipService.SipServiceBinder
            binder.service.setConnection(pjsipConnection)
            onBound.invoke(binder.service)
            unbindService(this)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

}