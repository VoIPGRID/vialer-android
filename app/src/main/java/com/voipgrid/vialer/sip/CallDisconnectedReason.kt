package com.voipgrid.vialer.sip

import org.openvoipalliance.phonelib.model.Reason


enum class CallDisconnectedReason {
    NONE,
    NUMBER_NOT_FOUND;

    companion object {
        @JvmStatic
        fun fromReason(reason: Reason) =when(reason) {
                Reason.ADDRESS_INCOMPLETE -> NUMBER_NOT_FOUND
                else -> NONE
        }
    }
}