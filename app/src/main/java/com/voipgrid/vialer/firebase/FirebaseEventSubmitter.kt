package com.voipgrid.vialer.firebase

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.voipgrid.vialer.logging.Logger
import org.openvoipalliance.phonelib.model.Session

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

    fun userCompletedCall(call: Session) = try {
        firebaseAnalytics.logEvent("call") {
            param("duration", call.duration.toLong())
            param("mos", call.quality.average.toLong())
            param("last_status", call.state.toString())
            param("last_reason", call.reason.toString())
        }
    } catch (e: Exception) {
        logger.e("Unable to submit call event ${e.localizedMessage}")
    }

    fun userLoggedIn() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
        }
    }
}