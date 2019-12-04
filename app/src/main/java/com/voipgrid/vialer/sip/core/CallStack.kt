package com.voipgrid.vialer.sip.core

import com.voipgrid.vialer.sip.SipCall

class CallStack : ArrayList<SipCall>() {

    val current: SipCall?
        get() = if (isEmpty()) null else last()

    val initial: SipCall?
        get() = if (isEmpty()) null else first()
}