package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.os.Handler
import android.view.View
import com.voipgrid.vialer.*
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.OnboardingState
import com.voipgrid.vialer.onboarding.core.Step
import kotlinx.android.synthetic.main.onboarding_missing_voip_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MissingVoipAccountStep : Step() {

    override val layout = R.layout.onboarding_missing_voip_account

    private val logger = Logger(this).forceRemoteLogging(true)

    @Inject lateinit var userSynchronizer: UserSynchronizer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)

        button_continue.setOnClickListener {
            logger.i("User has decided not to set a voip account despite not having one")

            onboarding?.progress(this)
        }

        button_set_voip_account.setOnClickListener {
            logger.i("Loading a web view to let user set their missing voip account")

            onboarding?.let {
                VoIPGRIDPortalWebActivity.launchForChangeUser(context)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        progressIfLinkedPhoneAccountFound()
    }

    /**
     * Perform a request to the api to check if we now have a phone
     * account and automatically progress onboarding.
     *
     */
    private fun progressIfLinkedPhoneAccountFound() = GlobalScope.launch(Dispatchers.Main) {
        userSynchronizer.sync()

        Handler().postDelayed({
            if (User.hasVoipAccount) {
                activity?.runOnUiThread {
                    User.voip.hasEnabledSip = true
                    onboarding?.progress(this@MissingVoipAccountStep)
                }
            }
        }, 1000)
    }

    override fun shouldSkip(state: OnboardingState) = state.hasVoipAccount
}