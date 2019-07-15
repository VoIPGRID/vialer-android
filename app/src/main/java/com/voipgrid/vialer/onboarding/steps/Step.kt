package com.voipgrid.vialer.onboarding.steps

import android.app.AlertDialog
import android.content.ClipDescription
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.IntegerRes
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.VoipgridApi
import com.voipgrid.vialer.onboarding.OnboardingActivity

abstract class Step: Fragment() {

    protected abstract val layout: Int

    /**
     * Only set to TRUE if you want to let the user leave this
     * step by scrolling right or left.
     *
     */
    open val canManuallyLeaveThisStep = false

    protected var onboarding : OnboardingActivity? = null

    protected val voipgridApi: VoipgridApi
        get() = ServiceGenerator.createApiService(onboarding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)
    }

    open fun shouldThisStepBeSkipped(): Boolean {
        return false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onboarding = activity as OnboardingActivity
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
        alert(title, description)
    }
}

fun EditText.onTextChanged(callback: (Editable?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            callback(p0)
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
    })
}