package com.voipgrid.vialer.callrecord;

import com.voipgrid.vialer.api.VoipgridApi;
import com.voipgrid.vialer.api.models.CallRecord;

import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;

public class CallRecordDataSourceFactory extends DataSource.Factory<Integer, CallRecord> {

    private final VoipgridApi mVoipgridApi;
    private MutableLiveData<CallRecordDataSource> postLiveData;

    private boolean fetchCallsFromEntireAccount = false;

    public CallRecordDataSourceFactory(VoipgridApi voipgridApi) {
        this.mVoipgridApi = voipgridApi;
    }

    @Override
    public DataSource<Integer, CallRecord> create() {
        CallRecordDataSource dataSource = new CallRecordDataSource(mVoipgridApi);
        if (fetchCallsFromEntireAccount) {
            dataSource.fetchCallsFromEntireAccount();
        }
        postLiveData = new MutableLiveData<>();
        postLiveData.postValue(dataSource);
        return dataSource;
    }

    CallRecordDataSourceFactory fetchCallsFromEntireAccount() {
        fetchCallsFromEntireAccount = true;

        return this;
    }

    MutableLiveData<CallRecordDataSource> getPostLiveData() {
        return postLiveData;
    }
}
