package com.voipgrid.vialer.callrecord.importing

import android.util.Log
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.models.CallRecord
import com.voipgrid.vialer.api.models.VoipGridResponse
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import okhttp3.Request
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import retrofit2.Call
import retrofit2.Response

typealias CallRecordApiCall = (Int, Int, String, String) -> Call<VoipGridResponse<CallRecord>>

class CallRecordsFetcher {

    /**
     * Fetch an iterable collection of call records, however the api calls will be chunked.
     *
     */
    suspend fun fetch(apiCall: CallRecordApiCall, from: DateTime, to: DateTime) = sequence {
        var page = 0
        var hasMoreRecords = false
        var request : Request

        do {
            apiCall.invoke(CHUNK_SIZE, page * CHUNK_SIZE, format(from), format(to))
                    .also { request = it.request() }
                    .execute()
                    .also {
                        if (!it.isSuccessful) {
                            if (it.code() == 403) {
                                throw PermissionDeniedException(request.url.toString().contains("personalized"))
                            }
                            throw Exception("Failed to load records")
                        }
                    }
                    .body()
                    ?.also { hasMoreRecords = it.meta?.next?.isNotBlank() ?: false }
                    ?.objects?.forEach { yield(it) }
            page++
        } while(hasMoreRecords && User.isLoggedIn)
    }

    private fun format(dateTime: DateTime): String = DateTimeFormat.forPattern(CallRecordEntity.DATE_PATERRN).print(dateTime)

    companion object {

        /**
         * The amount of records we query and process from the api per request.
         */
        const val CHUNK_SIZE = 500
    }

    class PermissionDeniedException(val wasRequestForPersonalCalls: Boolean) : Exception()
}