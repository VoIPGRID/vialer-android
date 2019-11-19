package com.voipgrid.vialer.sip

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import com.voipgrid.vialer.util.PhoneNumberUtils

class TelecomService : ConnectionService() {

    override fun onCreateOutgoingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {

        val connection = VialerConnection()

        val intent = Intent(applicationContext, SipService::class.java).apply {
            action = SipService.Actions.HANDLE_OUTGOING_CALL
            putExtra(SipConstants.EXTRA_PHONE_NUMBER, "244")
            putExtra(SipConstants.EXTRA_CONTACT_NAME, "Hello World")
            data = SipUri.sipAddressUri(applicationContext, PhoneNumberUtils.format("244"))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        connection.setActive()

        bind(connection) {}

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
        val connection = VialerConnection()
        bind(connection) {}
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
            connectionManagerPhoneAccount: PhoneAccountHandle,
            request: ConnectionRequest) {
    }

    private fun bind(vialerConnection: VialerConnection, onBound: (service: SipService) -> Unit) {
        bindService(Intent(applicationContext, SipService::class.java), SipServiceConnection(vialerConnection, onBound), BIND_AUTO_CREATE)
    }

    private inner class SipServiceConnection(private val vialerConnection: VialerConnection, private val onBound: (service: SipService) -> Unit) : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val binder = binder as SipService.SipServiceBinder
            binder.service.connection = vialerConnection
            onBound.invoke(binder.service)
            unbindService(this)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

}