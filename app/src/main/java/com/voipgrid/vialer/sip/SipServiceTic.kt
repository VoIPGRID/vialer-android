package com.voipgrid.vialer.sip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SipServiceTic(val listener: TicListener) {

    var running = true

    fun begin() = GlobalScope.launch(Dispatchers.Main) {
        while(running) {
            delay(TIC_RATE)
            listener.onTic()
        }
    }

    fun stop() {
        running = false
    }

    companion object {
        const val TIC_RATE: Long = 500
    }

    interface TicListener {
        fun onTic()
    }
}