package com.voipgrid.vialer.callrecord.importing

import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.callrecord.database.CallRecordsInserter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

abstract class CallRecordsImporter(private val fetcher: CallRecordsFetcher, private val inserter: CallRecordsInserter, api: VoipgridApi) {

    /**
     * The api types we will query, mapping the api to whether or not these are personal calls. This is simply
     * a way to avoid duplicating code for the different apis we need to query.
     *
     */
    protected val types = mapOf(
            api::getRecentCallsForLoggedInUser to true,
            api::getRecentCalls to false
    )

    /**
     * Fetch all records provided by the api call and insert them into the database.
     *
     */
    protected suspend fun fetchAndInsert(call: CallRecordApiCall, personal: Boolean, from: DateTime, to: DateTime) = withContext(Dispatchers.IO) {
        fetcher.fetch(call, from, to).forEach { record ->
            inserter.insert(record, personal)
        }
    }
    
    companion object {
        val TIMEZONE: DateTimeZone = DateTimeZone.forID("Europe/Amsterdam")
    }
}