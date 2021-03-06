package com.voipgrid.vialer.tasks.launch

import android.content.Context
import androidx.work.*
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.SecureCalling
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter
import java.util.concurrent.TimeUnit

/**
 * Register all periodic tasks that Vialer will execute.
 *
 */
class RegisterPeriodicTasks : OnLaunchTask {

    override fun execute(application: VialerApplication) {
        val workManager = WorkManager.getInstance(application)
        HistoricCallRecordsImporter.Worker.schedule(application)
        scheduleUpdatingVoipAccountParameters(workManager)
    }

    private fun scheduleUpdatingVoipAccountParameters(workManager: WorkManager) {
        val request = PeriodicWorkRequestBuilder<UpdateVoipAccountParametersWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        workManager.enqueueUniquePeriodicWork(UpdateVoipAccountParametersWorker::class.java.name, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

/**
 * Regularly make sure that the voip account's parameters are set correctly.
 *
 */
class UpdateVoipAccountParametersWorker(appContext: Context, workerParameters: WorkerParameters) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        SecureCalling.fromContext(applicationContext).updateApiBasedOnCurrentPreferenceSetting()
        return Result.success()
    }
}