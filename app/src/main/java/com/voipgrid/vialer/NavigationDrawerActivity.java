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

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
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

import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.Destination;
import com.voipgrid.vialer.api.models.FixedDestination;
import com.voipgrid.vialer.api.models.InternalNumbers;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.PhoneAccounts;
import com.voipgrid.vialer.api.models.SelectedUserDestinationParams;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UserDestination;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.middleware.MiddlewareHelper;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * NavigationDrawerActivity adds support to add a Toolbar and DrawerLayout to an Activity.
 */
public abstract class NavigationDrawerActivity extends LoginRequiredActivity
        implements Callback, AdapterView.OnItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener {

    @Inject Logout logout;

    private CustomFontSpinnerAdapter<Destination> mSpinnerAdapter;
    private DrawerLayout mDrawerLayout;
    private Spinner mSpinner;
    private Toolbar mToolbar;
    private TextView mNoConnectionText;
    private View mNavigationHeaderView;

    private VoipgridApi mVoipgridApi;
    private ConnectivityHelper mConnectivityHelper;
    private SystemUser mSystemUser;

    private String mSelectedUserDestinationId;
    private boolean mFirstTimeOnItemSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VialerApplication.get().component().inject(this);
        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mSystemUser = User.getVoipgridUser();
        refreshCurrentAvailability();
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
        refreshCurrentAvailability();
    }

    /**
     * Perform a request to refresh the current availability stats. This happens asynchronously.
     *
     */
    private void refreshCurrentAvailability() {
        if (mSystemUser == null) {
            return;
        }

        mVoipgridApi = ServiceGenerator.createApiService(this);
        Call<VoipGridResponse<UserDestination>> call = mVoipgridApi.getUserDestination();
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
                    textView.setText(getString(R.string.version_info_beta, version, String.valueOf(packageInfo.versionCode)));
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
                VoIPGRIDPortalWebActivity.launchForUserDestinations(this);
                break;
            case R.id.navigation_item_dial_plan:
                VoIPGRIDPortalWebActivity.launchForDialPlan(this);
                break;
            case R.id.navigation_item_info:
                VoIPGRIDPortalWebActivity.launchForAppInfo(this);
                break;
            case R.id.navigation_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
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
        logout.perform(false);
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

            storeInternalNumbers(userDestinationObjects);

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

    /**
     * Store a list of internal numbers to storage.
     *
     * @param userDestinationObjects
     */
    private void storeInternalNumbers(List<UserDestination> userDestinationObjects) {
        InternalNumbers internalNumbers = new InternalNumbers();
        PhoneAccounts phoneAccounts = new PhoneAccounts();

        for (UserDestination userDestination : userDestinationObjects) {
            internalNumbers.add(userDestination.getInternalNumber());

            for (PhoneAccount phoneAccount : userDestination.getPhoneAccounts()) {
                internalNumbers.add(phoneAccount.getNumber());
                phoneAccounts.add(phoneAccount.getId());
            }
        }

        User.internal.setInternalNumbers(internalNumbers);
        User.internal.setPhoneAccounts(phoneAccounts);
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
                VoIPGRIDPortalWebActivity.launchForUserDestinations(this);
            } else {
                Destination destination = (Destination) parent.getAdapter().getItem(position);
                if (destination.getDescription().equals(getString(R.string.not_available))) {
                    MiddlewareHelper.unregister(this);
                }
                SelectedUserDestinationParams params = new SelectedUserDestinationParams();
                params.fixedDestination = destination instanceof FixedDestination ? destination.getId() : null;
                params.phoneAccount = destination instanceof PhoneAccount ? destination.getId() : null;
                Call<Object> call = mVoipgridApi.setSelectedUserDestination(mSelectedUserDestinationId, params);
                call.enqueue(this);
                if (!MiddlewareHelper.isRegistered()) {
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

        CustomActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
                                           Toolbar toolbar, int openDrawerContentDescRes,
                                           int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            if (mSpinner != null) {
                refreshCurrentAvailability();
                // Make sure the systemuser info shown is up-to-date.
                mSystemUser = User.getVoipgridUser();
                setSystemUserInfo();
            }
        }
    }
}
