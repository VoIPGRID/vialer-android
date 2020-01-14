package com.voipgrid.vialer.settings

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.logging.Logger
import kotlinx.android.synthetic.main.dialog_feedback.*

class FeedbackDialogFragment : DialogFragment(), TextWatcher {

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

    override fun onResume() {
        super.onResume()
        updatePositiveButtonBasedOnTextFields()
        dialog?.feedback_message?.addTextChangedListener(this)
        dialog?.feedback_subject?.addTextChangedListener(this)
    }

    private fun submitFeedback(subject: String, message: String) {
        logger.i("Received feedback: $subject - $message")
        activity?.let {
            Toast.makeText(it, R.string.settings_feedback_dialog_form_submitted, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * We don't want to let the user submit feedback without filling both fields.
     *
     */
    private fun updatePositiveButtonBasedOnTextFields() {
        dialog.let {
            (it as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = it.feedback_message.text.isNotEmpty() && it.feedback_subject.text.isNotEmpty()
        }
    }

    override fun afterTextChanged(s: Editable?) {
        updatePositiveButtonBasedOnTextFields()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }
}