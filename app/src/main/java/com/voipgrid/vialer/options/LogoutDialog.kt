package com.voipgrid.vialer.options

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.VialerBaseActivity

class LogoutDialog(private val activity: VialerBaseActivity) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context)
                .setMessage(this.getString(R.string.logout_dialog_text))
                .setPositiveButton(this.getString(R.string.logout_dialog_positive)) { _: DialogInterface?, _: Int ->
                    this.activity.logout(false)
                }
                .setNegativeButton(this.getString(R.string.logout_dialog_negative)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
    }
}