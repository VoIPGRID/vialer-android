package com.voipgrid.vialer.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.middleware.Middleware
import org.koin.core.KoinComponent
import org.koin.core.inject

class OnBootReceiver : BroadcastReceiver(), KoinComponent {

    private val middleware: Middleware by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val logger = Logger(OnBootReceiver::class.java)
        logger.e("onBootReceiver")
        middleware.register()
    }
}