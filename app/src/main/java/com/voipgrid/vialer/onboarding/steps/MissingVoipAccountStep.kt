package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.util.Log
import android.view.View
import com.voipgrid.vialer.R
import com.voipgrid.vialer.WebActivityHelper
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.PhoneAccountHelper
import kotlinx.android.synthetic.main.onboarding_missing_voip_account.*

class MissingVoipAccountStep: Step() {

    override val layout = R.layout.onboarding_missing_voip_account

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.e("TEST123", "onbaording is null? " + (onboarding == null))
        continueButton.setOnClickListener {
            onboarding?.progress()
        }

        Log.e("TEST123", "onbaording is null? " + (onboarding == null))
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

        Log.e("TEST123", "onbaording is null? " + (onboarding == null))
        if (onboarding?.hasVoipAccount == true) {
            return
        }

        onboarding?.let {
            Thread {
                if (PhoneAccountHelper(onboarding).linkedPhoneAccount != null) {
                    onboarding?.progress()
                }
            }.start()
        }
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        Log.e("TEST123", "onbaording is null? " + (onboarding == null))
        return (onboarding?.hasVoipAccount ?: false)
    }
}