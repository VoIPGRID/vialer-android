package com.voipgrid.vialer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.voipgrid.vialer.api.UserSynchronizer
import com.voipgrid.vialer.callrecord.CallRecordFragment
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.reachability.ReachabilityReceiver
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.voip.IncomingCallHandler
import com.voipgrid.vialer.voip.VoipService
import com.voipgrid.vialer.voip.core.call.Call
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import javax.inject.Inject

class MainActivity : NavigationDrawerActivity() {

    @Inject lateinit var userSynchronizer: UserSynchronizer
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
        if (connectivityHelper.hasNetworkConnection()) return@launch

        userSynchronizer.sync()
    }

    /**
     * End immediately and return to the onboarding screen to let tthe user login
     * again.
     *
     */
    private fun returnUserToLoginScreen() {
        logout(true)
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
                            0 -> CallRecordFragment.TYPE.ALL_CALLS
                            1 -> CallRecordFragment.TYPE.MISSED_CALLS
                            else -> CallRecordFragment.TYPE.ALL_CALLS
                        })
                    }

                }
            })
        }
    }
}