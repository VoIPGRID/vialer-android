package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.tamir7.contacts.Contact;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.dialer.DialerActivity;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.IconHelper;
import com.voipgrid.vialer.util.TimeUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class CallRecordViewHolder extends RecyclerView.ViewHolder  implements View.OnClickListener {

    CircleImageView contactIcon;
    TextView contactName, contactInformation;
    ImageButton callButton;
    CallRecord callRecord;

    private Activity activity;
    private CachedContacts contacts;
    private static boolean mCallAlreadySetup = false;

    CallRecordViewHolder(@NonNull View view) {
        super(view);
        contactIcon = view.findViewById(R.id.text_view_contact_icon);
        contactName = view.findViewById(R.id.text_view_contact_name);
        contactInformation = view.findViewById(R.id.text_view_contact_information);
        callButton = view.findViewById(R.id.call_record_call_button);
    }

    public CallRecordViewHolder provideDependencies(Activity activity, CachedContacts contacts) {
        this.activity = activity;
        this.contacts = contacts;

        return this;
    }

    /**
     * Update this view based on the call record.
     *
     * @param callRecord
     */
    public void update(CallRecord callRecord) {
        this.callRecord = callRecord;
        String number = callRecord.getThirdPartyNumber();
        Contact contact = contacts.getContact(number);
        contactIcon.setImageBitmap(getContactImage(number, contact));
        setNumberAndCallButtonVisibility(callRecord, contact);
        contactInformation.setCompoundDrawablesWithIntrinsicBounds(getIcon(callRecord), 0, 0, 0);
        contactInformation.setText(DateUtils.getRelativeDateTimeString(
                activity,
                TimeUtils.convertToSystemTime(callRecord.getCallDate()),
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.YEAR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_TIME
        ));
    }

    private void setNumberAndCallButtonVisibility(CallRecord callRecord, @Nullable Contact contact) {
        if(callRecord.isAnonymous()) {
            contactName.setText(activity.getString(R.string.supressed_number));
            callButton.setVisibility(View.GONE);
            return;
        }

        callButton.setOnClickListener(this);

        if (contact != null) {
            contactName.setText(contact.getDisplayName());
            callButton.setVisibility(View.VISIBLE);
            return;
        }

        contactName.setText(callRecord.getThirdPartyNumber());
        callButton.setVisibility(View.VISIBLE);
    }

    /**
     * Attempt to find the contact's image, falling back to a generic icon if
     * none is available.
     *
     * @param number
     * @param contact
     * @return
     */
    private Bitmap getContactImage(String number, @Nullable Contact contact) {
        if (contact == null) {
            return IconHelper.getCallerIconBitmap("", number, 0);
        }

        Bitmap contactImage = contacts.getContactImage(number);

        if (contactImage != null) {
            return contactImage;
        }

        return IconHelper.getCallerIconBitmap(contact.getDisplayName().substring(0, 1), number, 0);
    }

    /**
     * Determine the correct call icon to display.
     *
     * @param callRecord
     * @return
     */
    private int getIcon(CallRecord callRecord) {
        if (callRecord.getDirection().equals(CallRecord.DIRECTION_OUTBOUND)) {
            return R.drawable.ic_outgoing;
        }

        return callRecord.getDuration() == 0 ? R.drawable.ic_incoming_missed : R.drawable.ic_incoming;
    }

    @Override
    public void onClick(View view) {
        if (callRecord.getThirdPartyNumber() != null && !mCallAlreadySetup) {
            mCallAlreadySetup = true;
            DialHelper.fromActivity(activity).callNumber(callRecord.getThirdPartyNumber(), "");
            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(
                    DialerActivity.LAST_DIALED, callRecord.getThirdPartyNumber()).apply();
        }

        mCallAlreadySetup = false;
    }
}
