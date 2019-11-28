package com.voipgrid.vialer.android.calling

interface CallManager {

    /**
     * Place a call via the android call system.
     *
     */
    fun call(number: String)

    /**
     * Make the android call system aware that there is an incoming call.
     *
     */
    fun incomingCall()
}