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
import com.voipgrid.vialer.R
import com.voipgrid.vialer.dialer.DialerActivity
import kotlinx.android.synthetic.main.fragment_call_record_holder.*
import kotlinx.android.synthetic.main.fragment_call_record_holder.view.*

class CallRecordFragmentHolder : Fragment() {

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
        layout.floating_action_button.setOnClickListener { openDialer() }
        setupTabs()
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

            val adapter = CallRecordFragmentAdapter(context, childFragmentManager)
            layout.tab_view_pager.adapter = adapter
            layout.tab_view_pager.offscreenPageLimit = adapter.count

            showMyCallsOnlySwitch.setOnCheckedChangeListener(multiCheckListener)

            setupWithViewPager(layout.tab_view_pager)
        }
    }
}