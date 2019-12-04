package com.voipgrid.vialer.sip.core

enum class Action {

    /**
     * An action that should be received when first creating the SipService
     * when there is an incoming call expected.
     *
     */
    HANDLE_INCOMING_CALL,

    /**
     * An action that should be received when first creating the SipService
     * when there is an outgoing call being started.
     *
     */
    HANDLE_OUTGOING_CALL,

    /**
     * An action that should be received when there is already an active
     * call, this allows something like a notification to decline a call.
     *
     */
    DECLINE_INCOMING_CALL,

    /**
     * An action that should be received when there is already an active
     * call, this allows something like a notification to answer a call.
     *
     */
    ANSWER_INCOMING_CALL,

    /**
     * End an already in progress call.
     *
     */
    END_CALL,

    /**
     * Cause the SipService to create a call activity for the current call, if there is no call
     * this action will have no affect.
     *
     */
    DISPLAY_CALL_IF_AVAILABLE,

    SILENCE
}