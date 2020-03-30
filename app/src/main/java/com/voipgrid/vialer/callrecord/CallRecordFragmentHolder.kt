package com.voipgrid.vialer.callrecord

import android.app.NotificationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.fragment_call_record_holder.view.*
import org.koin.android.ext.android.inject

class CallRecordFragmentHolder : Fragment() {

    private lateinit var layout: View

    private val notificationManager: NotificationManager by inject()

    companion object {
        val multiCheckListener = MultiCheckedChangeListener()
        lateinit var showMyCallsOnlySwitch: Switch
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?  {
        layout = inflater.inflate(R.layout.fragment_call_record_holder, null)
        showMyCallsOnlySwitch = layout.show_my_calls_only
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        notificationManager.cancelAll()
    }

    private fun setupTabs() {
        layout.tab_layout.apply {
            setTabTextColors(
                    ContextCompat.getColor(context, R.color.tab_inactive),
                    ContextCompat.getColor(context, R.color.tab_active)
            )

            val adapter = CallRecordFragmentAdapter(context, childFragmentManager)
            layout.tab_view_pager.adapter = adapter
            layout.tab_view_pager.offscreenPageLimit = adapter.count

            showMyCallsOnlySwitch.setOnCheckedChangeListener(multiCheckListener)

            setupWithViewPager(layout.tab_view_pager)
        }
    }
}