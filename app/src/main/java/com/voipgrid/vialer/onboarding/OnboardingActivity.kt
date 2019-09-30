package com.voipgrid.vialer.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
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

    @Inject lateinit var logout: Logout

    private lateinit var adapter: OnboardingAdapter

    private val currentStep : Step
        get() = adapter.getStep(viewPager.currentItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)

        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())

        adapter = OnboardingAdapter(supportFragmentManager, lifecycle).apply {
            addStep(LogoStep())
            addStep(LoginStep())
            addStep(TwoFactorStep())
            addStep(MobileNumberStep())
            addStep(MissingVoipAccountStep())
            addStep(ContactsPermissionStep())
            addStep(PhoneStatePermissionStep())
            addStep(MicrophonePermissionStep())
            addStep(OptimizationWhitelistStep())
            addStep(WelcomeStep())
        }

        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
    }

    override fun progress() {
        isLoading = false

        if (isLastItem()) {
            logger.i("Onboarding has been completed, forwarding to the main activity")

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        logger.i("Progressing the onboarder from ${currentStep.javaClass.simpleName}")

        runOnUiThread {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }
    }

    private fun isLastItem(): Boolean {
        return viewPager.currentItem == (adapter.itemCount - 1)
    }

    override fun onBackPressed() {
        logger.i("Back pressed, restarting onboarding")
        restart()
    }

    /**
     * Restart the onboarding process.
     *
     */
    override fun restart() {
        logger.i("Restarting onboarding procedure")
        logout.perform(true, this)
        finish()
    }

    private inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {

        override fun onPageSelected(currentPage: Int) {
            super.onPageSelected(currentPage)
            hideKeyboard()

            viewPager.postDelayed({
                val currentStep = adapter.getStep(currentPage)
                val nextPage = currentPage + 1

                if (currentStep.shouldThisStepBeSkipped()) {
                    logger.i("Skipping ${currentStep.javaClass.simpleName} at {$currentPage} and moving to {$nextPage}")
                    viewPager.setCurrentItem(nextPage, false)
                }
            }, 10)
        }
    }
}