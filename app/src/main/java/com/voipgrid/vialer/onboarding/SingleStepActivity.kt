package com.voipgrid.vialer.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.voipgrid.vialer.onboarding.core.OnboardingState
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.onboarding.steps.MissingVoipAccountStep
import com.voipgrid.vialer.onboarding.steps.TwoFactorStep
import com.voipgrid.vialer.util.AccountHelper
import kotlinx.android.synthetic.main.activity_onboarding.*

/**
 * This activity exists to let us launch any single page from the onboarder
 * as a standalone screen. You must always launch this activity with the extra
 * ONBOARDING_STEP, set to the class name of the step to launch.
 *
 */
class SingleStepActivity: OnboardingActivity() {

    private var step: Step? = null

    override val state: OnboardingState by lazy {
        val accountHelper = AccountHelper(this)

        OnboardingState().apply {
            username = accountHelper.email
            password = accountHelper.password
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(ONBOARDING_STEP)) { "You must provide a single onboarding step to launch this activity" }

        viewPager.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE

        step = when(intent.getStringExtra(ONBOARDING_STEP)) {
            TwoFactorStep::class.java.name -> TwoFactorStep()
            MissingVoipAccountStep::class.java.name -> MissingVoipAccountStep()
            else -> null
        }

        require(step != null) {"The step provided is invalid"}

        supportFragmentManager.beginTransaction().add(fragmentContainer.id, step as Step).commit()
    }

    override fun progress() {
        finish()
    }

    override fun restart() {
        finish()
        startActivity(intent)
    }

    override fun onBackPressed() {
    }

    companion object {
        const val ONBOARDING_STEP = "ONBOARDING_STEP"

        fun launch(context: Context, step: Class<out Step>) {
            context.startActivity(Intent(context, SingleStepActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ONBOARDING_STEP, step.name)
            })
        }
    }
}