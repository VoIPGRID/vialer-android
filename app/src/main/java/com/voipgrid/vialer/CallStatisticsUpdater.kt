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
        callStatisticsTimer?.schedule((MINIMUM_REQUIRED_CALL_LENGTH_SECONDS * 1000).toLong()) {
            Statistics.numberOfCalls++
            callStatisticsTimer?.cancel()
        }
    }

    override fun onCallDisconnected(reason: CallDisconnectedReason?) {
        callStatisticsTimer?.cancel()
    }

    override fun onCallStatusChanged(status: String?, callId: String?) {}

    companion object {
        const val MINIMUM_REQUIRED_CALL_LENGTH_SECONDS = 10
    }

}