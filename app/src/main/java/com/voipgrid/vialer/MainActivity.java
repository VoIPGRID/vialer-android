package com.voipgrid.vialer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.callrecord.CallRecordFragment;
import com.voipgrid.vialer.contacts.ContactsPermission;
import com.voipgrid.vialer.contacts.SyncUtils;
import com.voipgrid.vialer.contacts.UpdateChangedContactsService;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.util.PhoneAccountHelper;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.UpdateActivity;
import com.voipgrid.vialer.util.UpdateHelper;


public class MainActivity extends NavigationDrawerActivity implements
        View.OnClickListener,
        CallRecordFragment.OnFragmentInteractionListener {

    private ViewPager mViewPager;

    private ConnectivityHelper mConnectivityHelper;
    private JsonStorage mJsonStorage;

    private boolean mAskForPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mJsonStorage = new JsonStorage(this);

        mConnectivityHelper = ConnectivityHelper.get(this);

        /* check if the app has a SystemUser */
        if (!mJsonStorage.has(SystemUser.class)) {
            //start onboarding flow
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        } else if (mConnectivityHelper.hasNetworkConnection()) {
            //update SystemUser and PhoneAccount on background thread
            new PhoneAccountHelper(this).executeUpdatePhoneAccountTask();
        }
        // Start UpdateActivity if app has updated.
        if (UpdateHelper.requiresUpdate(this)) {
            this.startActivity(new Intent(this, UpdateActivity.class));
            finish();
            return;
        } else {
            if (SyncUtils.requiresFullContactSync(this)) {
                SyncUtils.requestContactSync(this);
            } else {
                // Live contact updates are not supported below api level 18.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                        && ContactsPermission.hasPermission(this)) {
                    startService(new Intent(this, UpdateChangedContactsService.class));
                }
            }

            SyncUtils.setPeriodicSync(this);

            setContentView(R.layout.activity_main);

            /* set the Toolbar to use as ActionBar */
            setActionBar(R.id.action_bar);

            setNavigationDrawer(R.id.drawer_layout);

            /* set tabs */
            setupTabs();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!ContactsPermission.hasPermission(this)){
            // We need to avoid a permission loop.
            if (mAskForPermission) {
                mAskForPermission = false;
                ContactsPermission.askForPermission(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode ==
                this.getResources().getInteger(R.integer.contact_permission_request_code)) {
            boolean allPermissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // ContactSync.
                SyncUtils.requestContactSync(this);
            }
        }
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

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.setAdapter(new TabAdapter(getSupportFragmentManager()));

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());

                // Get tracker.
                Tracker tracker = ((AnalyticsApplication) getApplication()).getDefaultTracker();

                // Set screen name.
                tracker.setScreenName(getScreenName(tab.getText().toString()));

                // Send a screen view.
                tracker.send(new HitBuilders.ScreenViewBuilder().build());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
    private void openDialer() {
        Intent intent = new Intent(this, DialerActivity.class);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            ActivityOptionsCompat options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(
                            this, findViewById(R.id.floating_action_button),
                            "floating_action_button_transition_name");
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    /**
     * Tab adapter to handle tabs in the ViewPager
     */
    public class TabAdapter extends FragmentStatePagerAdapter {

        public TabAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0) { // recents tab
                return CallRecordFragment.newInstance(null);
            }
            if(position == 1) { // missed tab
                return CallRecordFragment.newInstance(CallRecordFragment.FILTER_MISSED_RECORDS);
            }
            return TabFragment.newInstance("");
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
