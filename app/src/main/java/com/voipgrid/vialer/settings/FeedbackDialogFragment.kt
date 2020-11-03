package com.voipgrid.vialer.settings

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication
import com.voipgrid.vialer.api.FeedbackApi
import com.voipgrid.vialer.api.models.Feedback
import com.voipgrid.vialer.logging.Logger
import kotlinx.android.synthetic.main.dialog_feedback.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FeedbackDialogFragment(private val placeholderMessage: String? = null, private val messagePrefix: String = "") : DialogFragment(), TextWatcher, KoinComponent {

    private val logger = Logger(this)

    private val feedbackApi: FeedbackApi by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                    .setView(R.layout.dialog_feedback)
                    .setTitle(R.string.settings_feedback_dialog_title)
                    .setPositiveButton(R.string.settings_feedback_dialog_button_positive) { _, _ ->
                        dialog?.let { dialog ->
                            submitFeedback(dialog.feedback_message.text.toString())
                            dialog.feedback_message.text.clear()
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
        dialog?.feedback_message?.hint = placeholderMessage ?: getString(R.string.settings_feedback_dialog_desc)
    }

    private fun submitFeedback(message: String) = GlobalScope.launch(Dispatchers.Main) {
        logger.i("Received feedback: $message")

        var successful = try {
            val response = feedbackApi.submit(Feedback(messagePrefix + message))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }

        Toast.makeText(VialerApplication.get(), if (successful) R.string.settings_feedback_dialog_form_submitted else R.string.settings_feedback_dialog_form_submitted_failed, Toast.LENGTH_LONG).show()
    }

    /**
     * We don't want to let the user submit feedback without filling both fields.
     *
     */
    private fun updatePositiveButtonBasedOnTextFields() {
        dialog.let {
            (it as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = it.feedback_message.text.isNotEmpty()
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