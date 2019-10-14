package com.voipgrid.vialer.call.incoming.alerts

interface IncomingCallAlert {

    fun start()

    fun stop()

    fun isStarted(): Boolean
}