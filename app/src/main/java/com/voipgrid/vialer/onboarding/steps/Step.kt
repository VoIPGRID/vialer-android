package com.voipgrid.vialer.onboarding.steps

import android.app.AlertDialog
import android.content.ClipDescription
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.IntegerRes
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.OnboardingActivity

abstract class Step: Fragment() {

    protected abstract val layout: Int

    /**
     * Only set to TRUE if you want to let the user leave this
     * step by scrolling right or left.
     *
     */
    open val canManuallyLeaveThisStep = false

    /**
     * Should be set to TRUE if this step doesn't need to be completed in this
     * specific situation.
     *
     */
    open val shouldSkipThisStep = false

    protected var onboarding : OnboardingActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onboarding = activity as OnboardingActivity
    }

    open fun error(title: Int, description: Int) {
        AlertDialog.Builder(onboarding)
                .setTitle(onboarding?.getString(title))
                .setMessage(onboarding?.getString(description))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
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