package com.voipgrid.vialer.call.incoming.alerts

class IncomingCallAlerts(vibration: IncomingCallVibration, ringer: IncomingCallRinger) : IncomingCallAlert {

    val alerts = listOf(vibration, ringer)

    override fun start() {
        alerts.forEach {
            it.start()
        }
    }

    override fun stop() {
        alerts.forEach {
            it.stop()
        }
    }
}