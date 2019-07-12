package com.voipgrid.vialer.onboarding

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter: RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    val views = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int {
        return views.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tv
    }

    class ViewHolder(view: RecyclerView) : RecyclerView.ViewHolder(view) {

        val text: TextView

    }
}