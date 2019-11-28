package com.voipgrid.vialer.callrecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.CallRecordViewModel.Type.ALL_CALLS
import com.voipgrid.vialer.callrecord.CallRecordViewModel.Type.MISSED_CALLS
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import javax.inject.Inject


class CallRecordViewModel(application: Application) : AndroidViewModel(application) {

    @Inject lateinit var db: CallRecordDao

    val calls: LiveData<PagedList<CallRecordEntity>>

    private val filterLiveData = MutableLiveData<CallRecordsQuery>()

    init {
        VialerApplication.get().component().inject(this)

        calls = Transformations.switchMap(filterLiveData) {
            v -> db.callRecordsByDate(v.wasPersonal, v.wasMissed).toLiveData(50)
        }
    }

    /**
     * Change the query that we are using to fetch the calls from the database.
     *
     */
    fun updateDisplayedCallRecords(showMyCallsOnly: Boolean, type: Type) {
        filterLiveData.value = CallRecordsQuery(
                when (type) {
                    ALL_CALLS -> booleanArrayOf(false, true)
                    MISSED_CALLS -> booleanArrayOf(true)
                },
                when (showMyCallsOnly) {
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

    private class CallRecordsQuery(val wasMissed: BooleanArray, val wasPersonal: BooleanArray)
}