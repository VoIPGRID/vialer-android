package com.voipgrid.vialer.onboarding.steps

import com.voipgrid.vialer.R

class LogoStep: AutoContinuingStep() {
    override val delay: Int
        get() = onboarding?.resources?.getInteger(R.integer.logo_dismiss_delay_ms) ?: 1000

    override val layout = R.layout.fragment_logo
}