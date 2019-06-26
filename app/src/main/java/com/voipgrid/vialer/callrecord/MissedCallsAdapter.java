package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.api.models.CallRecord;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MissedCallsAdapter extends RecyclerView.Adapter<CallRecordViewHolder> {

    private Activity activity;
    private final CachedContacts contacts;

    private List<CallRecord> records = new ArrayList<>();

    public MissedCallsAdapter(CachedContacts contacts) {
        this.contacts = contacts;
    }

    @NonNull
    @Override
    public CallRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_call_record, parent, false);

        return new CallRecordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CallRecordViewHolder holder, int position) {
        CallRecord callRecord = records.get(position);

        if (callRecord == null) return;

        holder.setActivity(activity);
        holder.update(callRecord);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setRecords(List<CallRecord> objects) {
        this.records = objects;
        notifyDataSetChanged();
    }

    public MissedCallsAdapter setActivity(Activity activity) {
        this.activity = activity;

        return this;
    }
}
