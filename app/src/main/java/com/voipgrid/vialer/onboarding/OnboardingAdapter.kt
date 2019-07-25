package com.voipgrid.vialer.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.Step

class OnboardingAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private var steps = arrayListOf<Step>()

    private val logger = Logger(this)

    fun addStep(step: Step) {
        if (!step.shouldThisStepBeSkipped()) {
            logger.i("Adding ${step.javaClass.simpleName} to onboarder")
            steps.add(step)
        } else {
            logger.i("Not adding ${step.javaClass.simpleName} to onboarder")
        }
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