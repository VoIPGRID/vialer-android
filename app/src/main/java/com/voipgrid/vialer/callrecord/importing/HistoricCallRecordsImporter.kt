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
                delay(DELAY_BETWEEN_EACH_MONTH)
            } catch (e: Exception) {
                delay(DELAY_IF_RATE_LIMIT_IS_HIT)
            }
        }
    }

    private fun toEndOfMonth(date: DateTime) : DateTime = date.dayOfMonth().withMaximumValue().withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59)

    /**
     * We will attempt to find the months that need to be queried.
     *
     */
    private fun relevantMonths() = sequence {
        var start = EARLIEST_DATE

        while (start.isBeforeNow) {
            if (requiresQuerying(start)) yield(start)

            start = start.plusMonths(1)
        }
    }

    /**
     * Check if we have already successfully imported all the records for this month, if we have
     * we will skip it.
     *
     * We will always check the current month, and the previous month, otherwise we only check months
     * that haven't yet been imported
     *
     */
    private fun requiresQuerying(date: DateTime): Boolean {
        val current = DateTime()

        if (date.isAfter(current.minusMonths(RECENT_MONTHS_TO_ALWAYS_IMPORT))) return true

        return !User.internal.callRecordMonthsImported.contains(date)
    }

    companion object {

        /**
         * After each month we will wait a bit to prevent issues with rate limiting.
         */
        const val DELAY_BETWEEN_EACH_MONTH : Long = 10 * 1000

        /**
         * If we ever hit a rate limit, we will wait this long before continuing.
         */
        const val DELAY_IF_RATE_LIMIT_IS_HIT : Long = 5 * 60 * 1000

        /**
         * The earliest date that will be imported, we will not try and find call records before
         * this date.
         *
         */
         val EARLIEST_DATE = DateTime(2015, 1, 1, 0, 0)

        /**
         * We will always query all call records from this many months ago.
         *
         */
        const val RECENT_MONTHS_TO_ALWAYS_IMPORT = 3
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

