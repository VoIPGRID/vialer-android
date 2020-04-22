package com.voipgrid.vialer.onboarding.steps

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import com.voipgrid.vialer.R
import com.voipgrid.vialer.onboarding.steps.permissions.PasswordChange

class PasswordChangeDialog : PasswordChange() {

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.dialog_fire_missiles)
                    .setPositiveButton(R.string.fire,
                            DialogInterface.OnClickListener { dialog, id ->
                            })
                    .setNegativeButton(R.id.button_continue,
                            DialogInterface.OnClickListener { dialog, id ->
                                // User cancelled the dialog
                            })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
