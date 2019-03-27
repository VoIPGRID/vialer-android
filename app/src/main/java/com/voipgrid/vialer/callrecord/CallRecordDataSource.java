package com.voipgrid.vialer.callrecord;

import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.VoipGridResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import retrofit2.Response;

public class CallRecordDataSource extends PositionalDataSource<CallRecord> {

    private final VoipgridApi mVoipgridApi;

    private int code;

    CallRecordDataSource(VoipgridApi voipgridApi) {
        this.mVoipgridApi = voipgridApi;
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
                call = mVoipgridApi.getRecentCalls(size, startPosition, CallRecord.getLimitDate()).execute();
            } else {
                call = mVoipgridApi.getRecentCallsForLoggedInUser(size, startPosition, CallRecord.getLimitDate()).execute();
            }

            code = call.code();

            if (!call.isSuccessful()) {
                return new ArrayList<>();
            }

            if (call.body() == null) {
                return new ArrayList<>();
            }

            List<CallRecord> records = call.body().getObjects();

            total = call.body().getMeta().getTotalCount();

            return records;
        } catch (IOException e) {
            code = 500;
            return null;
        }
    }

    public int getLastCode() {
        return code;
    }
}
