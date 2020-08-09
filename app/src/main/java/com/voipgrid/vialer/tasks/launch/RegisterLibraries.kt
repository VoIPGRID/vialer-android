package com.voipgrid.vialer.tasks.launch

import android.content.Context
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.github.anrwatchdog.ANRWatchDog
import com.github.tamir7.contacts.Contacts
import com.google.gson.GsonBuilder
import com.segment.analytics.Analytics
import com.segment.analytics.Analytics.setSingletonInstance
import com.voipgrid.voip.di.voipModules
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.koin.appModule
import com.voipgrid.voip.Config
import com.voipgrid.voip.SoftPhone
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin


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

        startKoin {
            androidLogger()
            androidContext(application)
            modules(listOf(appModule) + voipModules)
        }

        User.voipAccount?.let {
            SoftPhone.config = Config(
                    it.accountId,
                    it.password,
                    "sipproxy.voipgrid.nl",
                    "5060"
            )
        }

    }

    private fun initializeSegmentAnalytics(context: Context) {
        val analytics = Analytics.Builder(context, context.getString(R.string.segment_write_key)).build()
        setSingletonInstance(analytics)
        Analytics.with(context).track("Application started. Segment analytics implemented successfully!")
    }

}