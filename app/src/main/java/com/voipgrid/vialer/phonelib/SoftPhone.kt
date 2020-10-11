package com.voipgrid.vialer.phonelib

import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.AttendedTransferSession
import org.openvoipalliance.phonelib.model.Session

class SoftPhone(public val phone: PhoneLib) {

    var call: Session? = null

    var transferSession: AttendedTransferSession? = null

    val hasCall: Boolean
        get() = call != null || transferSession != null

    val isOnTransfer: Boolean
        get() = transferSession != null

    fun cleanUp() {
        call = null
        transferSession = null
    }
}