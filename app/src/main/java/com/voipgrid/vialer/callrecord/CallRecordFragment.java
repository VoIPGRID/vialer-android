package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.ListFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import com.voipgrid.vialer.EmptyView;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A fragment representing a list of call records.
 */
public class CallRecordFragment extends ListFragment implements
        Callback<VoipGridResponse<CallRecord>>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_FILTER = "filter";
    public static final String FILTER_MISSED_RECORDS = "missed-records";

    private CallRecordAdapter mAdapter;
    private List<CallRecord> mCallRecords = new ArrayList<>();
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Inject ConnectivityHelper mConnectivityHelper;
    @Inject JsonStorage mJsonStorage;
    @Inject Preferences mPreferences;

    private String mFilter;
    private boolean mHaveNetworkRecords;
    private Unbinder mUnbinder;

    @BindView(R.id.show_calls_for_whole_client_switch) CompoundButton mShowCallsForWholeClientSwitch;

    public static CallRecordFragment newInstance(String filter) {
        CallRecordFragment fragment = new CallRecordFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_FILTER, filter);
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CallRecordFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VialerApplication.get().component().inject(this);
        mFilter = getArguments().getString(ARG_FILTER);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_call_records, null);
        mUnbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new CallRecordAdapter(getActivity(), mCallRecords);

        /* setup swipe refresh layout */
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.color_refresh));
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
        loadCallRecordsFromApi();
        loadCallRecordsFromCache();
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if wifi should be turned back on.
        if(ConnectivityHelper.mWifiKilled) {
            mConnectivityHelper.useWifi(getActivity(), true);
            ConnectivityHelper.mWifiKilled = false;
        }
        mAdapter.mCallAlreadySetup = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mShowCallsForWholeClientSwitch != null) {
            mShowCallsForWholeClientSwitch.setChecked(mPreferences.getDisplayCallRecordsForWholeClient());
        }
    }

    @OnCheckedChanged(R.id.show_calls_for_whole_client_switch)
    void showCallsForWholeClientSwitchWasChanged(CompoundButton view, boolean checked) {
        Log.e("TEST123", "checke change");
        mPreferences.setDisplayCallRecordsForWholeClient(checked);
        clearCallRecordCache();
        onRefresh();
    }

    /**
     * Completely clear the call record cache so call records must be fetched from the API.
     *
     */
    private void clearCallRecordCache() {
        mJsonStorage.remove(CallRecord[].class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    /**
     * Perform a refresh action
     */
    @Override
    public void onRefresh() {
        loadCallRecordsFromApi();
    }

    private void loadCallRecordsFromApi() {
        mHaveNetworkRecords = false;

        Api api = ServiceGenerator.createApiService(getContext());

        Call<VoipGridResponse<CallRecord>> call;

        if (mPreferences.getDisplayCallRecordsForWholeClient()) {
             call = api.getRecentCalls(50, 0, CallRecord.getLimitDate());
        } else {
            call = api.getRecentCallsForLoggedInUser(50, 0, CallRecord.getLimitDate());
        }

        call.enqueue(this);
    }

    /* Load from local cache first */
    private void loadCallRecordsFromCache() {
        new AsyncCallRecordLoader().execute();
    }

    private void displayCachedRecords(List<CallRecord> records) {
        // Cached results arrived later than the network results.
        if (mHaveNetworkRecords) {
            return;
        }
        displayCallRecords(records);
    }

    private void displayCallRecords(List<CallRecord> records) {
        List<CallRecord> filtered = filter(records);

        if(filtered != null && filtered.size() > 0) {
            mAdapter.setCallRecords(filtered);
            setEmptyView(null, false);
        } else if(filtered.size() == 0) {
            mAdapter.setCallRecords(filtered);

            setEmptyView(
                    new EmptyView(
                            getActivity(),
                            isShowingMissedCallRecords() ? getString(R.string.empty_view_missed_message) : getString(R.string.empty_view_default_message)
                    ),
                    true
            );
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Check if this call record fragment is meant to be showing missed call records rather than all call records.
     *
     * @return TRUE if fragment is showing missed calls, otherwise FALSE.
     */
    private boolean isShowingMissedCallRecords() {
        return mFilter != null && mFilter.equals(FILTER_MISSED_RECORDS);
    }

    /**
     * Apply filter on call records or return the call record list when the filter is null
     * @param callRecords List of CallRecord instances
     * @return List of CallRecord instances.
     */
    private List<CallRecord> filter(List<CallRecord> callRecords) {
        if (mFilter != null && callRecords != null && callRecords.size() > 0) {
            List<CallRecord> filtered = new ArrayList<>();

            if (isShowingMissedCallRecords()) {
                for (int i=0, size = callRecords.size(); i < size; i++) {
                    CallRecord callRecord = callRecords.get(i);

                    if (callRecord.getDirection().equals(CallRecord.DIRECTION_INBOUND) &&
                            callRecord.getDuration() == 0) {
                        filtered.add(callRecord);
                    }
                }
            }
            return filtered;
        }
        return callRecords;
    }

    @Override
    public void onResponse(@NonNull Call<VoipGridResponse<CallRecord>> call,
                           @NonNull Response<VoipGridResponse<CallRecord>> response) {
        if (response.isSuccessful() && response.body() != null) {
            mHaveNetworkRecords = true;
            List<CallRecord> records = response.body().getObjects();

            if(getActivity() != null && isAdded()) {
                displayCallRecords(records);
            }

            // Save the records to cache, if there are any.
            if (filter(records).size() > 0) {
                new AsyncCallRecordSaver(records).execute();
            }
        } else {
            failedFeedback(response);
        }
    }

    @Override
    public void onFailure(Call call, Throwable t) {
        failedFeedback(null);
    }

    private void failedFeedback(Response response) {
        if (getActivity() == null) {
            new Logger(CallRecordFragment.class).e("CallRecordFragment is no longer attached to an activity");
            return;
        }

        String message = getString(R.string.empty_view_default_message);

        // Check if authorized.
        if(response != null && (response.code() == 401 || response.code() == 403)) {
            message = getString(R.string.empty_view_unauthorized_message);
        }
        if (mAdapter.getCount() == 0) {
            setEmptyView(new EmptyView(getActivity(), message), true);
        } else {
            // Adapter has cached values and we're not about to overwrite them. However,
            // we do want to notify the user.
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setEmptyView(EmptyView emptyView, boolean visible) {
        if(getView() != null) {
            ViewGroup view = getView().findViewById(R.id.empty_view);
            if (view.getChildCount() > 0) {
                view.removeAllViews();
            }
            if (emptyView != null) {
                view.addView(emptyView);
            }
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private class AsyncCallRecordLoader extends AsyncTask<Void, Void, List<CallRecord>> {
        private JsonStorage<CallRecord[]> mJsonStorage;
        public AsyncCallRecordLoader() {
            mJsonStorage = new JsonStorage<>(getActivity());
        }

        protected List<CallRecord> doInBackground(Void args[]) {
            CallRecord[] records = mJsonStorage.get(CallRecord[].class);
            if (records != null) {
                return Arrays.asList(records);
            } else {
                return new ArrayList<>();
            }
        }

        protected void onPostExecute(List<CallRecord> records) {
            if(isAdded()){
                // Only display Records when the fragment is still attached to an activity.
                displayCachedRecords(records);
            }
        }
    }

    private class AsyncCallRecordSaver extends AsyncTask<Void, Void, Void> {
        private List<CallRecord> mRecords;

        public AsyncCallRecordSaver(List<CallRecord> records) {
            mRecords = records;
        }

        protected Void doInBackground(Void args[]) {
            CallRecord[] records = new CallRecord[mRecords.size()];
            mRecords.toArray(records);
            mJsonStorage.save(records);
            return null;
        }
    }
}
