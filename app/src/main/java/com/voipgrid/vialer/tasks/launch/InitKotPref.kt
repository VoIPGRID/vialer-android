package com.voipgrid.vialer.tasks.launch

import androidx.work.Configuration
import androidx.work.WorkManager
import com.chibatching.kotpref.Kotpref
import com.voipgrid.vialer.VialerApplication

class InitKotPref: OnLaunchTask {

    override fun execute(application: VialerApplication) {
        Kotpref.init(application)
        WorkManager.initialize(application, Configuration.Builder().build())
    }

}