package com.voipgrid.vialer.voip

import android.content.Context
import android.content.Intent
import android.util.Log
import com.voipgrid.vialer.call.NewIncomingCallActivity
import com.voipgrid.vialer.call.incoming.alerts.IncomingCallAlerts
import com.voipgrid.vialer.calling.IncomingCallActivity
import com.voipgrid.vialer.notifications.call.AbstractCallNotification
import com.voipgrid.vialer.voip.core.call.Call

class IncomingCallHandler(private val incomingCallAlerts: IncomingCallAlerts, private val context: Context) {

    fun handle(call: Call, notification: AbstractCallNotification) {
//        incomingCallAlerts.start()
//        notification.incoming(call.metaData.number ?: "", call.metaData.callerId ?: "")
        Log.e("TEST123", "launching activity!")
        context.startActivity(Intent(context, NewIncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}