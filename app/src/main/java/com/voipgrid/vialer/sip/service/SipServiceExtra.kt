package com.voipgrid.vialer.sip.service

/**
 * The extras (i.e. parameters) that can be passed to the SipService when starting it.
 *
 */
enum class SipServiceExtra {
    OUTGOING_PHONE_NUMBER,
    OUTGOING_CONTACT_NAME,
    INCOMING_TOKEN,
    INCOMING_CALL_START_TIME
}