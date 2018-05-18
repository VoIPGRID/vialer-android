package com.voipgrid.vialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.Destination;
import com.voipgrid.vialer.api.models.FixedDestination;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.util.AccountHelper;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.middleware.MiddlewareHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.voipgrid.vialer.middleware.MiddlewareConstants.REGISTRATION_STATUS;
import static com.voipgrid.vialer.middleware.MiddlewareConstants.STATUS_UNREGISTERED;

/**
 * NavigationDrawerActivity adds support to add a Toolbar and DrawerLayout to an Activity.
 */
public abstract class NavigationDrawerActivity extends LoginRequiredActivity
        implements Callback, AdapterView.OnItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener {

    private CustomFontSpinnerAdapter<Destination> mSpinnerAdapter;
    private DrawerLayout mDrawerLayout;
    private Spinner mSpinner;
    private Toolbar mToolbar;
    private TextView mNoConnectionText;
    private View mNavigationHeaderView;

    private Api mApi;
    private ConnectivityHelper mConnectivityHelper;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;

    private String mSelectedUserDestinationId;
    private boolean mFirstTimeOnItemSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mJsonStorage = new JsonStorage(this);
        mSystemUser = (SystemUser) mJsonStorage.get(SystemUser.class);

        if (mSystemUser != null && !TextUtils.isEmpty(getPassword())) {
            mApi = ServiceGenerator.createApiService(this);

            // Preload availability.
            Call<VoipGridResponse<UserDestination>> call = mApi.getUserDestination();
            call.enqueue(this);
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
        if (mToolbar == null) {
            throw new RuntimeException(
                    "NavigationDrawerActivity must have a valid resource ID to a Toolbar");
        }
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getTitle());
    }

    /**
     * Sets a DrawerLayout to use as the Activities NavigationDrawer and adds a DrawerListener.
     * Configures the ActionBar to show the navigation icon.
     *
     * @param resId resource ID to a DrawerLayout in the Activity Layout
     */
    protected void setNavigationDrawer(@IdRes int resId) {
        mDrawerLayout = (DrawerLayout) findViewById(resId);
        if (mDrawerLayout == null) {
            throw new RuntimeException(
                    "NavigationDrawerActivity must have a valid resource ID to a DrawerLayout");
        }

        NavigationView navigationView = (NavigationView) mDrawerLayout.findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle drawerToggle = new CustomActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );

        mDrawerLayout.addDrawerListener(drawerToggle);

        // Version 23.1.0 switches NavigationView to using a RecyclerView (rather than the previous
        // ListView) and the header is added as one of those elements. This means it is not
        // instantly available to call findViewById() - a layout pass is
        // needed before it is attached to the NavigationView.
        mNavigationHeaderView = navigationView.getHeaderView(0);

        // Set the email address and phone number of the logged in user.
        setSystemUserInfo();

        // Set which version of the app the user is using.
        setVersionInfo((TextView) mDrawerLayout.findViewById(R.id.text_view_version));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle.syncState();

        // Setup the spinner in the drawer.
        setupSpinner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force a reload of availability when returned to the activity,
        // per example when returning from the webview.
        Call<VoipGridResponse<UserDestination>> call = mApi.getUserDestination();
        call.enqueue(this);
    }

    private void setSystemUserInfo() {
        if (mSystemUser != null) {
            String phoneNumber = mSystemUser.getOutgoingCli();
            if (!TextUtils.isEmpty(phoneNumber)) {
                ((TextView) mNavigationHeaderView.findViewById(R.id.text_view_name)).setText(phoneNumber);
            }

            String email = mSystemUser.getEmail();
            if (!TextUtils.isEmpty(email)) {
                ((TextView) mNavigationHeaderView.findViewById(R.id.text_view_email)).setText(email);
            }
        }
    }

    private void setVersionInfo(TextView textView) {
        if (textView != null) {
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = packageInfo.versionName;
                if (version.contains("beta")) {
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
            case R.id.navigation_item_statistics:
                startWebActivity(
                        getString(R.string.statistics_menu_item_title),
                        getString(R.string.web_statistics),
                        getString(R.string.analytics_statistics_title)
                );
                break;
            case R.id.navigation_item_dial_plan:
                startWebActivity(
                        getString(R.string.dial_plan_menu_item_title),
                        getString(R.string.web_dial_plan),
                        getString(R.string.analytics_dial_plan_title)
                );
                break;
            case R.id.navigation_item_info:
                startWebActivity(
                        getString(R.string.info_menu_item_title),
                        getString(R.string.url_app_info),
                        getString(R.string.analytics_info_title)
                );
                break;
            case R.id.navigation_item_settings:
                startActivity(new Intent(this, AccountActivity.class));
                break;
            case R.id.navigation_item_logout:
                logout();
                break;
        }
        return false;
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(this.getString(R.string.logout_dialog_text));
        builder.setPositiveButton(this.getString(R.string.logout_dialog_positive),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        performLogout();
                    }
                });
        builder.setNegativeButton(this.getString(R.string.logout_dialog_negative),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Perform logout; Remove the stored SystemUser and PhoneAccount and show the login view
     */
    private void performLogout() {
        if (mConnectivityHelper.hasNetworkConnection()) {
            MiddlewareHelper.unregister(this);

            // Delete our account information.
            mJsonStorage.clear();
            new AccountHelper(this).clearCredentials();
            // Mark ourselves as unregistered.
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putInt(REGISTRATION_STATUS, STATUS_UNREGISTERED)
                    .apply();
            // Start a new session.
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getText(R.string.cannot_logout_error_title));
            alertDialogBuilder
                    .setMessage(getText(R.string.cannot_logout_error_text))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    /**
     * Start a new WebActivity to display the page
     *
     * @param title
     * @param page
     */
    private void startWebActivity(String title, String page, String gaTitle) {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.PAGE, page);
        intent.putExtra(WebActivity.TITLE, title);
        intent.putExtra(WebActivity.USERNAME, getEmail());
        intent.putExtra(WebActivity.PASSWORD, getPassword());
        intent.putExtra(WebActivity.API_TOKEN, getApiToken());
        intent.putExtra(WebActivity.GA_TITLE, gaTitle);
        startActivity(intent);
    }


    @Override
    public void onFailure(@NonNull Call call, @NonNull Throwable t) {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
            Toast.makeText(this, getString(R.string.set_userdestination_api_fail), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) {
        if (!response.isSuccessful()) {
            if (mDrawerLayout != null && mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                Toast.makeText(this, getString(R.string.set_userdestination_api_fail), Toast.LENGTH_LONG).show();
            }
            if (!mConnectivityHelper.hasNetworkConnection()) {
                // First check if there is a entry already to avoid duplicates.
                if (mSpinner != null && mNoConnectionText != null) {
                    mSpinner.setVisibility(View.GONE);
                    mNoConnectionText.setVisibility(View.VISIBLE);
                }
            }
        }
        if (response.body() instanceof VoipGridResponse) {
            List<UserDestination> userDestinationObjects = ((VoipGridResponse<UserDestination>) response.body()).getObjects();

            if (userDestinationObjects == null || userDestinationObjects.size() <= 0 || mSpinnerAdapter == null) {
                return;
            }

            UserDestination userDestination = userDestinationObjects.get(0);

            // Create not available destination.
            Destination notAvailableDestination = new FixedDestination();
            notAvailableDestination.setDescription(getString(R.string.not_available));

            // Clear old list and add the not available destination.
            mSpinnerAdapter.clear();
            mSpinnerAdapter.add(notAvailableDestination);

            // Set current destination.
            mSelectedUserDestinationId = userDestination.getSelectedUserDestination().getId();

            Destination activeDestination = userDestination.getActiveDestination();

            List<Destination> destinations = userDestination.getDestinations();
            int activeIndex = 0;

            // Add all possible destinations to array.
            for (int i = 0, size = destinations.size(); i < size; i++) {
                Destination destination = destinations.get(i);
                mSpinnerAdapter.add(destination);
                if (activeDestination != null &&
                        destination.getId().equals(activeDestination.getId())) {
                    activeIndex = i + 1;
                }
            }

            // Create add destination field.
            Destination addDestination = new FixedDestination();
            String addDestinationText = getString(R.string.fa_plus_circle) +
                    "   " + getString(R.string.add_availability);
            addDestination.setDescription(addDestinationText);
            mSpinnerAdapter.add(addDestination);

            mSpinnerAdapter.notifyDataSetChanged();
            mSpinner.setSelection(activeIndex);
        }
    }

    private static class CustomFontSpinnerAdapter<D> extends ArrayAdapter {
        // Initialise custom font, for example:
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "fontawesome-webfont.ttf");

        private CustomFontSpinnerAdapter(Context context, int resource) {
            super(context, resource);
        }

        // Affects default (closed) state of the spinner
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTypeface(font);
            return view;
        }

        // Affects opened state of the spinner
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setTypeface(font);
            return view;
        }
    }

    /**
     * Function to setup the availability spinner.
     */
    private void setupSpinner() {
        mSpinner = (Spinner) mNavigationHeaderView.findViewById(R.id.menu_availability_spinner);
        mSpinnerAdapter = new CustomFontSpinnerAdapter<>(this, android.R.layout.simple_spinner_item);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(this);

        // Setup spinner placeholder text for when there is no connection and thus no spinner options
        mNoConnectionText = (TextView) mNavigationHeaderView.findViewById(R.id.no_availability_text);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mFirstTimeOnItemSelected) {
            mFirstTimeOnItemSelected = false;
        } else {
            if (parent.getCount() - 1 == position) {
                startWebActivity(
                        getString(R.string.add_destination_title),
                        getString(R.string.web_add_destination),
                        getString(R.string.analytics_add_destination_title)
                );
            } else {
                Destination destination = (Destination) parent.getAdapter().getItem(position);
                if (destination.getDescription().equals(getString(R.string.not_available))) {
                    MiddlewareHelper.unregister(this);
                }
                SelectedUserDestinationParams params = new SelectedUserDestinationParams();
                params.fixedDestination = destination instanceof FixedDestination ? destination.getId() : null;
                params.phoneAccount = destination instanceof PhoneAccount ? destination.getId() : null;
                Call<Object> call = mApi.setSelectedUserDestination(mSelectedUserDestinationId, params);
                call.enqueue(this);
                if (!MiddlewareHelper.isRegistered(this)) {
                    // If the previous destination was not available, or if we're not registered
                    // for another reason, register again.
                    MiddlewareHelper.registerAtMiddleware(this);
                }
            }
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
            if (mSpinner != null) {
                // Force a reload of availability every time the drawer is opened.
                Call<VoipGridResponse<UserDestination>> call = mApi.getUserDestination();
                call.enqueue(mActivity);
                // Make sure the systemuser info shown is up-to-date.
                mSystemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
                setSystemUserInfo();
            }
        }
    }
}
