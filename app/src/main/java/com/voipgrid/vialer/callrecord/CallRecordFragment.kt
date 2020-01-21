package com.voipgrid.vialer.callrecord

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
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
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.NetworkUtil
import kotlinx.android.synthetic.main.fragment_call_records.*
import kotlinx.android.synthetic.main.fragment_call_records.view.*
import kotlinx.android.synthetic.main.view_show_missed_calls_only.*
import kotlinx.android.synthetic.main.view_show_missed_calls_only.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class CallRecordFragment(val type: CallRecordViewModel.Type)
    : Fragment(), SwipeRefreshLayout.OnRefreshListener {

//    val multiCheckListener = MultiCheckedChangeListener()

    private val callRecordViewModel by lazy {
        ViewModelProviders.of(this)[CallRecordViewModel::class.java]
    }

    private val adapter = CallRecordAdapter()
    private lateinit var layout: View

    @Inject
    lateinit var newCallRecordsImporter: NewCallRecordsImporter
    @Inject
    lateinit var networkUtil: NetworkUtil
    @Inject
    lateinit var broadcastReceiverManager: BroadcastReceiverManager

    private val handler = Handler()

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?  {
        layout = inflater.inflate(R.layout.fragment_call_records, null)
        return layout
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        callRecordViewModel.calls.observe(this@CallRecordFragment, Observer<PagedList<CallRecordEntity>> {
            adapter.submitList(it)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout.floating_action_button.setOnClickListener { openDialer() }
        setupTabs()
        adapter.activity = activity
        call_records.adapter = adapter
        setupSwipeContainer()
        setupRecyclerView()
        fetchCalls()
        callRecordViewModel.updateDisplayedCallRecords(show_my_calls_only.isChecked, type)
        show_my_calls_only.setOnCheckedChangeListener { _, _ ->
            handler.post {
                callRecordViewModel.updateDisplayedCallRecords(show_my_calls_only.isChecked, type)
            }
        }
        encryption_disabled_view.setEncryptionView()
    }

    /**
     * Show the dialer view
     */
    private fun openDialer() {
        val fragmentActivity = activity ?: return
        startActivity(
                Intent(context, DialerActivity::class.java),
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        fragmentActivity as Activity,
                        layout.floating_action_button,
                        "floating_action_button_transition_name"
                ).toBundle()
        )
    }

    private fun setupTabs() {
        layout.tab_layout.apply {
            setTabTextColors(
                    ContextCompat.getColor(context, R.color.tab_inactive),
                    ContextCompat.getColor(context, R.color.tab_active)
            )

            // TODO: Remove all !! from code
            val adapter = CallRecordFragmentAdapter(context, activity!!.supportFragmentManager)
            layout.tab_view_pager.adapter = adapter
            layout.tab_view_pager.offscreenPageLimit = adapter.count

            setupWithViewPager(layout.tab_view_pager)

//            addTab(layout.tab_layout.newTab().setText(R.string.tab_title_missed_calls))
//            addTab(layout.tab_layout.newTab().setText(R.string.tab_title_all_calls))
//            setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//                override fun onTabReselected(tab: TabLayout.Tab?) {
//                }
//
//                override fun onTabUnselected(tab: TabLayout.Tab?) {
//                }
//
//                override fun onTabSelected(tab: TabLayout.Tab?) {
//                    tab?.let {
//                        changeType(when(it.position) {
//                            0 -> CallRecordViewModel.Type.MISSED_CALLS
//                            1 -> CallRecordViewModel.Type.ALL_CALLS
//                            else -> CallRecordViewModel.Type.ALL_CALLS
//                        })
//                    }
//                }
//            })

            //show_my_calls_only_switch.setOnCheckedChangeListener(multiCheckListener)
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

    companion object {
        const val REFRESH_TIMEOUT: Long = 5000
    }
}

class AllCallsFragment : CallRecordFragment(CallRecordViewModel.Type.ALL_CALLS)

class MissedCallsFragment : CallRecordFragment(CallRecordViewModel.Type.MISSED_CALLS)

private fun RecyclerView.scrollToTop() {
    smoothScrollToPosition(0)
}