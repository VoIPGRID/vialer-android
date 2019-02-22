package com.voipgrid.vialer.callrecord;

import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.models.CallRecord;

import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;

public class CallRecordDataSourceFactory extends DataSource.Factory<Integer, CallRecord> {

    private final Api api;
    private MutableLiveData<CallRecordDataSource> postLiveData;

    private boolean fetchCallsFromEntireAccount = false;

    public CallRecordDataSourceFactory(Api api) {
        this.api = api;
    }

    @Override
    public DataSource<Integer, CallRecord> create() {
        CallRecordDataSource dataSource = new CallRecordDataSource(api);
        if (fetchCallsFromEntireAccount) {
            dataSource.fetchCallsFromEntireAccount();
        }
        postLiveData = new MutableLiveData<>();
        postLiveData.postValue(dataSource);
        return dataSource;
    }

    public CallRecordDataSourceFactory fetchCallsFromEntireAccount() {
        fetchCallsFromEntireAccount = true;

        return this;
    }

    public MutableLiveData<CallRecordDataSource> getPostLiveData() {
        return postLiveData;
    }
}
