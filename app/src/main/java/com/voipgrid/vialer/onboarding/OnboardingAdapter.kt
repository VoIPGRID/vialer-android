package com.voipgrid.vialer.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.Step

class OnboardingAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, vararg steps: Step) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private var steps = arrayListOf<Step>()

    private val logger = Logger(this)

    init {
        steps.forEach { addStep(it) }
    }

    private fun addStep(step: Step) {
        if (!step.shouldThisStepBeSkipped()) {
            logger.i("Adding ${step.javaClass.simpleName} to onboarder")
            steps.add(step)
        } else {
            logger.i("Not adding ${step.javaClass.simpleName} to onboarder")
        }
    }

    override fun getItemCount(): Int = steps.size

    override fun createFragment(position: Int): Fragment = steps[position]

    fun getStep(position: Int) = createFragment(position) as Step

    fun findCurrentStep(callerStep: Step) = steps.indexOf(callerStep)
}