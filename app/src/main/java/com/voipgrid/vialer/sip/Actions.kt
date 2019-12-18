package com.voipgrid.vialer.sip

import com.voipgrid.vialer.android.calling.AndroidCallConnection

class Actions(private val sip: SipService, private val connection: AndroidCallConnection) {

    fun hold() {
        connection.onHold()
    }

    fun unhold() {
        connection.onUnhold()
    }

    fun reject() {
        connection.onReject()
    }

    fun disconnect() {
        connection.onDisconnect()
    }

    fun answer() {
        connection.onAnswer()
    }
}