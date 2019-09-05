package com.voipgrid.vialer.tasks.launch

import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.github.anrwatchdog.ANRWatchDog
import com.github.tamir7.contacts.Contacts
import com.google.gson.GsonBuilder
import com.voipgrid.vialer.VialerApplication

/**
 * Register/bootstrap all the libraries that require it.
 *
 */
class RegisterLibraries : OnLaunchTask {

    override fun execute(application: VialerApplication) {
        ANRWatchDog().start()
        Contacts.initialize(application)
        Kotpref.gson = GsonBuilder().create()
    }
}