package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Cache;

/**
 * Adapter to display the call records
 */
public class CallRecordAdapter extends PagedListAdapter<CallRecord, CallRecordAdapter.CallRecordViewHolder> {

    private static final DiffUtil.ItemCallback<CallRecord> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CallRecord>() {
                @Override
                public boolean areItemsTheSame(CallRecord oldItem, CallRecord newItem) {
                    return oldItem.getId() == newItem.getId();
                }
                @Override
                public boolean areContentsTheSame(CallRecord oldItem, CallRecord newItem) {
                    return areItemsTheSame(oldItem, newItem);
                }
            };

    private final Context context;
    private final CachedContacts contacts;
    private Activity activity;

    private boolean mCallAlreadySetup = false;

    public CallRecordAdapter(Context context, CachedContacts contacts) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.contacts = contacts;
    }

    @NonNull
    @Override
    public CallRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_call_record, parent, false);

        return new CallRecordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CallRecordViewHolder holder, int position) {
        CallRecord callRecord = getItem(position);

        if (callRecord == null) return;

        holder.update(callRecord);
    }

    public CallRecordAdapter setActivity(Activity activity) {
        this.activity = activity;

        return this;
    }

    class CallRecordViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CircleImageView contactIcon;
        TextView contactName, contactInformation;
        ImageButton callButton;
        CallRecord callRecord;

        CallRecordViewHolder(@NonNull View view) {
            super(view);
            contactIcon = view.findViewById(R.id.text_view_contact_icon);
            contactName = view.findViewById(R.id.text_view_contact_name);
            contactInformation = view.findViewById(R.id.text_view_contact_information);
            callButton = view.findViewById(R.id.call_record_call_button);
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
                    context,
                    TimeUtils.convertToSystemTime(callRecord.getCallDate()),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.YEAR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_TIME
            ));
        }

        private void setNumberAndCallButtonVisibility(CallRecord callRecord, @Nullable Contact contact) {
            if(callRecord.isAnonymous()) {
                contactName.setText(context.getString(R.string.supressed_number));
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
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(DialerActivity.LAST_DIALED, callRecord.getThirdPartyNumber()).apply();
            }

            mCallAlreadySetup = false;
        }
    }
}
