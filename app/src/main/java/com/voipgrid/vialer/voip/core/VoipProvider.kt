package com.voipgrid.vialer.voip.core

import com.voipgrid.vialer.voip.core.call.Call

interface VoipProvider {

    fun initialize(configuration: Configuration, listener: VoipListener)

    fun destroy()

    fun call(number: String): Call

    fun register()

    fun mergeTransfer(first: Call, second: Call)
}