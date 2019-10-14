package com.voipgrid.vialer.callrecord.importing

import android.database.sqlite.SQLiteConstraintException
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.callrecord.CallRecordFragment
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import com.voipgrid.vialer.callrecord.database.CallRecordsInserter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.lang.Exception

class NewCallRecordsImporter(fetcher: CallRecordsFetcher, inserter: CallRecordsInserter, api: VoipgridApi, private val db: CallRecordDao) : CallRecordsImporter(fetcher, inserter, api) {

    private val defaultStartDate : DateTime
        get() = DateTime(TIMEZONE).withDayOfMonth(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0)

    /**
     * Import all new call records into the database.
     *
     */
    suspend fun import() = withContext(Dispatchers.IO) {
        types.forEach {
            try {
                fetchAndInsert(it.key, it.value, findStartDate(it.value), DateTime(TIMEZONE))
            } catch (e: SQLiteConstraintException) { }
        }
    }

    /**
     * Attempts to find the most recent call record and use that date so we query the minimal amount required. If the most recent call record
     * is not within this month, we will just pick the entire month's worth.
     *
     */
    private fun findStartDate(personal: Boolean): DateTime {
        val mostRecentCallRecordDate = db.getMostRecentCallRecord(personal)?.let { DateTime(it.callTime) } ?: return defaultStartDate

        return if (mostRecentCallRecordDate.isAfter(defaultStartDate)) mostRecentCallRecordDate else defaultStartDate
    }
}