package com.voipgrid.vialer.sip

import org.pjsip.pjsua2.pjsip_status_code

enum class CallDisconnectedReason {
    NONE,
    NUMBER_NOT_FOUND;

    companion object {
        @JvmStatic
        fun fromStatusCode(code: pjsip_status_code?): CallDisconnectedReason {
            return when(code) {
                pjsip_status_code.PJSIP_SC_NOT_FOUND -> NUMBER_NOT_FOUND
                else -> NONE
            }
        }
    }
}