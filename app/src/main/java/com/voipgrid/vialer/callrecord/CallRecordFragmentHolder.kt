package com.voipgrid.vialer.callrecord

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.dialer.DialerActivity
import kotlinx.android.synthetic.main.fragment_call_record_holder.*
import kotlinx.android.synthetic.main.fragment_call_record_holder.view.*

class CallRecordFragmentHolder : Fragment(), MainActivity.MainActivityFragment {

    private lateinit var layout: View

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