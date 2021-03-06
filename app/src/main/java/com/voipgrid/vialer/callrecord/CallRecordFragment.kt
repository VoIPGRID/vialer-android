package com.voipgrid.vialer.callrecord

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.callrecord.database.CallRecordEntity
import com.voipgrid.vialer.callrecord.importing.CallRecordsFetcher
import com.voipgrid.vialer.callrecord.importing.NewCallRecordsImporter
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.NetworkUtil
import kotlinx.android.synthetic.main.fragment_call_records.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class CallRecordFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val callRecordViewModel by lazy {
        activity?.run {
            ViewModelProviders.of(this)[CallRecordViewModel::class.java]
        } ?: throw Exception("No activity found")
    }

    private val adapter = CallRecordAdapter()
    private var type = CallRecordViewModel.Type.ALL_CALLS

    @Inject lateinit var newCallRecordsImporter: NewCallRecordsImporter
    @Inject lateinit var networkUtil: NetworkUtil
    @Inject lateinit var broadcastReceiverManager: BroadcastReceiverManager

    /**
     * Regularly refreshes the data set to keep timestamps relevant.
     *
     */
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            Handler().postDelayed(this, REFRESH_TIMEOUT)
        }
    }

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        callRecordViewModel.calls.observe(this@CallRecordFragment, Observer<PagedList<CallRecordEntity>> {
            adapter.submitList(it)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.activity = activity
        call_records.adapter = adapter
        setupSwipeContainer()
        setupRecyclerView()
        fetchCalls()
        callRecordViewModel.updateDisplayedCallRecords(show_my_calls_only.isChecked, type)
        show_my_calls_only.setOnCheckedChangeListener { _, _ ->
            callRecordViewModel.updateDisplayedCallRecords(show_my_calls_only.isChecked, type)
        }
    }

    override fun onResume() {
        super.onResume()
        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(networkChangeReceiver, ConnectivityManager.CONNECTIVITY_ACTION)
        Handler().postDelayed(runnable, REFRESH_TIMEOUT)
        call_records?.scrollToTop()
    }

    override fun onPause() {
        super.onPause()
        broadcastReceiverManager.unregisterReceiver(networkChangeReceiver)
        Handler().removeCallbacks(runnable)
    }

    private fun setupSwipeContainer() {
        swipe_container?.setColorSchemeColors(resources.getColor(R.color.color_refresh))
        swipe_container?.setOnRefreshListener(this)
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
            swipe_container?.isRefreshing = false
            displayError().noInternetConnection()
            return
        }

        hideError()
        fetchCalls()
    }

    private fun fetchCalls() = lifecycleScope.launch {
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
            swipe_container?.isRefreshing = false
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
     * Change the type of call records being displayed, this is so an external source can control
     * this fragment.
     *
     */
    fun changeType(type: CallRecordViewModel.Type) {
        this.type = type
        callRecordViewModel.updateDisplayedCallRecords(show_my_calls_only.isChecked, type)
        onRefresh()
    }

    companion object {
        const val REFRESH_TIMEOUT: Long = 5000
    }
}

private fun RecyclerView.scrollToTop() {
    smoothScrollToPosition(0)
}
