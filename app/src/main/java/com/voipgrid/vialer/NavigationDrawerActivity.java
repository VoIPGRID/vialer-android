package com.voipgrid.vialer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.Destination;
import com.voipgrid.vialer.api.models.FixedDestination;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.Middleware;
import com.voipgrid.vialer.util.Storage;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

/**
 * NavigationDrawerActivity adds support to add a Toolbar and DrawerLayout to an Activity.
 */
public abstract class NavigationDrawerActivity
        extends AppCompatActivity
        implements Callback, AdapterView.OnItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener {

    private ArrayAdapter<Destination> mSpinnerAdapter;
    private DrawerLayout mDrawerLayout;
    private Spinner mSpinner;
    private Toolbar mToolbar;

    private Api mApi;
    private ConnectivityHelper mConnectivityHelper;
    private Storage mStorage;
    private SystemUser mSystemUser;


    private String mDestinationId;
    private boolean mFirstTimeOnItemSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mStorage = new Storage(this);
        mSystemUser = (SystemUser) mStorage.get(SystemUser.class);


        if (mSystemUser != null){
            mApi = ServiceGenerator.createService(
                    mConnectivityHelper,
                    Api.class,
                    getString(R.string.api_url),
                    new OkClient(ServiceGenerator.getOkHttpClient(
                            this, mSystemUser.getEmail(), mSystemUser.getPassword()))
            );

            // Preload availability.
            mApi.getUserDestination(this);
        }
    }

    /**
     * Sets a Toolbar to use as the Activities ActionBar.
     * Adds a top padding to the Toolbar for SDK >= KITKAT to make use of the Translucent Status Bar
     * and uses the Activity title to set as the Toolbar title.
     *
     * @param resId resource ID to a Toolbar in the Activity Layout
     */
    protected void setActionBar(@IdRes int resId) {
        mToolbar = (Toolbar) findViewById(resId);
        if(mToolbar == null) {
            throw new RuntimeException(
                    "NavigationDrawerActivity must have a valid resource ID to a Toolbar");
        }
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getTitle());
    }

    /**
     *  Sets a DrawerLayout to use as the Activities NavigationDrawer and adds a DrawerListener.
     *  Configures the ActionBar to show the navigation icon.
     *
     * @param resId resource ID to a DrawerLayout in the Activity Layout
     */
    protected void setNavigationDrawer(@IdRes int resId) {
        mDrawerLayout = (DrawerLayout) findViewById(resId);
        if(mDrawerLayout == null) {
            throw new RuntimeException(
                    "NavigationDrawerActivity must have a valid resource ID to a DrawerLayout");
        }

        ((NavigationView) mDrawerLayout.findViewById(R.id.navigation_view))
                .setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle drawerToggle = new CustomActionBarDrawerToggle(
                this,  mDrawerLayout, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );

        mDrawerLayout.setDrawerListener(drawerToggle);

        SystemUser systemUser = (SystemUser) new Storage(this).get(SystemUser.class);
        if(systemUser != null) {
            String phoneNumber = systemUser.getOutgoingCli();
            if (!TextUtils.isEmpty(phoneNumber)) {
                ((TextView) mDrawerLayout.findViewById(R.id.text_view_name)).setText(phoneNumber);
            }

            String email = systemUser.getEmail();
            if (!TextUtils.isEmpty(email)) {
                ((TextView) mDrawerLayout.findViewById(R.id.text_view_email)).setText(email);
            }
        }

        setVersionInfo((TextView) mDrawerLayout.findViewById(R.id.text_view_version));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.syncState();

        // Setup the spinner in the drawer.
        setupSpinner();
    }

    private void setVersionInfo(TextView textView) {
        if(textView != null) {
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = packageInfo.versionName;
                if(version.contains("beta")) {
                    textView.setText(getString(R.string.version_info_beta, version, packageInfo.versionCode));
                } else {
                    textView.setText(getString(R.string.version_info, version));
                }
                textView.setVisibility(View.VISIBLE);
            } catch (PackageManager.NameNotFoundException e) {
                textView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Returns the Activities Toolbar
     *
     * @return Toolbar
     */
    protected Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.navigation_item_statistics :
                startWebActivity(getString(R.string.statistics_menu_item_title), getString(R.string.web_statistics)); break;
            case R.id.navigation_item_dial_plan :
                startWebActivity(getString(R.string.dial_plan_menu_item_title), getString(R.string.web_dial_plan)); break;
            case R.id.navigation_item_info :
                startWebActivity(getString(R.string.info_menu_item_title), getString(R.string.url_app_info)); break;
            case R.id.navigation_item_settings :
                startActivity(new Intent(this, AccountActivity.class)); break;
            case R.id.navigation_item_logout :
                logout(); break;
        }
        return false;
    }

    /**
     * Perform logout; Remove the stored SystemUser and PhoneAccount and show the login view
     */
    private void logout() {
        /* Delete our account information */
        new Storage(this).clear();
        /* Mark ourselves as unregistered */
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(Middleware.Constants.REGISTRATION_STATUS,
                        Middleware.Constants.STATUS_UNREGISTERED)
                .commit();
        /* Start a new session */
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Start a new WebActivity to display the page
     * @param title
     * @param page
     */
    private void startWebActivity(String title, String page) {
        SystemUser systemUser = new Storage<SystemUser>(this).get(SystemUser.class);
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.PAGE, page);
        intent.putExtra(WebActivity.TITLE, title);
        intent.putExtra(WebActivity.USERNAME, systemUser.getEmail());
        intent.putExtra(WebActivity.PASSWORD, systemUser.getPassword());
        startActivity(intent);
    }

    @Override
    public void success(Object object, Response response) {
        if(object instanceof VoipGridResponse) {

            List<UserDestination> userDestinationObjects =
                    ((VoipGridResponse<UserDestination>) object).getObjects();

            if (userDestinationObjects == null || userDestinationObjects.size() <=0){
                return;
            }

            UserDestination userDestination = userDestinationObjects.get(0);

            // Create not available destination.
            Destination notAvailableDestination = new FixedDestination();
            notAvailableDestination.setDescription(getString(R.string.not_available));

            // Clear old list and the not available destination.
            mSpinnerAdapter.clear();
            mSpinnerAdapter.add(notAvailableDestination);

            // Set current destination.
            mDestinationId = userDestination.getId();

            Destination activeDestination = userDestination.getActiveDestination();

            List<Destination> destinations = userDestination.getDestinations();
            int activeIndex = 0;

            // Add all possible destinations to array.
            for(int i=0, size=destinations.size(); i<size; i++) {
                Destination destination = destinations.get(i);
                mSpinnerAdapter.add(destination);
                if(activeDestination != null &&
                        destination.getId().equals(activeDestination.getId())) {
                    activeIndex = i+1;
                }
            }
            // Update spinner.
            mSpinnerAdapter.notifyDataSetChanged();
            mSpinner.setSelection(activeIndex);
        }
    }

    @Override
    public void failure(RetrofitError error) {
        error.printStackTrace();
    }


    /**
     * Function to setup the availability spinner.
     */
    private void setupSpinner(){
        mSpinner = (Spinner) findViewById(R.id.menu_availability_spinner);
        mSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(mFirstTimeOnItemSelected) {
            mFirstTimeOnItemSelected = false;
        } else {
            Destination destination = (Destination) parent.getAdapter().getItem(position);
            SelectedUserDestinationParams params = new SelectedUserDestinationParams();
            params.fixedDestination = destination instanceof FixedDestination ?
                    destination.getId() : null;
            params.phoneAccount = destination instanceof PhoneAccount ?
                    destination.getId() : null;
            mApi.setSelectedUserDestination(mDestinationId, params, this);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Custom ActionBarDrawerToggle that allows us to update the availability
     * every time the drawer is opened.
     */
    private class CustomActionBarDrawerToggle extends ActionBarDrawerToggle {

        private Callback mActivity;

        public CustomActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
                                           Toolbar toolbar, int openDrawerContentDescRes,
                                           int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes,
                    closeDrawerContentDescRes);
            mActivity = (Callback) activity;
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            // Force a reload of availability every time the drawer is opened.
            mApi.getUserDestination(mActivity);
        }
    }
}
