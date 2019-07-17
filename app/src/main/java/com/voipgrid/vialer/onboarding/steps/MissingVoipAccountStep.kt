package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.view.View
import com.voipgrid.vialer.R
import com.voipgrid.vialer.WebActivityHelper
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.PhoneAccountHelper
import kotlinx.android.synthetic.main.onboarding_missing_voip_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MissingVoipAccountStep: Step() {

    override val layout = R.layout.onboarding_missing_voip_account

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        continueButton.setOnClickListener {
            onboarding?.progress()
        }

        setVoipAccountButton.setOnClickListener {
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
                    onboarding?.progress()
                }
            }
        }
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return state.hasVoipAccount
    }
}