package com.voipgrid.vialer.callrecord

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
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
import com.voipgrid.vialer.MainActivity
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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import javax.inject.Inject

abstract class CallRecordFragment(val type: CallRecordViewModel.Type)
    : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val callRecordViewModel: CallRecordViewModel by sharedViewModel()

    private val adapter = CallRecordAdapter()

    @Inject lateinit var newCallRecordsImporter: NewCallRecordsImporter
    @Inject lateinit var networkUtil: NetworkUtil
    private val broadcastReceiverManager: BroadcastReceiverManager by inject()
    @Inject lateinit var cachedContacts: CachedContacts

    private val handler = Handler()

    private val multiCheckListener by lazy {
        CallRecordFragmentHolder.multiCheckListener
    }

    private val showMyCallsOnlySwitch by lazy {
        CallRecordFragmentHolder.showMyCallsOnlySwitch
    }

    private val checkListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        handler.post {
            callRecordViewModel.updateDisplayedCallRecords(isChecked)
        }
    }

    /**
     * Regularly refreshes the data set to keep timestamps relevant.
     *
     */
    private val runnable = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            handler.postDelayed(this, REFRESH_TIMEOUT)
        }
    }

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
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

        when(type) {
            CallRecordViewModel.Type.ALL_CALLS -> callRecordViewModel.calls
            CallRecordViewModel.Type.MISSED_CALLS -> callRecordViewModel.missedCalls
        }.observe(this@CallRecordFragment, Observer<PagedList<CallRecordEntity>> {
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

        broadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(
            networkChangeReceiver,
            ConnectivityManager.CONNECTIVITY_ACTION
        )

        callRecordViewModel.updateDisplayedCallRecords(showMyCallsOnlySwitch.isChecked)
        multiCheckListener.register(checkListener)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, REFRESH_TIMEOUT)
    }

    override fun onPause() {
        super.onPause()
        call_records.scrollToPosition(0)
        handler.removeCallbacks(runnable)
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

    override fun onDestroy() {
        super.onDestroy()
        multiCheckListener.unregister(checkListener)
        broadcastReceiverManager.unregisterReceiver(networkChangeReceiver)
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

        cachedContacts.clear()
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
        call_records?.visibility = View.VISIBLE
        call_records_unavailable_view?.visibility = View.GONE
    }


    companion object {
        const val REFRESH_TIMEOUT: Long = 5000
    }
}

class AllCallsFragment : CallRecordFragment(CallRecordViewModel.Type.ALL_CALLS)

class MissedCallsFragment : CallRecordFragment(CallRecordViewModel.Type.MISSED_CALLS)

class MultiCheckedChangeListener : CompoundButton.OnCheckedChangeListener {
    private val listeners = mutableListOf<CompoundButton.OnCheckedChangeListener>()

    fun register(listener: CompoundButton.OnCheckedChangeListener) = listeners.add(listener)

    fun unregister(listener: CompoundButton.OnCheckedChangeListener) = listeners.remove(listener)

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        listeners.forEach { it.onCheckedChanged(buttonView, isChecked) }
    }
}

private fun RecyclerView.scrollToTop() {
    smoothScrollToPosition(0)
}
