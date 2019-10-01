package com.voipgrid.vialer.onboarding

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.voipgrid.vialer.Logout
import com.voipgrid.vialer.MainActivity
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.onboarding.steps.*
import com.voipgrid.vialer.onboarding.steps.permissions.ContactsPermissionStep
import com.voipgrid.vialer.onboarding.steps.permissions.MicrophonePermissionStep
import com.voipgrid.vialer.onboarding.steps.permissions.OptimizationWhitelistStep
import com.voipgrid.vialer.onboarding.steps.permissions.PhoneStatePermissionStep
import kotlinx.android.synthetic.main.activity_onboarding.*
import javax.inject.Inject

class OnboardingActivity : Onboarder() {

    private val adapter = OnboardingAdapter(supportFragmentManager, lifecycle,
        LogoStep(),
        LoginStep(),
        MobileNumberStep(),
        MissingVoipAccountStep(),
        ContactsPermissionStep(),
        PhoneStatePermissionStep(),
        MicrophonePermissionStep(),
        OptimizationWhitelistStep(),
        WelcomeStep()
    )

    private val currentStep : Step
        get() = adapter.getStep(viewPager.currentItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
        viewPager.apply {
            adapter = this@OnboardingActivity.adapter
            isUserInputEnabled = false
        }
    }

    /**
     * This is called whenever we should be progressing to the next step in onboarding.
     *
     */
    override fun progress(callerStep: Step) {
        isLoading = false

        if (isLastItem()) {
            completeOnboarding()
            return
        }

        if (currentStep != callerStep) {
            logger.i("Caller step (${callerStep::class.java.simpleName}) does not match current step (${currentStep::class.java.simpleName}) so not progressing")
            return
        }

        logger.i("Progressing the onboarder from ${currentStep.javaClass.simpleName}")

        progressViewPager(callerStep)
    }

    /**
     * The view pager will be progressed to the next available step
     * that should not be skipped.
     *
     */
    private fun progressViewPager(callerStep: Step) = runOnUiThread {
        hideKeyboard()

        val currentPosition = adapter.findCurrentStep(callerStep)

        for (i in (currentPosition + 1) until adapter.itemCount) {
            if (adapter.getStep(i).shouldThisStepBeSkipped(state)) {
                continue
            }

            viewPager.setCurrentItem(i, false)

            return@runOnUiThread
        }

        throw Exception("There is no onboarding step left to progress to")
    }

    /**
     * Perform the actions we need to do now that the user has completed onboarding,
     * this involves launching the MainActivity.
     *
     */
    private fun completeOnboarding() {
        logger.i("Onboarding has been completed, forwarding to the main activity")
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun isLastItem(): Boolean {
        return viewPager.currentItem == (adapter.itemCount - 1)
    }

    override fun onBackPressed() {
        logger.i("Back pressed, restarting onboarding")
        when (viewPager.currentItem == 0) {
            true -> super.onBackPressed()
            false -> restart()
        }
    }

    /**
     * Restart the onboarding process.
     *
     */
    override fun restart() {
        logger.i("Restarting onboarding procedure")
        finish()
        logout(true)
    }
}