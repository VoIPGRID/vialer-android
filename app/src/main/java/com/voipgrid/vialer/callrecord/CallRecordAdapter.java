package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.github.tamir7.contacts.Contact;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.contacts.Contacts;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.permissions.ContactsPermission;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.IconHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;
import com.voipgrid.vialer.util.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter to display the call records
 */
public class CallRecordAdapter extends BaseAdapter implements View.OnClickListener {

    private List<CallRecord> mCallRecords;

    private Activity mActivity;

    public boolean mCallAlreadySetup = false;

    private Map<String, Contact> mCachedContacts = new HashMap<>();
    private Map<String, Bitmap> mCachedContactImages = new HashMap<>();

    @Inject Contacts mContacts;

    /**
     * Construct a new CallRecordAdapter
     * @param activity
     * @param callRecords
     */
    CallRecordAdapter(Activity activity, List<CallRecord> callRecords) {
        mActivity = activity;
        mCallRecords = callRecords;
        VialerApplication.get().component().inject(this);
    }

    /**
     * Set call records to the adapter
     * @param callRecords
     */
    void setCallRecords(List<CallRecord> callRecords) {
        mCallRecords = callRecords;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCallRecords.size();
    }

    @Override
    public CallRecord getItem(int position) {
        return mCallRecords.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        // Get the call record.
        CallRecord callRecord = getItem(position);
        Contact contact = null;
        String number = "";
        // Default resource for direction.
        int resource = 0;

        if(callRecord != null) {
            // Get the direction from the call record.
            String direction = callRecord.getDirection();

            // Set the drawable resource.
            if (direction.equals(CallRecord.DIRECTION_OUTBOUND)) {
                number = callRecord.getDialedNumber();
                resource = R.drawable.ic_outgoing;
            } else if (direction.equals(CallRecord.DIRECTION_INBOUND)) {
                number = callRecord.getCaller();
                if (callRecord.getDuration() == 0) {
                    resource = R.drawable.ic_incoming_missed;
                } else {
                    resource = R.drawable.ic_incoming;
                }
            }

            // Get possible name or null.
            if (ContactsPermission.hasPermission(mActivity)) {
                if (!mCachedContacts.containsKey(number)) {
                    mCachedContacts.put(number, mContacts.getContactByPhoneNumber(number));
                }
                contact = mCachedContacts.get(number);
            }
        }

        if(convertView == null) {
            // Inflate the layout.
            LayoutInflater inflater = mActivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.list_item_call_record, parent, false);
        }

        Bitmap bitmapImage = null;

        if (contact != null) {

            if (!mCachedContactImages.containsKey(number)) {
                mCachedContactImages.put(number, mContacts.getContactImageByPhoneNumber(number));
            }

            bitmapImage = mCachedContactImages.get(number);

            if (bitmapImage == null) {
                bitmapImage = IconHelper.getCallerIconBitmap(contact.getDisplayName().substring(0, 1), number, 0);
            }
        } else {
            bitmapImage = IconHelper.getCallerIconBitmap("", number, 0);
        }

        View photoView = convertView.findViewById(R.id.text_view_contact_icon);

        ((CircleImageView) photoView).setImageBitmap(bitmapImage);
        // Set up the ViewHolder.
        viewHolder = new ViewHolder();
        viewHolder.title = (TextView) convertView.findViewById(R.id.text_view_contact_name);
        viewHolder.information = (TextView) convertView.findViewById(
                R.id.text_view_contact_information
        );

        ImageButton callButton = convertView.findViewById(R.id.call_record_call_button);

        // Store the holder with the view.
        convertView.setTag(viewHolder);

        if(callRecord != null) {
            // Set name or number as text.
            if(number != null && PhoneNumberUtils.isAnonymousNumber(callRecord.getCaller())) {
                viewHolder.title.setText(convertView.getContext().getString(R.string.supressed_number));
                // Make call button invisible.
                callButton.setVisibility(View.GONE);
            } else if (contact != null) {
                viewHolder.title.setText(contact.getDisplayName());
                callButton.setOnClickListener(this);
                callButton.setVisibility(View.VISIBLE);
            } else {
                viewHolder.title.setText(number);
                callButton.setOnClickListener(this);
                callButton.setVisibility(View.VISIBLE);
            }

            // Set the compound drawable to the view.
            viewHolder.information.setCompoundDrawablesWithIntrinsicBounds(resource, 0, 0, 0);

            viewHolder.information.setText(DateUtils.getRelativeDateTimeString(
                    mActivity,
                    TimeUtils.convertToSystemTime(callRecord.getCallDate()),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.YEAR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_TIME
            ));

        }
        return convertView;
    }

    @Override
    public void onClick(View view) {
        // Get the position of the list item the buttons is clicked for.
        View parentRow = (View) view.getParent();
        ListView listView = (ListView) parentRow.getParent();
        final int position = listView.getPositionForView(parentRow);

        // Get the call record.
        CallRecord callRecord = getItem(position);
        String direction = callRecord.getDirection();
        String numberToCall = null;

        // Determine direction and the number we need to call.
        if (direction.equals(CallRecord.DIRECTION_OUTBOUND)) {
            numberToCall = callRecord.getDialedNumber();
        } else if (direction.equals(CallRecord.DIRECTION_INBOUND)) {
            numberToCall = callRecord.getCaller();
        }

        if (numberToCall != null && !mCallAlreadySetup) {
            mCallAlreadySetup = true;
            DialHelper.fromActivity(mActivity).callNumber(numberToCall, "");
            PreferenceManager.getDefaultSharedPreferences(mActivity).edit().putString(DialerActivity.LAST_DIALED, numberToCall).apply();
        }
            mCallAlreadySetup = false;
    }

    static class ViewHolder {
        TextView title;
        TextView information;
    }
}
