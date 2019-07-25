package com.voipgrid.vialer

import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.voipgrid.vialer.api.ApiTokenFetcher
import com.voipgrid.vialer.api.models.SystemUser
import com.voipgrid.vialer.callrecord.CallRecordFragment
import com.voipgrid.vialer.contacts.ImportContactsForT9Search
import com.voipgrid.vialer.dialer.DialerActivity
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.onboarding.Onboarder
import com.voipgrid.vialer.onboarding.SingleOnboardingStepActivity
import com.voipgrid.vialer.onboarding.steps.TwoFactorStep
import com.voipgrid.vialer.permissions.ContactsPermission
import com.voipgrid.vialer.reachability.ReachabilityReceiver
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.JsonStorage
import com.voipgrid.vialer.util.PhoneAccountHelper
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : NavigationDrawerActivity() {

    @Inject lateinit var jsonStorage: JsonStorage<SystemUser>
    @Inject lateinit var phoneAccountHelper: PhoneAccountHelper
    @Inject lateinit var reachabilityReceiver: ReachabilityReceiver

    private val logger = Logger(this)

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

        if (connectivityHelper.hasNetworkConnection()) {
            fetchApiTokenIfDoesNotExist()
            phoneAccountHelper.executeUpdatePhoneAccountTask()
        }

        observeContacts()
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
     * Schedule a task to update the contacts when there are changes.
     *
     */
    private fun observeContacts() {
        if (!ContactsPermission.hasPermission(this)) return

        ImportContactsForT9Search.run()
        ImportContactsForT9Search.schedule()
    }

    /**
    * If we do not currently have an api token stored, fetch one from the server.
    *
    */
    private fun fetchApiTokenIfDoesNotExist() {
        if (hasApiToken()) return

        logger.i("There is no api-key currently stored, will attempt to fetch one")

        ApiTokenFetcher.usingSavedCredentials(this).setListener(ApiTokenListener()).fetch()
    }

    /**
     * End immediately and return to the onboarding screen to let tthe user login
     * again.
     *
     */
    private fun returnUserToLoginScreen() {
        logout.perform(true)
        Onboarder.start(this)
        finish()
    }

    /**
     * Check that the user has fully completed the onboarding process. If they have not,
     * we will redirect them to login.
     *
     */
    private fun userIsOnboarded(): Boolean {
        return jsonStorage.has(SystemUser::class.java) && jsonStorage.get(SystemUser::class.java).mobileNumber != null
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
                    view_pager.currentItem = tab?.position ?: 0
                }
            })
        }

        view_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tab_layout))
        view_pager.adapter = TabAdapter(supportFragmentManager)
    }

    /**
     * Listen for the api token request and display a dialog to enter the two-factor token
     * if one is required.
     *
     */
    private inner class ApiTokenListener : ApiTokenFetcher.ApiTokenListener {
        override fun twoFactorCodeRequired() {
            if (isFinishing) {
                return
            }

            logger.i("Prompting the user to enter a two-factor code")

            SingleOnboardingStepActivity.launch(this@MainActivity, TwoFactorStep::class.java)
        }

        override fun onSuccess(apiToken: String) {}

        override fun onFailure() {}
    }

    /**
     * Tab adapter to handle tabs in the ViewPager
     */
    private inner class TabAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when(position) {
                0 -> CallRecordFragment.mine()
                1 -> CallRecordFragment.all()
                else -> TabFragment.newInstance("")
            }
        }

        override fun getCount(): Int {
            return 2
        }
    }
}