package com.voipgrid.vialer.sip.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.core.Action
import com.voipgrid.vialer.sip.core.SipActionHandler

class ScreenOffReceiver : BroadcastReceiver() {

    private val logger = Logger(this)

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            logger.i("Detected screen off event, disabling call alert")
            SipActionHandler.performActionOnSipService(context, Action.SILENCE)
        }
    }
}