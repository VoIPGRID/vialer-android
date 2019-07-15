package com.voipgrid.vialer.onboarding

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.steps.Step
import kotlin.reflect.KClass

class OnboardingAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle) {

    private var steps = arrayListOf<Step>()

    fun addStep(step: Step) {
        steps.add(step)
    }

    override fun getItemCount(): Int {
        return steps.size
    }

    override fun createFragment(position: Int): Fragment {
        return steps[position]
    }

    fun getStep(position: Int): Step {
        return createFragment(position) as Step
    }
}