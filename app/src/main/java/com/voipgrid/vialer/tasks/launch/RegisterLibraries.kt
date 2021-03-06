package com.voipgrid.vialer.tasks.launch

import android.content.Context
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.github.anrwatchdog.ANRWatchDog
import com.github.tamir7.contacts.Contacts
import com.google.gson.GsonBuilder
import com.segment.analytics.Analytics
import com.segment.analytics.Analytics.setSingletonInstance
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication


/**
 * Register/bootstrap all the libraries that require it.
 *
 */
class RegisterLibraries : OnLaunchTask {

    override fun execute(application: VialerApplication) {
        ANRWatchDog().start()
        Contacts.initialize(application)
        initializeSegmentAnalytics(application)
        Kotpref.gson = GsonBuilder().create()
    }

    private fun initializeSegmentAnalytics(context: Context) {
        val analytics = Analytics.Builder(context, context.getString(R.string.segment_write_key)).build()
        setSingletonInstance(analytics)
        Analytics.with(context).track("Application started. Segment analytics implemented successfully!")
    }

}