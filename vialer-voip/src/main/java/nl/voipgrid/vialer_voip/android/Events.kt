package nl.voipgrid.vialer_voip.android

enum class Events(val extras: Array<Extras> = arrayOf()) {

    /**
     * An outgoing call has completed setup and is ready to be
     * displayed to the user.
     *
     */
    OUTGOING_CALL_HAS_BEEN_SETUP,

    /**
     * An incoming call is ringing.
     *
     */
    INCOMING_CALL_IS_RINGING,
    CALL_STATE_HAS_CHANGED(arrayOf(Extras.CALL_STATE, Extras.PREVIOUS_STATE)),
    VOIP_TIC
}