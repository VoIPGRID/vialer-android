package com.voipgrid.vialer.onboarding.core

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.onboarding.Onboarder

abstract class Step: Fragment() {

    protected abstract val layout: Int

    protected val onboarding : Onboarder?
        get() = activity as Onboarder?

    protected val voipgridApi: VoipgridApi
        get() = ServiceGenerator.createApiService(onboarding)

    protected val state: OnboardingState
        get() = onboarding?.state ?: OnboardingState()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)
    }

    protected val isActiveScreen: Boolean
        get() = onboarding != null

    /**
     * Return TRUE to skip this step, this is checked twice, when first
     * being added to the onboarder and also just before the screen is due
     * to be shown.
     *
     */
    open fun shouldThisStepBeSkipped(): Boolean {
        return false
    }

    /**
     * Display an alert message.
     *
     */
    open fun alert(title: Int, description: Int) {
        AlertDialog.Builder(onboarding)
                .setTitle(onboarding?.getString(title))
                .setMessage(onboarding?.getString(description))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
    }

    /**
     * Display an error message.
     *
     */
    open fun error(title: Int, description: Int) {
        onboarding?.isLoading = false
        alert(title, description)
    }
}