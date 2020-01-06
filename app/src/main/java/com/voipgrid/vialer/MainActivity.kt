package com.voipgrid.vialer

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Switch
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.voipgrid.vialer.callrecord.CallRecordFragmentAdapter
import com.voipgrid.vialer.callrecord.MultiCheckedChangeListener
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.reachability.ReachabilityReceiver
import com.voipgrid.vialer.sip.SipService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : NavigationDrawerActivity() {

    @Inject lateinit var reachabilityReceiver: ReachabilityReceiver

    override val logger = Logger(this)

    val multiCheckListener = MultiCheckedChangeListener()

    val showMyCallsOnlySwitch by lazy {
        findViewById<LinearLayout>(R.id.show_my_calls_only)
        .findViewById<Switch>(R.id.show_my_calls_only_switch)!!
    }

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

        lifecycle.addObserver(RatingPopupListener(this))
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

    private fun setupTabs() {
        tab_layout.apply {
            setTabTextColors(
                    ContextCompat.getColor(this@MainActivity, R.color.tab_inactive),
                    ContextCompat.getColor(this@MainActivity, R.color.tab_active)
            )

            val adapter = CallRecordFragmentAdapter(this@MainActivity, supportFragmentManager)
            tab_view_pager.adapter = adapter
            tab_view_pager.offscreenPageLimit = adapter.count

            showMyCallsOnlySwitch.setOnCheckedChangeListener(multiCheckListener)

            setupWithViewPager(tab_view_pager)
        }
    }
}