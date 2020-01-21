package com.voipgrid.vialer.callrecord

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.voipgrid.vialer.R

class CallRecordFragmentAdapter(val context: Context, fragmentManager: FragmentManager)
    : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val items = listOf(
            context.getString(R.string.tab_title_all_calls) to AllCallsFragment(),
            context.getString(R.string.tab_title_missed_calls) to MissedCallsFragment()
    )

    override fun getItem(position: Int) = items[position].second

    override fun getCount() = items.size

    override fun getPageTitle(position: Int) = items[position].first
}