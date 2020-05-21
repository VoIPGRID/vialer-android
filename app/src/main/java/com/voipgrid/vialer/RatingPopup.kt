package com.voipgrid.vialer

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.codemybrainsout.ratingdialog.RatingDialog
import com.voipgrid.vialer.api.FeedbackApi
import com.voipgrid.vialer.api.models.Feedback
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.persistence.RatingPopup
import com.voipgrid.vialer.persistence.Statistics
import com.voipgrid.vialer.settings.FeedbackDialogFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class RatingPopupListener(val context: AppCompatActivity) : LifecycleObserver, KoinComponent {

    private val feedbackApi: FeedbackApi by inject()
    private val logger = Logger(this)

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun showIfDue() {
        if (shouldShowPopup) {
            showPopup()
        }
    }

    private val shouldShowPopup get() = !RatingPopup.shown && Statistics.numberOfCalls >= 3

    private fun showPopup() {
        val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]

        val dialog = RatingDialog.Builder(context)
                .threshold(3f)
                .title(context.getString(
                    R.string.rating_popup_title,
                    context.getString(R.string.app_name)
                ))
                .positiveButtonText(
                        context.getString(R.string.rating_popup_ignore_button).toUpperCase(locale)
                )
                .positiveButtonTextColor(R.color.color_primary)
                .ratingBarColor(R.color.color_primary)
                .ratingBarBackgroundColor(R.color.rating_star_background)
                .formTitle(context.getString(R.string.rating_popup_feedback_title))
                .formHint(context.getString(R.string.rating_popup_feedback_hint))
                .formSubmitText(
                        context.getString(R.string.rating_popup_feedback_submit_button).toUpperCase(locale)
                )
                .formCancelText(context.getString(R.string.cancel).toUpperCase(locale))
                .onRatingBarFormSumbit { feedback -> submitFeedback(feedback) }
                .onThresholdFailed { ratingDialog, rating, _ ->
                    logger.i("User failed the rating threshold with a $rating star rating")
                    ratingDialog.dismiss()
                    FeedbackDialogFragment(
                            context.getString(R.string.rating_popup_feedback_hint),
                            messagePrefix = "User has left the following feedback after rating the app $rating stars: "
                    ).show(context.supportFragmentManager, "")
                }
                .build()

        RatingPopup.shown = true
        dialog.show()
    }

    /**
     * Submit feedback and properly catch errors where they occur.
     *
     */
    private fun submitFeedback(message: String) = GlobalScope.launch {
        try {
            val response = feedbackApi.submit(Feedback(message))

            if (!response.isSuccessful) {
                logger.e("Unable to submit feedback with code ${response.code()}: $message")
            }

        } catch (e: Exception) {
            logger.e("Failed to submit feedback: $message")
        }
    }


    companion object {
        private val PLAYSTORE_URL = Uri.parse(
                "https://play.google.com/store/apps/details?id=com.voipgrid.vialer"
        )
    }
}