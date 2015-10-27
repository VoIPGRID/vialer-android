package com.voipgrid.vialer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private MatrixCursor mMatrixCursor;

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
            Cursor cursor = getContentResolver().query(intent.getData(), new String[] {
                    ContactsContract.CommonDataKinds.StructuredName.PHONETIC_NAME,
                    ContactsContract.Data.DATA2 }, null, null, null);
            cursor.moveToFirst();

            number = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA2));
            mNumberInputEditText.setNumber(number);

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
        mContactsAdapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.list_item_contact,
                null,
                new String[] { "name","photo","details"},
                new int[] { R.id.text_view_contact_name,R.id.text_view_contact_icon,R.id.text_view_contact_information}, 0);

        // Getting reference to listview
        ListView lstContacts = (ListView) findViewById(R.id.list_view);

        lstContacts.setOnItemClickListener(this);

        // Setting the adapter to listview
        lstContacts.setAdapter(mContactsAdapter);

        // Creating an AsyncTask object to retrieve and load listview with contacts
        ListViewContactsLoader listViewContactsLoader = new ListViewContactsLoader();

        // Starting the AsyncTask process to retrieve and load listview with contacts
        listViewContactsLoader.execute();

        lstContacts.setOnItemClickListener(this);
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

        List<String> numbers = Arrays.asList(
                ((MatrixCursor) parent.getItemAtPosition(position))
                        .getString(4)
                        .replace("[", "")
                        .replace("]", "")
                        .split(","));

        int size = numbers.size();
        if(size == 1) {
            onCallNumber(number, name);
        } else if(size > 1) {
            //Toast.makeText(DialerActivity.this, Arrays.toString(numbers.toArray()), Toast.LENGTH_SHORT).show();
            chooseNumber(name, numbers);
        }
    }

    public void chooseNumber(final String name, List<String> numbers) {
        final ArrayAdapter<String> adapter = new ArrayAdapter(
                this,
                android.R.layout.select_dialog_item);
        adapter.addAll(numbers);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialer_choose_number);

        builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onCallNumber(PhoneNumberUtils.format(adapter.getItem(which)), name);
                dialog.dismiss();
            }
        });

        builder.show();
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
        new ListViewContactsLoader().execute(phoneNumber);
    }


    /** An AsyncTask class to retrieve and load listview with contacts */
    private class ListViewContactsLoader extends AsyncTask<CharSequence, Void, Cursor> {

        @Override
        protected Cursor doInBackground(CharSequence... params) {

            mMatrixCursor = new MatrixCursor(new String[] { "_id","name","photo","details", "numbers"} );

            CharSequence constraint = null;
            if(params != null && params.length > 0) {
                constraint = params[0];
            }

            Uri contactsUri = ContactsContract.Contacts.CONTENT_URI;

            String[] t9Lookup = getResources().getStringArray(R.array.t9lookup);
            StringBuilder builder = new StringBuilder();

            String constraintString = "";
            if(constraint != null && constraint.length() > 0) {
                constraintString = constraint.toString();
            }

            for (int i = 0, constraintLength = constraintString.length(); i < constraintLength; i++) {
                char c = constraintString.charAt(i);

                if (c >= '0' && c <= '9') {
                    builder.append(t9Lookup[c - '0']);
                } else if (c == '+') {
                    builder.append(c);
                } else {
                    builder.append("[" + Character.toLowerCase(c) + Character.toUpperCase(c) + "]");
                }
            }

            String whereStatement = ContactsContract.Contacts.HAS_PHONE_NUMBER + "=?" +
                    " AND " + "replace(" + ContactsContract.Contacts.DISPLAY_NAME +  ", ' ', '')" + " GLOB " + "?";
            String whereArguments[] = new String[]{"1", "*" + builder.toString() + "*"};
            String order = ContactsContract.Contacts.DISPLAY_NAME + " ASC ";

            Cursor contactsCursor = getContentResolver().query(contactsUri, null, whereStatement, whereArguments, order);

            if(contactsCursor.moveToFirst()){
                do{
                    long contactId = contactsCursor.getLong(contactsCursor.getColumnIndex("_ID"));

                    Uri dataUri = ContactsContract.Data.CONTENT_URI;

                    // Querying the table ContactsContract.Data to retrieve individual items like
                    // home phone, mobile phone, work email etc corresponding to each contact
                    Cursor dataCursor = getContentResolver().query(dataUri, null,
                            ContactsContract.Data.CONTACT_ID + "=" + contactId,
                            null, null);

                    String displayName="";

                    String photoPath = null;

                    List<String> numbers = new ArrayList();

                    String number = null;

                    if(dataCursor.moveToFirst()) {
                        // Getting Display Name
                        displayName = dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                        byte[] photoByte = null;

                        do {
                            // Getting Photo
                            if (dataCursor.getString(dataCursor.getColumnIndex("mimetype")).equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)) {
                                photoByte = dataCursor.getBlob(dataCursor.getColumnIndex("data15"));

                                if (photoByte != null) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(photoByte, 0, photoByte.length);

                                    // Getting Caching directory
                                    File cacheDirectory = getBaseContext().getCacheDir();

                                    // Temporary file to store the contact image
                                    File tmpFile = new File(cacheDirectory.getPath() + "/wpta_" + contactId + ".png");

                                    // The FileOutputStream to the temporary file
                                    try {
                                        FileOutputStream fOutStream = new FileOutputStream(tmpFile);

                                        // Writing the bitmap to the temporary file as png file
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);

                                        // Flush the FileOutputStream
                                        fOutStream.flush();

                                        //Close the FileOutputStream
                                        fOutStream.close();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        bitmap.recycle();
                                    }
                                    photoPath = tmpFile.getPath();
                                }
                            }
                        } while (dataCursor.moveToNext());
                    }

                    if(dataCursor.moveToFirst()){
                        do{
                            // Getting Phone numbers
                            if(dataCursor.getString(dataCursor.getColumnIndex("mimetype")).equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
                                if(dataCursor.getInt(dataCursor.getColumnIndex("data2")) > 0) {
                                    numbers.add(dataCursor.getString(dataCursor.getColumnIndex("data1")));
                                } else {
                                    number = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                                }
                            }
                        }while(dataCursor.moveToNext());

                    }
                    mMatrixCursor.addRow(new Object[]{Long.toString(contactId), displayName, photoPath, number, Arrays.toString(numbers.toArray())});
                }while(contactsCursor.moveToNext());
            }
            return mMatrixCursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            // Setting the cursor containing contacts to listview
            mContactsAdapter.swapCursor(result);
            mContactsAdapter.notifyDataSetChanged();
        }
    }
}
