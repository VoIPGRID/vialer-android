package com.voipgrid.vialer.android.calling

/**
 * This class is loaded when we are on a device that does not support the call manager api.
 *
 */
class FakeCallManager : CallManager {

    override fun call(number: String) {

    }

    override fun incomingCall() {

    }
}