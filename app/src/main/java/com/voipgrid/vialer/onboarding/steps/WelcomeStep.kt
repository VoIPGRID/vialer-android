package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.view.View
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.onboarding.core.AutoContinuingStep
import kotlinx.android.synthetic.main.onboarding_step_welcome.*

class WelcomeStep : AutoContinuingStep() {
    override val delay = 3000
    override val layout = R.layout.onboarding_step_welcome

    private val user: SystemUser by lazy {
        VialerApplication.get().component().systemUser
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subtitle_label.text = user.fullName
    }
}