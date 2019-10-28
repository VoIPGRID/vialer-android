package nl.voipgrid.vialer_voip.core

import nl.voipgrid.vialer_voip.core.call.Call

interface VoipDriver {

    /**
     * Initialize the voip library, this must perform all tasks necessary
     * for an account to be able to register.
     *
     */
    fun initialize(configuration: Configuration, listener: VoipListener)

    /**
     * Register with the given credentials, allowing for calls to be made/received.
     *
     */
    fun register(credentials: Credentials)

    /**
     * Place a call to the given number and return the correctly setup call
     * object.
     *
     */
    fun call(number: String): Call

    /**
     * Connect the two given calls, this is for use in an attended transfer.
     *
     */
    fun connectCalls(a: Call, b: Call)

    /**
     * Destroy the voip library, clean up everything that is required.
     *
     */
    fun destroy()
}