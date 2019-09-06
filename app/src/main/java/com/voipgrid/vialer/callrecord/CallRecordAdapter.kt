package com.voipgrid.vialer.callrecord

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.voipgrid.vialer.R
import com.voipgrid.vialer.callrecord.database.CallRecordEntity

class CallRecordAdapter : PagedListAdapter<CallRecordEntity, CallRecordViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallRecordViewHolder =
            CallRecordViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_call_record, parent, false))

    var activity: Activity? = null

    override fun onBindViewHolder(holder: CallRecordViewHolder, position: Int) {
        getItem(position)?.let {
            activity?.let { activity -> holder.setActivity(activity) }
            holder.bindTo(it)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CallRecordEntity>() {
            override fun areItemsTheSame(oldConcert: CallRecordEntity,
                                         newConcert: CallRecordEntity) = oldConcert.id == newConcert.id

            override fun areContentsTheSame(oldConcert: CallRecordEntity,
                                            newConcert: CallRecordEntity) = oldConcert == newConcert
        }
    }
}