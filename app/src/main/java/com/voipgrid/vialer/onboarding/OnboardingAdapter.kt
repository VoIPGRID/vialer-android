package com.voipgrid.vialer.onboarding

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.voipgrid.vialer.onboarding.core.Step

class OnboardingAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        vararg val steps: Step
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = steps.size

    override fun createFragment(position: Int) = steps[position]

    fun getStep(position: Int) = createFragment(position)

    fun indexOfStep(callerStep: Step) = steps.indexOfFirst { callerStep isSameAs it }
}