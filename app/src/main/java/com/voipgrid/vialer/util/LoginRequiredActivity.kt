package com.voipgrid.vialer.util

import android.content.Intent
import com.voipgrid.vialer.User
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.VialerBaseActivity
import com.voipgrid.vialer.onboarding.OnboardingActivity

abstract class LoginRequiredActivity : VialerBaseActivity() {

    override val logger = Logger(this)

    override fun onResume() {
        super.onResume()

        if (!User.isLoggedIn) {
            logger.w("Not logged in anymore! Redirecting to onboarding")
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}