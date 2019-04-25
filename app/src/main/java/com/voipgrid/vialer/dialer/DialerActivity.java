package com.voipgrid.vialer.dialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.calling.Dialer;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.contacts.ContactsSyncTask;
import com.voipgrid.vialer.contacts.SyncUtils;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.permissions.ContactsPermission;
import com.voipgrid.vialer.reachability.ReachabilityReceiver;
import com.voipgrid.vialer.t9.ContactCursorLoader;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.IconHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.LoginRequiredActivity;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.inject.Inject;

import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * DialerActivity with a numpad view and functionality to perform a T9 contacts search
 */
public class DialerActivity extends LoginRequiredActivity implements
        View.OnClickListener,
        AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, Dialer.Listener {
    public static final String LAST_DIALED = "last_dialed";

    @Inject SharedPreferences mSharedPreferences;

    private SimpleCursorAdapter mContactsAdapter = null;

    @Inject AnalyticsHelper mAnalyticsHelper;
    @Inject ConnectivityHelper mConnectivityHelper;
    @Inject JsonStorage mJsonStorage;
    @Inject ReachabilityReceiver mReachabilityReceiver;
    @Inject Contacts mContacts;

    DialHelper mDialHelper;

    private String t9Query;

    private boolean mHasPermission;
    private boolean mAskForPermission;

    @BindView(R.id.list_view) ListView mContactsListView;
    @BindView(R.id.t9helper) View mT9HelperFragment;
    @BindView(R.id.message) TextView mEmptyView;
    @BindView(R.id.button_call) ImageButton mFloatingActionButton;
    @BindView(R.id.bottom) Dialer mDialer;
    @BindView(R.id.top) ViewGroup mTop;
    @BindView(R.id.no_contact_permission_warning) View mNoContactPermissionWarning;
    @BindView(R.id.permission_contact_description) TextView mPermissionContactDescription;
    @BindView(R.id.progress_bar) ProgressBar mProgressBar;
    @BindView(R.id.progress_text) TextView mProgressText;
    @BindView(R.id.contact_processing_container) View mContactsProcessingContainer;

    private Thread mContactsProcessingThread;

    public static final int RESULT_DIALED_NUMBER = 1;

    public static final String EXTRA_RETURN_AS_RESULT = "EXTRA_RETURN_AS_RESULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);
        ButterKnife.bind(this);
        mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.color_primary), PorterDuff.Mode.SRC_ATOP);
        VialerApplication.get().component().inject(this);
        mDialHelper = DialHelper.fromActivity(this);
        mDialer.setListener(this);
        mPermissionContactDescription.setText(getString(R.string.permission_contact_description, getString(R.string.app_name)));

        Intent intent = getIntent();
        String type = intent.getType();

        // Sadly HTC fails to include the mime-type in the intent in 4.4.2.
        Uri contactUri = intent.getData();

        mHasPermission = ContactsPermission.hasPermission(this);
        mAskForPermission = true;
        // Check for contact permissions before doing contact related work.
        if (mHasPermission) {
            setupContactsListView();

            /**
             * The app added a "Vialer call <number>" to the native contacts app. clicking this
             * opens the app with the appname's profile and the data necessary for opening the app.
             */
            if ((!TextUtils.isEmpty(type) && type.equals(getString(R.string.profile_mimetype)))
                    || contactUri != null) {
                // Redirect user to login.
                // This can be needed when a user logs out and in the logged out state
                // presses call with vialer in a contact.
                if (!mJsonStorage.has(SystemUser.class)) {
                    startActivity(new Intent(this, SetupActivity.class));
                    finish();
                }

                Cursor cursor = getContentResolver().query(contactUri, new String[]{
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME,
                        ContactsContract.Data.DATA3}, null, null, null);
                cursor.moveToFirst();
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA3));
                mDialer.setNumber(number);
                cursor.close();
            }
        } else {
            mNoContactPermissionWarning.setVisibility(View.VISIBLE);
            mT9HelperFragment.setVisibility(View.GONE);
            mContactsListView.setVisibility(View.GONE);
        }

        // Make sure there is no keyboard popping up when pasting in the dialer input field.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        if (isForTransfer()) {
            mFloatingActionButton.setImageResource(R.drawable.ic_call_transfer);
        }
    }

    private boolean isForTransfer() {
        return getIntent().getBooleanExtra(EXTRA_RETURN_AS_RESULT, false);
    }

    public void numberWasChanged(String phoneNumber) {
        t9Query = phoneNumber;

        if (phoneNumber.length() == 0) {
            clearContactList();
        } else {
            // Load contacts matching t9Query.
            getSupportLoaderManager().restartLoader(1, null, DialerActivity.this).forceLoad();
        }
    }

    @Override
    public void digitWasPressed(String digit) {

    }

    @Override
    public void exitButtonWasPressed() {

    }

    @OnClick(R.id.give_contact_permission_button)
    void openAndroidApplicationDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(intent, 1);
    }

    /**
     * Function to clear the contact list in the dialer.
     */
    private void clearContactList() {
        if (!ContactsPermission.hasPermission(this)) {
            mNoContactPermissionWarning.setVisibility(View.VISIBLE);
            return;
        }

        if (mContactsAdapter != null) {
            mContactsAdapter.swapCursor(null);
            mContactsAdapter.notifyDataSetChanged();
        }

        mT9HelperFragment.setVisibility(View.VISIBLE);
        mContactsListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mNoContactPermissionWarning.setVisibility(View.GONE);
        mEmptyView.setText("");
    }

    /**
     * Function to get the bitmap from the uri.
     *
     * @param thumbnailUriString String uri of the file
     * @return Bitmap of the uri.
     */
    private Bitmap loadContactThumbnail(String thumbnailUriString) {
        Uri thumbUri = Uri.parse(thumbnailUriString);
        AssetFileDescriptor afd = null;
        try {
            afd = getContentResolver().openAssetFileDescriptor(thumbUri, "r");
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            if (fileDescriptor != null) {
                // Decodes the bitmap
                return BitmapFactory.decodeFileDescriptor(
                        fileDescriptor, null, null);
            }
        } catch (FileNotFoundException e) {

        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }

    /**
     * Function to setup the listview and cursor adapter.
     */
    private void setupContactsListView() {
        mContactsAdapter = new SimpleCursorAdapter(
                getBaseContext(),
                R.layout.list_item_contact,
                null,
                new String[]{"name", "photo", "number", "type"},
                new int[]{
                        R.id.text_view_contact_name,
                        R.id.text_view_contact_icon,
                        R.id.text_view_contact_information,
                        R.id.text_view_contact_type
                },
                0
        );

        mContactsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            /**
             * Binds the Cursor column defined by the specified index to the specified view.
             */
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text_view_contact_icon) {
                    // The class stores a contact uri for which
                    // we can retrieve a photo.
                    boolean hasThumbnail = false;
                    String thumbnailUriString = cursor.getString(columnIndex);
                    if (thumbnailUriString != null) {
                        Bitmap thumbNail = loadContactThumbnail(thumbnailUriString);
                        if (thumbNail != null) {
                            hasThumbnail = true;
                            ((CircleImageView) view).setImageBitmap(thumbNail);
                        }
                    }

                    if (!hasThumbnail) {
                        String firstLetter = cursor.getString(1).replaceAll("\\<.*?>", "").substring(0, 1);
                        Bitmap bitmapImage = IconHelper.getCallerIconBitmap(firstLetter, cursor.getString(0), 0);
                        ((CircleImageView) view).setImageBitmap(bitmapImage);
                    }
                    return true;
                } else if (view instanceof TextView) {
                    String result = cursor.getString(columnIndex);
                    ((TextView) view).setText(Html.fromHtml(result != null ? result : ""));
                    return true;
                }
                return false;
            }
        });

        // Getting reference to listview.
        mContactsListView.setOnScrollListener(this);
        mContactsListView.setOnItemClickListener(this);
        mContactsListView.setAdapter(mContactsAdapter);
        mContactsListView.setEmptyView(mEmptyView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if wifi should be turned back on.
        if (ConnectivityHelper.mWifiKilled) {
            mConnectivityHelper.useWifi(this, true);
            ConnectivityHelper.mWifiKilled = false;
        }

        // Permission changed since last accessing this Activity.
        if (mHasPermission != ContactsPermission.hasPermission(this)) {
            // Force a recreate of the Activity to reflect the new permission.
            Intent intent = getIntent();
            startActivity(intent);
            finish();
        }

        // If we don't have permission we need to ask for it.
        if (!mHasPermission) {
            // We need to avoid a permission loop.
            if (mAskForPermission) {
                mAskForPermission = false;
                ContactsPermission.askForPermission(this);
            }
        }
        mReachabilityReceiver.startListening();
        clearContactList();

        if (mContactsProcessingThread == null || !mContactsProcessingThread.isAlive()) {
            mContactsProcessingThread = new Thread(new ProgressUpdater(this));
            mContactsProcessingThread.start();
        }

        refreshUi();
    }

    @Override
    public void onStop() {
        super.onStop();
        mReachabilityReceiver.stopListening();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mContactsProcessingThread != null && mContactsProcessingThread.isAlive()) {
            mContactsProcessingThread.interrupt();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
        if (!allPermissionsGranted(permissions, grantResults)) {
            return;
        }

        if (requestCode == this.getResources().getInteger(R.integer.contact_permission_request_code)) {
            // ContactSync.
            SyncUtils.requestContactSync(this);
            // Reload Activity to reflect new permission.
            Intent intent = getIntent();
            startActivity(intent);
            finish();
        } else if (requestCode == this.getResources().getInteger(R.integer.microphone_permission_request_code)) {
            mDialHelper.callAttemptedNumber();
        }
    }

    /**
     * Initiate an outgoing call by starting CallActivity and pass a SipUri based on the number
     *
     * @param number      number to call
     * @param contactName contact name to display
     */
    public void onCallNumber(String number, String contactName) {
        if (isForTransfer()) {
            Intent intent = new Intent();
            intent.putExtra("DIALED_NUMBER", number);
            setResult(RESULT_DIALED_NUMBER, intent);
            finish();
            return;
        }

        String phoneNumberToCall = PhoneNumberUtils.format(number);
        if (number.length() < 1) {
            Toast.makeText(this, getString(R.string.dialer_invalid_number), Toast.LENGTH_LONG).show();
        } else {
            mDialHelper.callNumber(phoneNumberToCall, contactName);
            mSharedPreferences.edit().putString(LAST_DIALED, number).apply();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_dialpad:
                showDialpad();
                break;
        }
    }

    @OnClick(R.id.button_call)
    void onCallButtonClicked() {
        String phoneNumber = mDialer.getNumber();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            mFloatingActionButton.setClickable(false);
            onCallNumber(PhoneNumberUtils.format(phoneNumber), null);
        } else {
            // Set last dialed number on call button clicked when number is empty.
            String last_dialed = mSharedPreferences.getString(LAST_DIALED, "");
            mDialer.setNumber(last_dialed);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String name = ((TextView) view.findViewById(
                R.id.text_view_contact_name)).getText().toString();
        String number = ((TextView) view.findViewById(
                R.id.text_view_contact_information)).getText().toString();
        onCallNumber(number, name);
    }

    private void showDialpad() {
        mDialer.setVisibility(View.VISIBLE);
        findViewById(R.id.button_dialpad).setVisibility(View.INVISIBLE);
        findViewById(R.id.button_call).setVisibility(View.VISIBLE);
    }

    /**
     * Expand the contacts scroller to full screen and add a button to return to the dialpad.
     *
     */
    private void expandContactsScroller() {
        mDialer.setVisibility(View.GONE);
        findViewById(R.id.button_dialpad).setVisibility(View.VISIBLE);
        findViewById(R.id.button_call).setVisibility(View.GONE);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL &&
                mDialer.getVisibility() == View.VISIBLE) {
            expandContactsScroller();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Create loader and set t9Query to load for.
        ContactCursorLoader loader = new ContactCursorLoader(this);
        loader.setT9Query(t9Query);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mContactsAdapter == null) {
            return;
        }

        if (mDialer.getNumber().isEmpty()) {
            clearContactList();
            return;
        }

        if (!ContactsSyncTask.getProgress().isComplete()) {
            refreshUi();
            return;
        }

        // Set new data on adapter and notify data changed.
        mContactsAdapter.swapCursor(data);
        mContactsAdapter.notifyDataSetChanged();
        mT9HelperFragment.setVisibility(View.GONE);

        // Set empty to no contacts found when result is empty.
        if (data != null && data.getCount() == 0) {
            mEmptyView.setText(R.string.dialer_no_contacts_found_message);
        }

        refreshUi();
        mT9HelperFragment.setVisibility(View.GONE);
    }

    private void refreshUi() {
        if (ContactsSyncTask.getProgress().isComplete()) {
            mContactsProcessingContainer.setVisibility(View.GONE);
            mT9HelperFragment.setVisibility(View.VISIBLE);
        } else {
            mContactsProcessingContainer.setVisibility(View.VISIBLE);
            mT9HelperFragment.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        clearContactList();
    }

    @Override
    public void onBackPressed() {
        if (isContactsExpanded()) {
            showDialpad();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isContactsExpanded() {
        return mDialer.getVisibility() != View.VISIBLE;
    }

    /**
     * Updates the contacts sync progress bar.
     *
     */
    private static class ProgressUpdater implements Runnable {

        private final DialerActivity mActivity;

        /**
         * The frequency to check the current status of the contacts import.
         */
        private static final int POLL_TIME_MS = 500;

        ProgressUpdater(DialerActivity activity) {
            mActivity = activity;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (ContactsSyncTask.getProgress().isComplete()) {
                    mActivity.runOnUiThread(mActivity::refreshUi);
                    return;
                }

                mActivity.runOnUiThread(() -> {
                    mActivity.mProgressText.setText(mActivity.getString(
                            R.string.dialer_contacts_not_processed,
                            ContactsSyncTask.getProgress().getProcessed(),
                            ContactsSyncTask.getProgress().getTotal())
                    );
                    mActivity.mProgressBar.setProgress(getCompletionPercentage());
                });

                try {
                    Thread.sleep(POLL_TIME_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private int getCompletionPercentage() {
            return (int) (((float) ContactsSyncTask.getProgress().getProcessed() / (float) ContactsSyncTask.getProgress().getTotal()) * 100);
        }
    }
}