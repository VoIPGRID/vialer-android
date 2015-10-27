package com.voipgrid.vialer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.Storage;
import com.voipgrid.vialer.t9.ListViewContactsLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * DialerActivity with a numpad view and functionality to perform a T9 contacts search
 */
public class DialerActivity extends AppCompatActivity implements
        View.OnClickListener,
        KeyPadView.OnKeyPadClickListener,
        AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener,
        NumberInputEditText.OnInputChangedListener {

    private final static String TAG = DialerActivity.class.getSimpleName();

    private ConnectivityHelper mConnectivityHelper;

    private TextView mDialerWarning;

    private Storage mStorage;

    private AnalyticsHelper mAnalyticsHelper;

    private NumberInputEditText mNumberInputEditText;

    private ViewGroup mKeyPadViewContainer;

    private SimpleCursorAdapter mContactsAdapter;
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

        setupKeypad();

        setupContactsListView();

        Intent intent = getIntent();
        String type = intent.getType();

        String number = null;
        if(!TextUtils.isEmpty(type) && type.equals(getString(R.string.profile_mimetype))) {
            /**
             * The app added a "Vialer call <number>" to the native contacts app.
             *
             */
            Cursor cursor = getContentResolver().query(intent.getData(), new String[] {
                    ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME,
                    ContactsContract.Data.DATA2 }, null, null, null);
            cursor.moveToFirst();
            number = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA2));
            mNumberInputEditText.setNumber(number);
            cursor.close();
        }

        mContactsAdapter.getFilter().filter(number);
    }

    private void setupKeypad() {

        KeyPadView keyPadView = (KeyPadView) findViewById(R.id.key_pad_view);
        keyPadView.setOnKeyPadClickListener(this);

        mKeyPadViewContainer = (ViewGroup) findViewById(R.id.key_pad_container);

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnScrollListener(this);

        mNumberInputEditText = (NumberInputEditText) findViewById(R.id.number_input_edit_text);
        mNumberInputEditText.setOnInputChangedListener(this);
    }

    private void setupContactsListView() {
        mContactsAdapter = new SimpleCursorAdapter(
                getBaseContext(),
                R.layout.list_item_contact,
                null,
                new String[] { "name", "photo", "number"},
                new int[] {
                        R.id.text_view_contact_name,
                        R.id.text_view_contact_icon,
                        R.id.text_view_contact_information
                },
                0
        );

        mContactsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
            /** Binds the Cursor column defined by the specified index to the specified view */
            public boolean setViewValue(View view, Cursor cursor, int columnIndex){
                if(view.getId() == R.id.text_view_contact_icon) {
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
        ListView lstContacts = (ListView) findViewById(R.id.list_view);
        lstContacts.setOnItemClickListener(this);
        lstContacts.setAdapter(mContactsAdapter);

        // Creating an AsyncTask object to retrieve and load listview with contacts
        ListViewContactsLoader listViewContactsLoader = new ListViewContactsLoader(
                getBaseContext(), mContactsAdapter);
        // Starting the AsyncTask process to retrieve and load listview with contacts
        listViewContactsLoader.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    @Override
    public void onInputChanged(String phoneNumber) {
        new ListViewContactsLoader(getBaseContext(), mContactsAdapter).execute(phoneNumber);
    }
}
