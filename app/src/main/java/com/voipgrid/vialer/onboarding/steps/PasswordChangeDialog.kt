package com.voipgrid.vialer.onboarding.steps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.api.PasswordChange
import kotlinx.android.synthetic.main.activity_create_new_password.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

typealias OnSuccessCallback = ((newPassword: String) -> Unit)?

class PasswordChangeDialog(private val email: String, private val currentPassword: String) : DialogFragment() {

    private val passwordChange: PasswordChange by inject()

    var onSuccess: OnSuccessCallback = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_create_new_password, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateContinueButton()

        arrayOf(password_field_1, password_field_2).forEach {
            it.addTextChangedListener(afterTextChanged = {
                updateContinueButton()
            })
        }

        button_continue.setOnClickListener { passwordChangeWasClicked() }
    }

    private fun passwordChangeWasClicked() = GlobalScope.launch(Dispatchers.Main) {
        val newPassword = password_field_1.text.toString()
        val result = passwordChange.perform(email, currentPassword, newPassword)

        when (result) {
            PasswordChange.Result.SUCCESS -> onSuccess?.invoke(newPassword)
            PasswordChange.Result.FAIL -> Toast.makeText(activity, activity?.getString(R.string.password_change_failure_toast), Toast.LENGTH_LONG).show()
        }
    }

    private fun updateContinueButton() {
        if (password_field_1.text.isNullOrBlank()) {
            button_continue.isEnabled = false
            return
        }

        button_continue.isEnabled = password_field_1.text.toString() == password_field_2.text.toString()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
