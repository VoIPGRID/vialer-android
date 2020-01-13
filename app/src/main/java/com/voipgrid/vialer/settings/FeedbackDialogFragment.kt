package com.voipgrid.vialer.settings

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.Logger
import kotlinx.android.synthetic.main.dialog_feedback.*

class FeedbackDialogFragment : DialogFragment() {

    private val logger = Logger(this)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                    .setView(R.layout.dialog_feedback)
                    .setTitle(R.string.settings_feedback_dialog_title)
                    .setMessage(R.string.settings_feedback_dialog_desc)
                    .setPositiveButton(R.string.settings_feedback_dialog_button_positive) { _, _ ->
                        dialog?.let { dialog ->
                            submitFeedback(dialog.feedback_message.text.toString(), dialog.feedback_subject.text.toString())
                            dialog.feedback_message.text.clear()
                            dialog.feedback_subject.text.clear()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun submitFeedback(subject: String, message: String) {
        logger.i("Received feedback: $subject - $message")
        activity?.let {
            Toast.makeText(it, R.string.settings_feedback_dialog_form_submitted, Toast.LENGTH_LONG).show()
        }
    }
}