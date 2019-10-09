package com.voipgrid.vialer.voip.core

import com.voipgrid.vialer.voip.core.call.Call

interface VoipProvider {

    fun initialize(configuration: Configuration, listener: CallListener)

    fun destroy()

    suspend fun register()

    suspend fun call(number: String): Call
}