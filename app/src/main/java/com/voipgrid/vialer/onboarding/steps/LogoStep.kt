package com.voipgrid.vialer.onboarding.steps

import android.os.Handler
import com.voipgrid.vialer.R

class LogoStep: Step() {

    override val layout = R.layout.fragment_logo

    private val delay : Int
        get() = onboarding?.resources?.getInteger(R.integer.logo_dismiss_delay_ms) ?: 1000

    override fun onResume() {
        super.onResume()

        Handler().postDelayed({ onboarding?.progress() }, delay.toLong())
    }
}