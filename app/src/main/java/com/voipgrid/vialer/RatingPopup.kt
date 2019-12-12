package com.voipgrid.vialer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.codemybrainsout.ratingdialog.RatingDialog
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.persistence.RatingPopup
import com.voipgrid.vialer.persistence.Statistics
import java.util.*

class RatingPopupListener(val context: Context) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun showIfDue() {
        if (shouldShowPopup) {
            showPopup()
        }
    }

    private val shouldShowPopup get(): Boolean {
        val installedDate = Date(
            context.packageManager
            .getPackageInfo(context.packageName, 0)
            .firstInstallTime
        )

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)

        return (!RatingPopup.shown &&
                (installedDate.before(calendar.time) || Statistics.numberOfCalls >= 3))
    }

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
                .onRatingBarFormSumbit { feedback ->
                    val logger = Logger(MainActivity::class).forceRemoteLogging(true)

                    logger.i("Feedback:\n$feedback")

                    AlertDialog.Builder(context)
                            .setTitle(R.string.rating_popup_post_feedback_title)
                            .setMessage(R.string.rating_popup_post_feedback_message)
                            .setPositiveButton(R.string.rating_popup_post_feedback_done) {
                                dialog, _ -> dialog.dismiss()
                            }
                            .setNegativeButton(
                                    R.string.rating_popup_post_feedback_write_review
                            ) { _, _ ->
                                context.startActivity(Intent( Intent.ACTION_VIEW, PLAYSTORE_URL))
                            }
                            .show()
                }
                .build()

        RatingPopup.shown = true
        dialog.show()
    }


    companion object {
        private val PLAYSTORE_URL = Uri.parse(
                "https://play.google.com/store/apps/details?id=com.voipgrid.vialer"
        )
    }
}