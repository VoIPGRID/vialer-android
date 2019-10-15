package com.voipgrid.vialer.tasks.launch

import android.util.Log
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.github.anrwatchdog.ANRWatchDog
import com.github.tamir7.contacts.Contacts
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.GsonBuilder
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.koin.voipModule
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
        Kotpref.gson = GsonBuilder().create()

        startKoin {
            androidLogger()
            androidContext(application)
            modules(voipModule)
        }
    }
}