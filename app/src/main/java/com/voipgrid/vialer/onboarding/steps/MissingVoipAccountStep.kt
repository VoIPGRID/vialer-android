package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.view.View
import com.voipgrid.vialer.Preferences
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.WebActivityHelper
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.PhoneAccountHelper
import kotlinx.android.synthetic.main.onboarding_missing_voip_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MissingVoipAccountStep : Step() {

    @Inject lateinit var preferences: Preferences

    override val layout = R.layout.onboarding_missing_voip_account

    private val logger = Logger(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)

        continueButton.setOnClickListener {
            logger.i("User has decided not to set a voip account despite not having one")

            onboarding?.progress()
        }

        setVoipAccountButton.setOnClickListener {
            logger.i("Loading a web view to let user set their missing voip account")

            onboarding?.let {
                WebActivityHelper(onboarding).startWebActivity(
                        getString(R.string.user_change_title),
                        getString(R.string.web_user_change),
                        getString(R.string.analytics_user_change)
                )
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
    private fun progressIfLinkedPhoneAccountFound() {
        GlobalScope.launch {
            PhoneAccountHelper(onboarding).linkedPhoneAccount?.let {
                launch(Dispatchers.Main) {
                    preferences.setSipEnabled(true)
                    onboarding?.progress()
                }
            }
        }
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return isActiveScreen && state.hasVoipAccount
    }
}