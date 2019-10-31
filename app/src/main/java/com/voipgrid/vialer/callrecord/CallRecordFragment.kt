package com.voipgrid.vialer.callrecord

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.CallRecordFragment.TYPE.ALL_CALLS
import com.voipgrid.vialer.callrecord.CallRecordFragment.TYPE.MISSED_CALLS
import com.voipgrid.vialer.callrecord.database.CallRecordDao
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import com.voipgrid.vialer.callrecord.importing.CallRecordsFetcher
import com.voipgrid.vialer.callrecord.importing.HistoricCallRecordsImporter
import com.voipgrid.vialer.callrecord.importing.NewCallRecordsImporter
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.NetworkUtil
import kotlinx.android.synthetic.main.fragment_call_records.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class CallRecordFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private var liveData: LiveData<PagedList<CallRecordEntity>>? = null
    private val adapter = CallRecordAdapter()
    private var type = ALL_CALLS

    @Inject lateinit var newCallRecordsImporter: NewCallRecordsImporter
    @Inject lateinit var historicCallRecordsImporter: HistoricCallRecordsImporter
    @Inject lateinit var networkUtil: NetworkUtil
    @Inject lateinit var broadcastReceiverManager: BroadcastReceiverManager
    @Inject lateinit var db: CallRecordDao

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            onRefresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_call_records, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.activity = activity
        call_records.adapter = adapter
        setupSwipeContainer()
        setupRecyclerView()
        findNewCalls()
        queryDatabaseBasedOnCurrentType()
        show_my_calls_only.setOnCheckedChangeListener { _, _ -> onRefresh() }
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(networkChangeReceiver, ConnectivityManager.CONNECTIVITY_ACTION)
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiverManager.unregisterReceiver(networkChangeReceiver)
    }

    private fun setupSwipeContainer() {
        swipe_container.setColorSchemeColors(resources.getColor(R.color.color_refresh))
        swipe_container.setOnRefreshListener(this)
    }

    private fun setupRecyclerView() {
        val mLayoutManager = LinearLayoutManager(VialerApplication.get())
        activity?.let {
            call_records.apply {
                addItemDecoration(DividerItemDecoration(it, LinearLayoutManager.VERTICAL))
                layoutManager = mLayoutManager
                itemAnimator = DefaultItemAnimator()
            }
        }
    }

    /**
     * Refresh the data being shown by attempting to query the api again,
     * the data displayed will then be automatically based on what is in the database.
     *
     */
    override fun onRefresh() {
        if (!networkUtil.isOnline) {
            swipe_container.isRefreshing = false
            displayError().noInternetConnection()
            return
        }

        hideError()
        queryDatabaseBasedOnCurrentType()
        findNewCalls()
    }

    /**
     * Perform a request to find any new records from the api and import them into the
     * database.
     *
     */
    private fun findNewCalls() = GlobalScope.launch(Dispatchers.Main) {
        call_records.scrollToTop()
        try {
            newCallRecordsImporter.import()
            hideError()
            call_records.apply {
                scrollToTop()
                delay(200)
                scrollToTop()
            }
        } catch (e: CallRecordsFetcher.PermissionDeniedException) {
            if (e.wasRequestForPersonalCalls) {
                displayError().permissionsFailed()
            }
        } catch (e: Exception) {}
        finally {
            swipe_container.isRefreshing = false
        }
    }

    /**
     * Display an error, the error to display can be chained on from this method.
     *
     * i.e. displayError().permissionsFailed()
     *
     */
    private fun displayError(): CallRecordsUnavailableView {
        call_records.visibility = View.GONE
        return call_records_unavailable_view
    }

    /**
     * Hide the error, making the call records visible again.
     *
     */
    private fun hideError() {
        call_records.visibility = View.VISIBLE
        call_records_unavailable_view.visibility = View.GONE
    }

    /**
     * Perform the database query so our data is matching the type of call records we should
     * be displaying.
     *
     */
    private fun queryDatabaseBasedOnCurrentType() {
        val personal = when(show_my_calls_only.isChecked) {
            true  -> booleanArrayOf(true)
            false -> booleanArrayOf(false, true)
        }

        liveData?.removeObservers(this)

        when(type) {
            ALL_CALLS -> db.callRecordsByDate(wasMissed = booleanArrayOf(false, true), wasPersonal = personal)
            MISSED_CALLS -> db.callRecordsByDate(wasMissed = booleanArrayOf(true), wasPersonal = personal)
        }.toLiveData(pageSize = 50).also { liveData = it }.observe(this, Observer<PagedList<CallRecordEntity>> {
            adapter.submitList(it)
        })
    }

    /**
     * Change the type of call records being displayed, this is so an external source can control
     * this fragment.
     *
     */
    fun changeType(type: TYPE) {
        this.type = type
        onRefresh()
    }

    /**
     * The type of call records to show.
     *
     */
    enum class TYPE {
        ALL_CALLS, MISSED_CALLS
    }
}

private fun RecyclerView.scrollToTop() {
    smoothScrollToPosition(0)
}
