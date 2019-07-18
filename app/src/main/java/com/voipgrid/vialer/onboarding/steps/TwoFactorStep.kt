package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.view.View
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.VoipgridLogin
import com.voipgrid.vialer.onboarding.core.Step
import com.voipgrid.vialer.util.TwoFactorFragmentHelper
import kotlinx.android.synthetic.main.onboarding_step_two_factor.*
import javax.inject.Inject
import kotlin.math.log

class TwoFactorStep : Step() {

    override val layout = R.layout.onboarding_step_two_factor

    @Inject lateinit var login: VoipgridLogin

    private val twoFactorHelper: TwoFactorFragmentHelper by lazy {
        TwoFactorFragmentHelper(onboarding, two_factor_code_field)
    }

    private val code: String
        get() = two_factor_code_field.text.toString()


    private val logger = Logger(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VialerApplication.get().component().inject(this)
        twoFactorHelper.pasteCodeFromClipboard()

        login.onError = { title: Int, description: Int ->
            logger.w("There was an error logging in with two-factor code $title - $description")
            button_continue.isEnabled = true
            error(title, description)
        }

        login.onLoggedIn = {
            logger.i("User successfully logged in with a two-factor code")
            onboarding?.progress()
        }

        button_continue.setOnClickListener {
            logger.i("User is attempting to login with two-factor code: $code")
            button_continue.isEnabled = false
            onboarding?.isLoading = true
            login.attempt(state.username, state.password, code)
        }
    }

    override fun onResume() {
        super.onResume()
        twoFactorHelper.focusOnTokenField()
        twoFactorHelper.pasteCodeFromClipboard()
    }

    override fun shouldThisStepBeSkipped(): Boolean {
        return isActiveScreen && !state.requiresTwoFactor
    }
}