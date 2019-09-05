package com.voipgrid.vialer.callrecord;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

<<<<<<< HEAD
import com.voipgrid.vialer.Preferences;
=======
import com.voipgrid.vialer.EmptyView;
>>>>>>> 0be2efa... Refactored all shared preference interactions to be via a User object, rather than having to import ambiguous classes like JsonStorage. This creates a much more fluent, readable api throughout the code.
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.util.BroadcastReceiverManager;
import com.voipgrid.vialer.util.NetworkUtil;

import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.Unbinder;


public class CallRecordFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener, Observer<PagedList<CallRecord>>, MissedCalls.Callback {

    @Inject CallRecordDataSourceFactory factory;
    @Inject CallRecordAdapter adapter;
    @Inject MissedCalls mMissedCalls;
    @Inject MissedCallsAdapter missedCallsAdapter;
    @Inject NetworkUtil mNetworkUtil;
    @Inject BroadcastReceiverManager mBroadcastReceiverManager;

    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeContainer;
    @BindView(R.id.call_records) RecyclerView mRecyclerView;
    @BindView(R.id.show_missed_calls_only_switch) Switch showMissedCallsOnlySwitch;
    @BindView(R.id.call_records_container) View mCallRecordsContainer;
    @BindView(R.id.call_records_unavailable_view) CallRecordsUnavailableView callRecordsUnavailableView;

    private Unbinder unbinder;
    private NetworkChangeReceiver mNetworkChangeReceiver = new NetworkChangeReceiver();

    private boolean fetchCallsFromEntireAccount = false;

    /**
     * The number of call records to fetch with each HTTP request.
     */
    private static final int CALL_RECORDS_PAGE_SIZE = 50;

    /**
     * Display only the user's calls.
     *
     */
    public static CallRecordFragment mine() {
        return new CallRecordFragment();
    }

    /**
     * Display the user's calls for the entire account.
     *
     */
    public static CallRecordFragment all() {
        CallRecordFragment fragment = new CallRecordFragment();
        fragment.fetchCallsFromEntireAccount = true;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_records, null);
        VialerApplication.get().component().inject(this);
        unbinder = ButterKnife.bind(this, view);

        if (fetchCallsFromEntireAccount) {
            factory.fetchCallsFromEntireAccount();
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMissedCallsAdapter();
        setupCallRecordAdapter();
        setupSwipeContainer();
        setupRecyclerView();
        showAppropriateRecords();
        showMissedCallsOnlySwitch.setChecked(User.userPreferences.getDisplayMissedCallsOnly());
        onRefresh();
    }

    private void showAppropriateRecords() {
        mRecyclerView.setAdapter(showMissedCalls() ? missedCallsAdapter : adapter);
    }

    private void setupCallRecordAdapter() {
        PagedList.Config config = new PagedList.Config.Builder().setPageSize(CALL_RECORDS_PAGE_SIZE).build();
        new LivePagedListBuilder(factory, config).build().observe(this, this);
        adapter.setActivity(getActivity());
    }

    private void setupMissedCallsAdapter() {
        missedCallsAdapter.setActivity(getActivity());
    }

    private void setupSwipeContainer() {
        swipeContainer.setColorSchemeColors(getResources().getColor(R.color.color_refresh));
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.post(() -> setRefreshing(true));
    }

    /**
     * Helper method to set refreshing on the swipe container but to avoid NPE if it is null.
     *
     * @param refreshing
     */
    private void setRefreshing(boolean refreshing) {
        if (swipeContainer == null) return;

        swipeContainer.setRefreshing(refreshing);
    }

    private void setupRecyclerView() {
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(VialerApplication.get());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onRefresh() {
        if (!mNetworkUtil.isOnline()) {
            mCallRecordsContainer.setVisibility(View.GONE);
            callRecordsUnavailableView.noInternetConnection();
            return;
        } else {
            callRecordsUnavailableView.hide();
        }

        mCallRecordsContainer.setVisibility(View.VISIBLE);

        if (showMissedCalls()) {
            mMissedCalls.fetch(this, fetchCallsFromEntireAccount);
            return;
        }

        if (factory == null || factory.getPostLiveData() == null || factory.getPostLiveData().getValue() == null) {
            return;
        }

        factory.getPostLiveData().getValue().invalidate();
    }

    @Override
    public void onChanged(PagedList<CallRecord> callRecords) {
        setRefreshing(false);

        CallRecordDataSource value = factory.getPostLiveData().getValue();

        int code = (value == null ? 500 : value.getLastCode());

        if (!String.valueOf(code).startsWith("2")) {
            handleFailedRequest(code);
            return;
        }

        callRecordsUnavailableView.hide();

        if (callRecords.isEmpty()) {
            callRecordsUnavailableView.unavailable();
            return;
        }

        adapter.submitList(callRecords);
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (!visible) return;

        if (showMissedCallsOnlySwitch != null) {
            redrawList();
            showMissedCallsOnlySwitch.setChecked(User.userPreferences.getDisplayMissedCallsOnly());
        }
    }

    /**
     * Force the recycler view to redraw the table, updating the relative timestamps.
     *
     */
    private void redrawList() {
        mRecyclerView.setAdapter(mRecyclerView.getAdapter());
    }

    @Override
    public void missedCallsHaveBeenRetrieved(List<CallRecord> missedCallRecords) {
        if (!isAdded()) return;

        setRefreshing(false);

        if (missedCallRecords.isEmpty()) {
            callRecordsUnavailableView.noMissedCalls();
            return;
        }

        callRecordsUnavailableView.hide();
        missedCallsAdapter.setRecords(missedCallRecords);
    }

    @Override
    public void attemptToRetrieveMissedCallsDidFail(int code) {
        handleFailedRequest(code);
    }

    private void handleFailedRequest(int code) {
        if (!isAdded()) return;

        setRefreshing(false);

        getActivity().runOnUiThread(() -> {
            if (code == 401 || code == 403) {
                callRecordsUnavailableView.permissionsFailed();
            }
            else {
                callRecordsUnavailableView.unavailable();
            }
        });
    }

    private boolean showMissedCalls() {
        return User.userPreferences.getDisplayMissedCallsOnly();
    }

    @OnCheckedChanged(R.id.show_missed_calls_only_switch)
    void missedCallsSwitchWasChanged(CompoundButton missedCallsSwitch, boolean checked) {
        User.userPreferences.setDisplayMissedCallsOnly(checked);
        showAppropriateRecords();
        setRefreshing(true);
        onRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        redrawList();
        mBroadcastReceiverManager.registerReceiverViaGlobalBroadcastManager(mNetworkChangeReceiver, "android.net.conn.CONNECTIVITY_CHANGE");
    }

    @Override
    public void onPause() {
        super.onPause();
        mBroadcastReceiverManager.unregisterReceiver(mNetworkChangeReceiver);
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCallRecordsContainer.getVisibility() != View.VISIBLE) {
                onRefresh();
            }
        }
    }
}
