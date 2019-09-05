package com.voipgrid.vialer.tasks.launch

import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter

/**
 * Register all periodic tasks that Vialer will execute.
 *
 */
class RegisterPeriodicTasks : OnLaunchTask {

    override fun execute(application: VialerApplication) {
        HistoricCallRecordsImporter.Worker.schedule(application)
    }
}