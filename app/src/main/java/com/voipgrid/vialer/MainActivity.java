package com.voipgrid.vialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.voipgrid.vialer.api.ApiTokenFetcher;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.callrecord.CallRecordFragment;
import com.voipgrid.vialer.contacts.ImportContactsForT9Search;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.onboarding.AccountFragment;
import com.voipgrid.vialer.onboarding.OnboardingActivity;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.permissions.ContactsPermission;
import com.voipgrid.vialer.permissions.PhonePermission;
import com.voipgrid.vialer.reachability.ReachabilityReceiver;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.BatteryOptimizationManager;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.PhoneAccountHelper;

import javax.inject.Inject;

public class MainActivity extends NavigationDrawerActivity implements View.OnClickListener {

    private ViewPager mViewPager;
    private Logger mLogger;

    private ReachabilityReceiver mReachabilityReceiver;

    @Inject BatteryOptimizationManager batteryOptimizationManager;
    @Inject SharedPreferences sharedPreferences;
    @Inject FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VialerApplication.get().component().inject(this);

        Bundle startBundle = getIntent().getExtras();
        if (startBundle != null) {
            boolean onBoot = startBundle.getBoolean("OnBoot");
            if (onBoot) {
                finish();
                return;
            }
        }

        mLogger = new Logger(this.getClass());
        JsonStorage jsonStorage = new JsonStorage(this);
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(this);
        Boolean hasSystemUser = jsonStorage.has(SystemUser.class);
        SystemUser systemUser = (SystemUser) jsonStorage.get(SystemUser.class);

        // Check if the app has a SystemUser.
        // When there is no SystemUser present start the on boarding process.
        // When there is a SystemUser but there is no mobile number configured go to the
        // on boarding part where the mobile number needs to be configured.
        if (!hasSystemUser) {
            // Start on boarding flow.
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        } else if (systemUser.getMobileNumber() == null) {
            Intent intent = new Intent(this, SetupActivity.class);

            Bundle bundle = new Bundle();
            bundle.putInt("fragment", R.id.fragment_account);
            bundle.putString("activity", AccountFragment.class.getSimpleName());
            intent.putExtras(bundle);

            startActivity(intent);
            finish();
            return;
        } else if (connectivityHelper.hasNetworkConnection()) {
            fetchApiTokenIfDoesNotExist();

            // Update SystemUser and PhoneAccount on background thread.
            new PhoneAccountHelper(this).executeUpdatePhoneAccountTask();
        }

        observeContacts();

        setContentView(R.layout.activity_main);

        // Set the Toolbar to use as ActionBar.
        setActionBar(R.id.action_bar);

        setNavigationDrawer(R.id.drawer_layout);

        // Set tabs.
        setupTabs();

        FloatingActionButton openDialerFab = findViewById(R.id.floating_action_button);
        openDialerFab.setOnClickListener(this);

        mReachabilityReceiver = new ReachabilityReceiver(this);
    }

    /**
     * If we do not currently have an api token stored, fetch one from the server.
     *
     */
    private void fetchApiTokenIfDoesNotExist() {
        if (hasApiToken()) return;

        mLogger.i("There is no api-key currently stored, will attempt to fetch one");

        ApiTokenFetcher.usingSavedCredentials(this).setListener(new ApiTokenListener()).fetch();
    }

    @Override
    protected void onResume() {
        mReachabilityReceiver.startListening();
        super.onResume();


        // We currently only support a single call so any time this activity is opened, we will
        // request the SipService to display the current call. If there is no current call, this will have no
        // affect.
        SipService.performActionOnSipService(this, SipService.Actions.DISPLAY_CALL_IF_AVAILABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mReachabilityReceiver.stopListening();
    }

    /**
     * Sets Tab Recents and Tab Contacts to the TabLayout
     */
    private void setupTabs() {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabTextColors(
                ContextCompat.getColor(this, R.color.tab_inactive),
                ContextCompat.getColor(this, R.color.tab_active));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_title_recents));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_title_missed));

        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.setAdapter(adapter);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());

                firebaseAnalytics.setCurrentScreen(MainActivity.this, getScreenName(tab.getText().toString()), null);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    /**
     * Schedule a task to update the contacts when there are changes.
     *
     */
    private void observeContacts() {
        if (!ContactsPermission.hasPermission(this)) return;

        ImportContactsForT9Search.Companion.run();
        ImportContactsForT9Search.Companion.schedule();
    }

    private String getScreenName(String text) {
        if(text.equals(getString(R.string.tab_title_recents))) {
            return getString(R.string.analytics_screenname_recents);
        }
        if(text.equals(getString(R.string.tab_title_missed))) {
            return getString(R.string.analytics_screenname_missed);
        }
        return text;
    }

    /**
     * Handle onClick events within the MainActivity
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.floating_action_button : openDialer(); break;
        }
    }

    /**
     * Show the dialer view
     */
    public void openDialer() {
        Intent intent = new Intent(this, DialerActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(
                        this, findViewById(R.id.floating_action_button),
                        "floating_action_button_transition_name");
        startActivity(intent, options.toBundle());
    }

    /**
     * Tab adapter to handle tabs in the ViewPager
     */
    public class TabAdapter extends FragmentPagerAdapter {

        public TabAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0) {
                return CallRecordFragment.mine();
            }
            if(position == 1) {
                return CallRecordFragment.all();
            }
            return TabFragment.newInstance("");
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    /**
     * Listen for the api token request and display a dialog to enter the two-factor token
     * if one is required.
     *
     */
    private class ApiTokenListener implements ApiTokenFetcher.ApiTokenListener {
        @Override
        public void twoFactorCodeRequired() {
            if(isFinishing()) {
                return;
            }

            mLogger.i("Prompting the user to enter a two-factor code");

            TwoFactorAuthenticationDialogFragment twoFactorAuthenticationDialogFragment = new TwoFactorAuthenticationDialogFragment();
            twoFactorAuthenticationDialogFragment.show(getFragmentManager(), "");
            twoFactorAuthenticationDialogFragment.setCancelable(false);
        }

        @Override
        public void onSuccess(String apiToken) {}

        @Override
        public void onFailure() {}
    }
}