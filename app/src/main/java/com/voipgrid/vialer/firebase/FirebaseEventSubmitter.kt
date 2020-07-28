package com.voipgrid.vialer.firebase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.voipgrid.vialer.logging.Logger

class FirebaseEventSubmitter {

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
}