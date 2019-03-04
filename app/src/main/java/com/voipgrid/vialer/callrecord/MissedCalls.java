package com.voipgrid.vialer.callrecord;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.VoipGridResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class MissedCalls {

    /**
     * This class will make a single request to the api for the number of calls listed here
     * and will look through every record for missed calls.
     *
     */
    private static final int NUMBER_OF_RECORDS_TO__PARSE_FOR_MISSED_CALLS = 500;

    private final Api api;

    public MissedCalls(Api api) {
        this.api = api;
    }

    /**
     * Fetch the missed calls from the api, missed calls are calculated client side rather than via an
     * api end-point which means that we have to query a large number of them to find them.
     *
     * @param callback To receive a response when the missed calls have been fetched
     * @param fetchCallsFromEntireAccount The
     */
    void fetch(Callback callback, boolean fetchCallsFromEntireAccount) {
        createHttpCall(fetchCallsFromEntireAccount).enqueue(
                new retrofit2.Callback<VoipGridResponse<CallRecord>>() {
                    @Override
                    public void onResponse(Call<VoipGridResponse<CallRecord>> call,
                            Response<VoipGridResponse<CallRecord>> response) {
                        if (!response.isSuccessful()) {
                            callback.attemptToRetrieveMissedCallsDidFail(response.code());
                            return;
                        }

                        callback.missedCallsHaveBeenRetrieved(filterMissedCalls(response.body().getObjects()));
                    }

                    @Override
                    public void onFailure(Call<VoipGridResponse<CallRecord>> call, Throwable t) {
                        callback.attemptToRetrieveMissedCallsDidFail(500);
                    }
                });
    }

    /**
     * Create the HTTP "Call" object based on whether we should be querying the api for the entire
     * account or not.
     *
     * @param fetchCallsFromEntireAccount If TRUE, all records will be fetched.
     * @return
     */
    private Call<VoipGridResponse<CallRecord>> createHttpCall(boolean fetchCallsFromEntireAccount) {
        if (fetchCallsFromEntireAccount) {
            return api.getRecentCalls(NUMBER_OF_RECORDS_TO__PARSE_FOR_MISSED_CALLS, 0, CallRecord.getLimitDate());
        } else {
            return api.getRecentCallsForLoggedInUser(NUMBER_OF_RECORDS_TO__PARSE_FOR_MISSED_CALLS, 0, CallRecord.getLimitDate());
        }
    }

    /**
     * Work through the list of CallRecords and determine which are missed
     * and which are not.
     *
     * @param records
     * @return
     */
    private List<CallRecord> filterMissedCalls(List<CallRecord> records) {
        List<CallRecord> missedCalls = new ArrayList<>();

        for (CallRecord record : records) {
            if (record.wasMissed()) {
                missedCalls.add(record);
            }
        }

        return missedCalls;
    }

    public interface Callback {
        void missedCallsHaveBeenRetrieved(List<CallRecord> missedCallRecords);

        void attemptToRetrieveMissedCallsDidFail(int code);
    }
}
