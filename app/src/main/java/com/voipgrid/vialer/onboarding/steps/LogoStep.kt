package com.voipgrid.vialer.onboarding.steps

import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.core.AutoContinuingStep
import com.voipgrid.vialer.onboarding.core.OnboardingState

class LogoStep : AutoContinuingStep() {
    override val delay: Int
        get() = onboarding?.resources?.getInteger(R.integer.logo_dismiss_delay_ms) ?: 1000

    override val layout = R.layout.onboarding_step_logo

    override fun onResume() {
        super.onResume()
        shown = true
    }

    override fun shouldSkip(state: OnboardingState) = !shown

    companion object {

        /**
         * We only want to display the logo when the user is actually booting the app,
         * not every time onboarding is restarted.
         *
         */
        var shown = false
    }
}