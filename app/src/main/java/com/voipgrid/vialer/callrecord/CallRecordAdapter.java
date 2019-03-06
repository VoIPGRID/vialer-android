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
public class CallRecordAdapter extends PagedListAdapter<CallRecord, CallRecordViewHolder> {

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

    private final CachedContacts contacts;
    private Activity activity;

    public CallRecordAdapter(CachedContacts contacts) {
        super(DIFF_CALLBACK);
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

        holder.provideDependencies(activity, contacts);
        holder.update(callRecord);
    }

    public CallRecordAdapter setActivity(Activity activity) {
        this.activity = activity;

        return this;
    }
}
