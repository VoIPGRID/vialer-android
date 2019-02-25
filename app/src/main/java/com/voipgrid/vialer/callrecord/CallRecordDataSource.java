package com.voipgrid.vialer.callrecord;

import android.util.Log;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.VoipGridResponse;

import java.io.IOException;
import java.util.List;


import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import retrofit2.Response;

public class CallRecordDataSource extends PositionalDataSource<CallRecord> {

    private final Api api;

    CallRecordDataSource(Api api) {
        this.api = api;
    }

    /**
     * The total number of records that are available to fetch from the api.
     */
    private int total;

    /**
     * If set to TRUE, calls from the entire account will be fetched.
     *
     */
    private boolean fetchCallsFromEntireAccount = false;

    /**
     * Instruct the data source to fetch calls from the entire account.
     *
     */
    CallRecordDataSource fetchCallsFromEntireAccount() {
        fetchCallsFromEntireAccount = true;
        return this;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<CallRecord> callback) {
        List<CallRecord> records = fetch(params.pageSize, params.requestedStartPosition);

        if (records == null) return;

        callback.onResult(records, params.requestedStartPosition, total);
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<CallRecord> callback) {
        List<CallRecord> records = fetch(params.loadSize, params.startPosition);

        if (records == null) return;

        callback.onResult(records);
    }

    /**
     * Query the VoIPGRID api for call records.
     *
     * @param size
     * @param startPosition
     * @return
     */
    private List<CallRecord> fetch(int size, int startPosition) {
        try {
            Response<VoipGridResponse<CallRecord>> call;

            if (fetchCallsFromEntireAccount) {
                call = api.getRecentCalls(size, startPosition, CallRecord.getLimitDate()).execute();
            } else {
                call = api.getRecentCallsForLoggedInUser(size, startPosition, CallRecord.getLimitDate()).execute();
            }

            if (!call.isSuccessful()) {
                return null;
            }

            if (call.body() == null) {
                return null;
            }

            List<CallRecord> records = call.body().getObjects();

            total = call.body().getMeta().getTotalCount();

            return records;
        } catch (IOException e) {
            return null;
        }
    }
}
