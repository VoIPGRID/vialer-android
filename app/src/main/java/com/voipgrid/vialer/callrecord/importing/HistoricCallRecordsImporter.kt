package com.voipgrid.vialer.callrecord.importing

import android.content.Context
import androidx.work.*
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.callrecord.database.CallRecordsInserter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HistoricCallRecordsImporter(fetcher: CallRecordsFetcher, inserter: CallRecordsInserter, api: VoipgridApi) : CallRecordsImporter(fetcher, inserter, api) {

    /**
     * Import all historic call records into the database, this includes all months that we can find calls for.
     *
     */
    suspend fun import() = withContext(Dispatchers.IO) {
        relevantMonths().toList().reversed().forEach { date ->
            try {
                types.forEach { type ->
                    fetchAndInsert(type.key, type.value, date, toEndOfMonth(date))
                }
                User.internal.callRecordMonthsImported.add(date)
                delay(30000)
            } catch (e: Exception) {
                delay(60000)
            }
        }
    }

    private fun toEndOfMonth(date: DateTime) : DateTime = date.dayOfMonth().withMaximumValue().withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59)

    /**
     * We will attempt to find the months that need to be queried.
     *
     */
    private fun relevantMonths() = sequence {
        var start = DateTime(EARLIER_YEAR, EARLIEST_MONTH, 1, 0, 0)

        while (start.isBeforeNow) {
            if (requiresQuerying(start)) {
                yield(start)
            }
            start = start.plusMonths(1)
        }
    }

    /**
     * Check if we have already successfully imported all the records for this month, if we have
     * we will skip it.
     *
     */
    private fun requiresQuerying(date: DateTime): Boolean {
        val current = DateTime()

        if (date.year == current.year && current.monthOfYear == date.monthOfYear) return true

        return !User.internal.callRecordMonthsImported.contains(date)
    }

    companion object {
        const val EARLIEST_MONTH = 1
        const val EARLIER_YEAR = 2015
    }

    /**
     * Allows historic call records to be imported using Android's WorkManager API.
     *
     */
    class Worker(appContext: Context, workerParameters: WorkerParameters) : CoroutineWorker(appContext, workerParameters) {

        @Inject lateinit var historicCallRecordsImporter: HistoricCallRecordsImporter

        override suspend fun doWork(): Result {
            VialerApplication.get().component().inject(this)
            historicCallRecordsImporter.import()
            return Result.success()
        }

        companion object {
            private val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .setRequiresBatteryNotLow(true)
                    .build()

            private val oneTime = OneTimeWorkRequestBuilder<Worker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                    .build()

            private val periodic = PeriodicWorkRequestBuilder<Worker>(3, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

            /**
             * Begin working to import historic call records immediately.
             *
             */
            fun start(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork(Worker::class.java.name, ExistingWorkPolicy.KEEP, oneTime)

            /**
             * Schedule the call records to be imported at regular intervals.
             *
             */
            fun schedule(context: Context) = WorkManager.getInstance(context).enqueueUniquePeriodicWork(Worker::class.java.name, ExistingPeriodicWorkPolicy.KEEP, periodic)
        }
    }
}

