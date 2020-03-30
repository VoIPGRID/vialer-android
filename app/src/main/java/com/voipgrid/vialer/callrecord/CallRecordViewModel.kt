package com.voipgrid.vialer.callrecord

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.CallRecordViewModel.Type.ALL_CALLS
import com.voipgrid.vialer.callrecord.CallRecordViewModel.Type.MISSED_CALLS
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import javax.inject.Inject


class CallRecordViewModel(private val db: CallRecordDao) : ViewModel() {

    val calls: LiveData<PagedList<CallRecordEntity>>
    val missedCalls: LiveData<PagedList<CallRecordEntity>>

    private val filterLiveData = MutableLiveData<CallRecordsQuery>()

    init {
        calls = Transformations.switchMap(filterLiveData) {
            v -> db.callRecordsByDate(v.wasPersonal).toLiveData(15)
        }

        missedCalls = Transformations.switchMap(filterLiveData) {
            v -> db.missedCallRecordsByDate(v.wasPersonal).toLiveData(15)
        }
    }

    /**
     * Change the query that we are using to fetch the calls from the database.
     *
     */
    fun updateDisplayedCallRecords(showMyCallsOnly: Boolean) {
        filterLiveData.value = CallRecordsQuery(
                wasPersonal = when (showMyCallsOnly) {
                    true  -> booleanArrayOf(true)
                    false -> booleanArrayOf(false, true)
                }
        )
    }

    /**
     * The type of call records to show.
     *
     */
    enum class Type {
        ALL_CALLS, MISSED_CALLS
    }

    private class CallRecordsQuery(val wasPersonal: BooleanArray)
}