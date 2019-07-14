package com.voipgrid.vialer.onboarding.steps

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntegerRes
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.OnboardingActivity

abstract class Step: Fragment() {

    protected abstract val layout: Int

    /**
     * Only set to TRUE if you want to let the user leave this
     * step by scrolling right or left.
     *
     */
    open val canManuallyLeaveThisStep = false

    /**
     * Should be set to TRUE if this step doesn't need to be completed in this
     * specific situation.
     *
     */
    open val shouldSkipThisStep = false

    protected var onboarding : OnboardingActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onboarding = activity as OnboardingActivity
    }
}