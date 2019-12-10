package com.voipgrid.vialer

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.codemybrainsout.ratingdialog.RatingDialog
import com.google.android.material.tabs.TabLayout
import com.voipgrid.vialer.callrecord.CallRecordFragment
import com.voipgrid.vialer.callrecord.CallRecordViewModel
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.persistence.RatingPopup
import com.voipgrid.vialer.persistence.Statistics
import com.voipgrid.vialer.reachability.ReachabilityReceiver
import com.voipgrid.vialer.sip.SipService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class MainActivity : NavigationDrawerActivity() {

    @Inject lateinit var reachabilityReceiver: ReachabilityReceiver

    override val logger = Logger(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VialerApplication.get().component().inject(this)
        setContentView(R.layout.activity_main)
        setActionBar(R.id.action_bar)
        setNavigationDrawer(R.id.drawer_layout)

        if (!userIsOnboarded()) {
            logger.i("User has not properly onboarded, logging them out")
            returnUserToLoginScreen()
            return
        }

        syncUser()

        setupTabs()
        floating_action_button.setOnClickListener { openDialer() }
    }

    override fun onStart() {
        super.onStart()

        showRatingPopupIfDue()
    }

    override fun onResume() {
        super.onResume()
        reachabilityReceiver.startListening()

        // We currently only support a single call so any time this activity is opened, we will
        // request the SipService to display the current call. If there is no current call, this will have no
        // affect.
        SipService.performActionOnSipService(this, SipService.Actions.DISPLAY_CALL_IF_AVAILABLE)
    }

    override fun onPause() {
        super.onPause()
        reachabilityReceiver.stopListening()
    }

    private fun showRatingPopupIfDue() {
        val installedDate = Date(packageManager.getPackageInfo(packageName, 0).firstInstallTime)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)

        // Show app rating popup if app is installed for 7 days or the user has made
        // 3 or more calls
        if (!RatingPopup.shown &&
                (installedDate.before(calendar.time) || Statistics.numberOfCalls >= 3)) {
            val locale = ConfigurationCompat.getLocales(resources.configuration)[0]

            val dialog = RatingDialog.Builder(this)
                    .threshold(3f)
                    .title(getString(R.string.rating_popup_title))
                    .positiveButtonText(
                            getString(R.string.rating_popup_ignore_button).toUpperCase(locale)
                    )
                    .positiveButtonTextColor(R.color.color_primary)
                    .ratingBarColor(R.color.color_primary)
                    .ratingBarBackgroundColor(R.color.rating_star_background)
                    .formTitle(getString(R.string.rating_popup_feedback_title))
                    .formHint(getString(R.string.rating_popup_feedback_hint))
                    .formSubmitText(
                            getString(R.string.rating_popup_feedback_submit_button).toUpperCase(locale)
                    )
                    .formCancelText(getString(R.string.cancel).toUpperCase(locale))
                    .onRatingBarFormSumbit { feedback ->
                        val logger = Logger(MainActivity::class).forceRemoteLogging(true)

                        logger.i("Feedback:\n$feedback")

                        AlertDialog.Builder(this)
                                .setTitle(R.string.rating_popup_post_feedback_title)
                                .setMessage(R.string.rating_popup_post_feedback_message)
                                .setPositiveButton(R.string.rating_popup_post_feedback_done) {
                                    dialog, _ -> dialog.dismiss()
                                }
                                .setNegativeButton(
                                        R.string.rating_popup_post_feedback_write_review
                                ) { _, _ ->
                                    startActivity(Intent(Intent.ACTION_VIEW, PLAYSTORE_URL))
                                }
                                .show()
                    }
                    .build()

            RatingPopup.shown = true
            dialog.show()
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
        return User.isLoggedIn && User.voipgridUser?.mobileNumber != null
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

    private fun setupTabs() {
        tab_layout.apply {
            setTabTextColors(
                    ContextCompat.getColor(this@MainActivity, R.color.tab_inactive),
                    ContextCompat.getColor(this@MainActivity, R.color.tab_active)
            )
            addTab(tab_layout.newTab().setText(R.string.tab_title_recents))
            addTab(tab_layout.newTab().setText(R.string.tab_title_missed))
            setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        (call_record_fragment as CallRecordFragment).changeType(when(it.position) {
                            0 -> CallRecordViewModel.Type.ALL_CALLS
                            1 -> CallRecordViewModel.Type.MISSED_CALLS
                            else -> CallRecordViewModel.Type.ALL_CALLS
                        })
                    }

                }
            })
        }
    }

    companion object {
        private val PLAYSTORE_URL = Uri.parse(
            "https://play.google.com/store/apps/details?id=com.voipgrid.vialer"
        )
    }
}