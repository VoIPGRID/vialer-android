package com.voipgrid.vialer.voip.core

import com.voipgrid.vialer.voip.core.call.Call

internal interface VoipProvider {

    fun initialize(configuration: Configuration, listener: VoipListener)

    fun destroy()

    fun call(number: String): Call

    fun register(credentials: Credentials)

    fun mergeTransfer(first: Call, second: Call)
}