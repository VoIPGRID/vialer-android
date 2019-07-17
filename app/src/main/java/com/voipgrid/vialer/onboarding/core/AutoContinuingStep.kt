package com.voipgrid.vialer.onboarding.core

import android.os.Handler

abstract class AutoContinuingStep : Step() {

    abstract val delay: Int

    override fun onResume() {
        super.onResume()

        Handler().postDelayed({ onboarding?.progress() }, delay.toLong())
    }
}