package com.voipgrid.vialer.onboarding.steps

import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.core.AutoContinuingStep

class LogoStep : AutoContinuingStep() {
    override val delay: Int
        get() = onboarding?.resources?.getInteger(R.integer.logo_dismiss_delay_ms) ?: 1000

    override val layout = R.layout.onboarding_step_logo
}