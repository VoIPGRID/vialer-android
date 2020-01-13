package com.voipgrid.vialer

import com.voipgrid.vialer.calling.CallStatusReceiver
import com.voipgrid.vialer.persistence.Statistics
import com.voipgrid.vialer.sip.CallDisconnectedReason
import java.util.*
import kotlin.concurrent.schedule

class CallStatisticsUpdater : CallStatusReceiver.Listener {
    private var callStatisticsTimer: Timer? = null

    override fun onCallConnected() {
        callStatisticsTimer = Timer()
        callStatisticsTimer?.schedule(20000) {
            Statistics.numberOfCalls++
            callStatisticsTimer?.cancel()
        }
    }

    override fun onCallDisconnected(reason: CallDisconnectedReason?) {
        callStatisticsTimer?.cancel()
    }

    override fun onCallStatusChanged(status: String?, callId: String?) {}

    override fun onCallRingingIn() {}

    override fun onServiceStopped() {}

    override fun onCallHold() {}

    override fun onCallUnhold() {}

    override fun onCallRingingOut() {}

}