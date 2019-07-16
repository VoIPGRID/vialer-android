package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.util.Log
import android.view.View
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.onboarding.VoipgridLogin
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.TwoFactorFragmentHelper
import kotlinx.android.synthetic.main.onboarding_step_two_factor.*
import javax.inject.Inject

class TwoFactorStep: Step() {

    override val layout = R.layout.onboarding_step_two_factor

    @Inject lateinit var login: VoipgridLogin

    private val twoFactorHelper: TwoFactorFragmentHelper by lazy {
        TwoFactorFragmentHelper(onboarding, two_factor_code_field)
    }

    private val code: String
        get() = two_factor_code_field.text.toString()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)
        twoFactorHelper.pasteCodeFromClipboard()

        login.onError = { title: Int, description: Int ->
            button_continue.isEnabled = true
            error(title, description)
        }

        login.onLoggedIn = { onboarding?.progress() }

        button_continue.setOnClickListener {
            button_continue.isEnabled = false
            onboarding?.code = code
            login.attempt(onboarding?.username ?: "", onboarding?.password ?: "", onboarding?.code ?: "")
        }
    }

    override fun onResume() {
        super.onResume()
        twoFactorHelper.focusOnTokenField()
        twoFactorHelper.pasteCodeFromClipboard()
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return !(onboarding?.requiresTwoFactor ?: true)
    }
}