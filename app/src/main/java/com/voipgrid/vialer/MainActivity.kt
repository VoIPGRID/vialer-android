package com.voipgrid.vialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stepstone.apprating.AppRatingDialog
import com.stepstone.apprating.listener.RatingDialogListener
import com.voipgrid.vialer.api.FeedbackApi
import com.voipgrid.vialer.api.models.Feedback
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.permissions.ContactsPermission
import com.voipgrid.vialer.persistence.RatingPopup
import com.voipgrid.vialer.persistence.Statistics
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.voip.VoIP
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext


class MainActivity : LoginRequiredActivity(), RatingDialogListener {

    private var ratingDialog: AppRatingDialog? = null
    private val feedbackApi: FeedbackApi by inject()
    private val voip: VoIP by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
        setContentView(R.layout.activity_main)

        if (!userIsOnboarded()) {
            logger.i("User has not properly onboarded, logging them out")
            returnUserToLoginScreen()
            return
        }

        syncUser()

        floating_action_button?.setOnClickListener { openDialer() }

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        navView.setupWithNavController(navController)

        if (intent.hasExtra(Extra.NAVIGATE_TO.name)) {
            findNavController(R.id.nav_host_fragment).navigate(intent.getIntExtra(Extra.NAVIGATE_TO.name, 0))
        }

        voip.placeCall("243")
    }

    override fun onResume() {
        super.onResume()

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_contacts).isEnabled = ContactsPermission.hasPermission(this)

        if (shouldAskForRating) {
            askForRating()
        }
    }

    /**
     * Updates the user and voip account information from the api.
     *
     */
    private fun syncUser() = GlobalScope.launch(Dispatchers.Main) {
        if (!isConnectedToNetwork()) return@launch

        userSynchronizer.sync()
    }

    /**
     * End immediately and return to the onboarding screen to let tthe user login
     * again.
     *
     */
    private fun returnUserToLoginScreen() {
        logout.perform(true, this)
        finish()
    }

    /**
     * Check that the user has fully completed the onboarding process. If they have not,
     * we will redirect them to login.
     *
     */
    private fun userIsOnboarded(): Boolean {
        return User.isLoggedIn && User.voipgridUser?.mobileNumber != null && User.internal.hasCompletedOnBoarding
    }

    /**
     * Show the dialer view
     */
    private fun openDialer() {
        startActivity(
                Intent(this, DialerActivity::class.java),
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        floating_action_button,
                        "floating_action_button_transition_name"
                ).toBundle()
        )
    }

    private fun askForRating() {
        ratingDialog = AppRatingDialog.Builder()
                .setPositiveButtonText(R.string.rating_popup_feedback_submit_button)
                .setNegativeButtonText(R.string.rating_popup_ignore_button)
                .setNeutralButtonText(R.string.rating_popup_dont_ask_again_button)
                .setNoteDescriptions(listOf(
                        getString(R.string.rating_popup_below_threshold_text),
                        getString(R.string.rating_popup_below_threshold_text),
                        getString(R.string.rating_popup_above_threshold_text),
                        getString(R.string.rating_popup_above_threshold_text),
                        getString(R.string.rating_popup_above_threshold_text)
                ))
                .setDefaultRating(0)
                .setTitle(getString(R.string.rating_popup_title, getString(R.string.app_name)))
                .setDescription(R.string.rating_popup_post_feedback_title)
                .setCommentInputEnabled(true)
                .setHint(R.string.rating_popup_feedback_hint)
                .setCancelable(false)
                .setCanceledOnTouchOutside(false)
                .create(this@MainActivity)
                .apply { show() }
    }

    enum class Extra {
        NAVIGATE_TO
    }

    override fun onNegativeButtonClicked() {
        Statistics.numberOfCalls = 0
        RatingPopup.shown = false
    }

    override fun onNeutralButtonClicked() {
        RatingPopup.shown = true
    }

    override fun onPositiveButtonClicked(rate: Int, comment: String) {
        if (rate <= 0 && comment.isBlank()) {
            Toast.makeText(this, R.string.settings_feedback_dialog_form_rating_required, Toast.LENGTH_LONG).show()
            return
        }

        RatingPopup.shown = true
        if (comment.isNotBlank()) {
            submitFeedback("Feedback submitted with a $rate star-rating: $comment")
        } else {
            submitFeedback("The app was given a $rate star-rating but the user left no comment.")
        }

        if (rate >= 3) {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                setPackage("com.android.vending")
            })
        }
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

    private val shouldAskForRating get() = !RatingPopup.shown && Statistics.numberOfCalls >= 3
}