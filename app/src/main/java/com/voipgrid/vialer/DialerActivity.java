package com.voipgrid.vialer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.contacts.ContactsManager;
import com.voipgrid.vialer.contacts.ContactsPermission;
import com.voipgrid.vialer.onboarding.SetupActivity;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.Storage;
import com.voipgrid.vialer.t9.ListViewContactsLoader;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * DialerActivity with a numpad view and functionality to perform a T9 contacts search
 */
public class DialerActivity extends AppCompatActivity implements
        View.OnClickListener,
        KeyPadView.OnKeyPadClickListener,
        AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener {

    private final static String LOG_TAG = DialerActivity.class.getSimpleName();

    private boolean mHasPermission;
    private boolean mAskForPermission;

    private ConnectivityHelper mConnectivityHelper;

    private TextView mDialerWarning;
    private ListView mContactsListView;

    private Storage mStorage;

    private AnalyticsHelper mAnalyticsHelper;

    private NumberInputEditText mNumberInputEditText;

    private ViewGroup mKeyPadViewContainer;

    private SimpleCursorAdapter mContactsAdapter = null;
    private Preferences mPreferences;


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
        TextView emptyView = (TextView) findViewById(R.id.message);

        // This should be called before setupContactParts.
        setupKeypad();

        Intent intent = getIntent();
        String type = intent.getType();

        // Sadly HTC fails to include the mime-type in the intent in 4.4.2.
        Uri contactUri = intent.getData();

        mHasPermission = ContactsPermission.hasPermission(this);
        mAskForPermission = true;
        // Check for contact permissions before doing contact related work.
        if (mHasPermission) {
            // Handling this intent is only needed when we have contact permissions.
            String number = null;
            emptyView.setText(getString(R.string.dialer_no_contacts_found_message));
            mContactsListView.setEmptyView(emptyView);

            // This should be called after setupKeyPad.
            setupContactParts();

            /**
             * The app added a "Vialer call <number>" to the native contacts app. clicking this
             * opens the app with the appname's profile and the data necessary for opening the app.
             */
            if((!TextUtils.isEmpty(type) && type.equals(getString(R.string.profile_mimetype))) || contactUri != null) {
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
                number = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA3));
                mNumberInputEditText.setNumber(number);
                cursor.close();
            }
        } else {
            // Set the empty view for the contact list to inform the user this functionality will
            // not work.
            emptyView.setText(getString(R.string.permission_contact_dialer_list_message));
            mContactsListView.setEmptyView(emptyView);
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

        Cursor cursor = this.getContentResolver()
                .query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    ContactsContract.Data.HAS_PHONE_NUMBER + " = 1",
                    null,
                    null
                );

        // Due to performance we only support <= 750 contacts for T9 search.
        if (cursor == null || cursor.getCount() <= 750){
            // Replace the empty listener set in setupKeypad with the T9 search function.
            mNumberInputEditText.setOnInputChangedListener(new NumberInputEditText.OnInputChangedListener() {
                @Override
                public void onInputChanged(String phoneNumber) {
                    new ListViewContactsLoader(getBaseContext(), mContactsAdapter).execute(phoneNumber);
                }
            });
        }
    }

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
             * Binds the Cursor column defined by the specified index to the specified view
             */
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text_view_contact_icon) {
                    // The ListViewContactsLoader class stores a contact uri for which
                    // we can retrieve a photo.
                    Uri photoUri = Uri.parse(cursor.getString(columnIndex));
                    // open a photo inputStream given contact uri.
                    InputStream photoInputStream =
                            ContactsContract.Contacts.openContactPhotoInputStream(
                                    getContentResolver(), photoUri);
                    if (photoInputStream != null) {
                        // decode it to a bitmap when the stream is opened successfully
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

        // Getting reference to listview
        mContactsListView.setOnScrollListener(this);
        mContactsListView.setOnItemClickListener(this);
        mContactsListView.setAdapter(mContactsAdapter);

        // Creating an AsyncTask object to retrieve and load listview with contacts
        ListViewContactsLoader listViewContactsLoader = new ListViewContactsLoader(
                getBaseContext(), mContactsAdapter);
        // Starting the AsyncTask process to retrieve and load listview with contacts
        listViewContactsLoader.execute();
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == this.getResources().getInteger(R.integer.contact_permission_request_code)) {
            boolean allPermissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // ContactSync.
                Log.d(LOG_TAG, "Starting ContactSync after getting contact permissions");
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
        String name = ((TextView) view.findViewById(R.id.text_view_contact_name)).getText().toString();
        String number = ((TextView) view.findViewById(R.id.text_view_contact_information)).getText().toString();
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
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }
}
