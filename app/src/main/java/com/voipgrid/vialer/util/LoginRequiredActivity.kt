package com.voipgrid.vialer.util

import android.content.Intent
import com.voipgrid.vialer.User
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.logging.VialerBaseActivity
import com.voipgrid.vialer.onboarding.OnboardingActivity
import org.koin.android.ext.android.inject

abstract class LoginRequiredActivity : VialerBaseActivity() {

    override val logger = Logger(this)

    protected val userSynchronizer: UserSynchronizer by inject()

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