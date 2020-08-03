package com.voipgrid.vialer.firebase

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipCall

object FirebaseEventSubmitter {

    private val logger = Logger(this)

    private val firebaseAnalytics by lazy {
        Firebase.analytics
    }

    fun userDidRateApp(stars: Int) {
        firebaseAnalytics.logEvent("rating") {
            param("stars", stars.toLong())
        }

        logger.i("Logged event rating with stars $stars")
    }

    fun userCompletedCall(call: SipCall) = try {
        firebaseAnalytics.logEvent("call") {
            param("duration", call.callDuration.toLong())
            if (call.hasCalculatedMos()) {
                param("mos", call.mos)
            }
            param("codec", call.codec)
            param("last_status", call.info.lastStatusCode.toString())
            param("last_reason", call.info.lastReason)
            param("direction", call.callDirection)
            param("transport", call.transport)
        }
    } catch (e: Exception) {
        logger.e("Unable to submit call event ${e.localizedMessage}")
    }

    fun userLoggedIn() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
        }
    }
}