package com.voipgrid.vialer.sip.service

import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.sip.outgoing.OutgoingCallRinger
import com.voipgrid.vialer.sip.utils.BusyTone
import org.koin.android.ext.android.inject

class Sounds(val busyTone: BusyTone, val outgoingCallRinger: OutgoingCallRinger, val incomingCallAlerts: IncomingCallAlerts) {

    /**
     * Silence all sounds.
     *
     */
    fun silence() {
        outgoingCallRinger.stop()
        incomingCallAlerts.stop()
    }

}