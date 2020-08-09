package com.voipgrid.vialer.call.incoming.alerts

class IncomingCallAlerts(vibration: IncomingCallVibration, wake: IncomingCallScreenWake) : IncomingCallAlert {

    val alerts = listOf(vibration, wake)

    override fun start() {
        alerts.forEach {
            if (!it.isStarted()) {
                it.start()
            }
        }
    }

    override fun stop() {
        alerts.forEach {
            it.stop()
        }
    }

    override fun isStarted(): Boolean {
        return alerts.filter { !it.isStarted() }.none()
    }
}