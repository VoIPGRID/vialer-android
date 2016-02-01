package com.voipgrid.vialer.dialer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.WarningActivity;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.contacts.ContactsManager;
import com.voipgrid.vialer.contacts.ContactsPermission;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.t9.ContactCursorLoader;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.Storage;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * DialerActivity with a numpad view and functionality to perform a T9 contacts search
 */
public class DialerActivity extends AppCompatActivity implements
        View.OnClickListener,
        KeyPadView.OnKeyPadClickListener,
        AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private ListView mContactsListView;
    private NumberInputEditText mNumberInputEditText;
    private SimpleCursorAdapter mContactsAdapter = null;
    private TextView mDialerWarning;
    private TextView mEmptyView;
    private ViewGroup mKeyPadViewContainer;

    private AnalyticsHelper mAnalyticsHelper;
    private ConnectivityHelper mConnectivityHelper;
    private Preferences mPreferences;
    private Storage mStorage;

    private String t9Query;
    private boolean mHasPermission;
    private boolean mAskForPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);

        /* set the AnalyticsHelper */
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );

        mStorage = new Storage(this);

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mPreferences = new Preferences(this);

        mDialerWarning = (TextView) findViewById(R.id.dialer_warning);
        mContactsListView = (ListView) findViewById(R.id.list_view);
        mEmptyView = (TextView) findViewById(R.id.message);
        mEmptyView.setText("");

        // This should be called before setupContactParts.
        setupKeypad();

        Intent intent = getIntent();
        String type = intent.getType();

        // Sadly HTC fails to include the mime-type in the intent in 4.4.2.
        Uri contactUri = intent.getData();

        mHasPermission = ContactsPermission.hasPermission(this);
        mAskForPermission = true;
        // Check for contact permissions before doing contact related work.
        if (mHasPermission) { // Handling this intent is only needed when we have contact permissions.

            // This should be called after setupKeyPad.
            setupContactParts();

            /**
             * The app added a "Vialer call <number>" to the native contacts app. clicking this
             * opens the app with the appname's profile and the data necessary for opening the app.
             */
            if((!TextUtils.isEmpty(type) && type.equals(getString(R.string.profile_mimetype)))
                    || contactUri != null) {
                // Redirect user to login.
                // This can be needed when a user logs out and in the logged out state
                // presses call with vialer in a contact.
                if(!mStorage.has(SystemUser.class)) {
                    startActivity(new Intent(this, SetupActivity.class));
                    finish();
                }

                Cursor cursor = getContentResolver().query(contactUri, new String[] {
                        ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME,
                        ContactsContract.Data.DATA3 }, null, null, null);
                cursor.moveToFirst();
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA3));
                mNumberInputEditText.setNumber(number);
                cursor.close();
            }
        } else {
            // Set the empty view for the contact list to inform the user this functionality will
            // not work.
            mEmptyView.setText(getString(R.string.permission_contact_dialer_list_message));
            mContactsListView.setEmptyView(mEmptyView);
        }
    }

    /**
     * Setup the keypad and add a empty onInputChanged listener.
     */
    private void setupKeypad() {
        KeyPadView keyPadView = (KeyPadView) findViewById(R.id.key_pad_view);
        keyPadView.setOnKeyPadClickListener(this);

        mKeyPadViewContainer = (ViewGroup) findViewById(R.id.key_pad_container);

        mNumberInputEditText = (NumberInputEditText) findViewById(R.id.number_input_edit_text);

        mNumberInputEditText.setOnInputChangedListener(new NumberInputEditText.OnInputChangedListener() {
            @Override
            public void onInputChanged(String phoneNumber) {
                // Keep this empty. A implemented version will be set if we have contact permissions
                // be a empty one is required if we do not have permission.
            }
        });
    }

    /**
     * Setup the ContactsList, filter the list on the given number and add a onInputChanged
     * listener for the T9 contact search.
     */
    private void setupContactParts() {
        // Setup the list view.
        setupContactsListView();
        // Replace the empty listener set in setupKeypad with the T9 search function.
        mNumberInputEditText.setOnInputChangedListener(new NumberInputEditText.OnInputChangedListener() {
            @Override
            public void onInputChanged(String phoneNumber) {
                t9Query = phoneNumber;
                // Input field is cleared so clear contact list.
                if (phoneNumber.length() == 0) {
                    clearContactList();
                } else {
                    // Load contacts matching t9Query.
                    getSupportLoaderManager().restartLoader(1, null, DialerActivity.this).forceLoad();
                }
            }
        });
    }

    /**
     * Function to clear the contact list in the dialer.
     */
    private void clearContactList() {
        mContactsAdapter.swapCursor(null);
        mContactsAdapter.notifyDataSetChanged();
        mEmptyView.setText("");
    }

    /**
     * Function to setup the listview and cursor adapter.
     */
    private void setupContactsListView() {
        mContactsAdapter = new SimpleCursorAdapter(
                getBaseContext(),
                R.layout.list_item_contact,
                null,
                new String[]{"name", "photo", "number"},
                new int[]{
                        R.id.text_view_contact_name,
                        R.id.text_view_contact_icon,
                        R.id.text_view_contact_information
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
                    Uri photoUri = Uri.parse(cursor.getString(columnIndex));
                    // Open a photo inputStream given contact uri.
                    InputStream photoInputStream =
                            ContactsContract.Contacts.openContactPhotoInputStream(
                                    getContentResolver(), photoUri);
                    if (photoInputStream != null) {
                        // Decode it to a bitmap when the stream is opened successfully.
                        Bitmap bm = BitmapFactory.decodeStream(photoInputStream);
                        ((CircleImageView) view).setImageBitmap(bm);
                    } else {
                        // Otherwise set it to transparent for reusability reasons.
                        ((CircleImageView) view).setImageResource(android.R.color.transparent);
                    }
                    return true; //true because the data was bound to the icon view
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

        // Permission changed since last accessing this Activity.
        if (mHasPermission != ContactsPermission.hasPermission(this)){
            // Force a recreate of the Activity to reflect the new permission.
            Intent intent = getIntent();
            startActivity(intent);
            finish();
        }

        // If we don't have permission we need to ask for it.
        if (!mHasPermission){
            // We need to avoid a permission loop.
            if (mAskForPermission) {
                mAskForPermission = false;
                ContactsPermission.askForPermission(this);
            }
        }

        // Set the warming for lacking connection types.
        mDialerWarning.setVisibility(View.VISIBLE);
        if(!mConnectivityHelper.hasNetworkConnection()) {
            mDialerWarning.setText(R.string.dialer_warning_no_connection);
            mDialerWarning.setTag(getString(R.string.dialer_warning_no_connection_message));
        } else if(!mConnectivityHelper.hasFastData() && mPreferences.canUseSip()) {
            mDialerWarning.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarning.setTag(getString(R.string.dialer_warning_a_b_connect_connectivity_message));
        } else if(!mStorage.has(PhoneAccount.class) && mPreferences.canUseSip()) {
            mDialerWarning.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarning.setTag(getString(R.string.dialer_warning_a_b_connect_account_message));
        } else {
            mDialerWarning.setVisibility(View.GONE);
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
                ContactsManager.requestContactSync(this);
                // Reload Activity to reflect new permission.
                Intent intent = getIntent();
                startActivity(intent);
                finish();
            }
        }
    }

    /**
     * Initiate an outgoing call by starting CallActivity and pass a SipUri based on the number
     *
     * @param number number to call
     * @param contactName contact name to display
     */
    public void onCallNumber(String number, String contactName) {
        new DialHelper(this, mStorage, mConnectivityHelper, mAnalyticsHelper)
                .callNumber(PhoneNumberUtils.format(number), contactName);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_call :
                String phoneNumber = mNumberInputEditText.getNumber();
                if(phoneNumber!= null && !phoneNumber.isEmpty()) {
                    onCallNumber(PhoneNumberUtils.format(phoneNumber), null);
                }
                break;
            case R.id.button_dialpad :
                toggleKeyPadView();
                break;
            case R.id.dialer_warning :
                Intent intent = new Intent(this, WarningActivity.class);
                intent.putExtra(WarningActivity.TITLE, ((TextView) view).getText());
                intent.putExtra(WarningActivity.MESSAGE, (String) view.getTag());
                startActivity(intent);
                break;
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

    @Override
    public void onKeyPadButtonClick(String digit, String chars) {
        mNumberInputEditText.add(digit);
    }

    private void toggleKeyPadView() {
        boolean visible = mKeyPadViewContainer.getVisibility() == View.VISIBLE;

        mKeyPadViewContainer.setVisibility(visible ? View.GONE : View.VISIBLE);

        findViewById(R.id.button_dialpad).setVisibility(visible ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_call).setVisibility(!visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(scrollState == SCROLL_STATE_TOUCH_SCROLL &&
                mKeyPadViewContainer.getVisibility() == View.VISIBLE) {
            toggleKeyPadView();
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
        // Set new data on adapter and notify data changed.
        mContactsAdapter.swapCursor(data);
        mContactsAdapter.notifyDataSetChanged();
        // Set empty to no contacts found when result is empty.
        if (data != null && data.getCount() == 0) {
            mEmptyView.setText(getString(R.string.dialer_no_contacts_found_message));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        clearContactList();
    }
}
