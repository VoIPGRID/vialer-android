package com.voipgrid.vialer.callrecord;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.contacts.Contacts;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
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
import butterknife.Unbinder;


public class CallRecordFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener, Observer<PagedList<CallRecord>> {

    @Inject CallRecordDataSourceFactory factory;
    @Inject CallRecordAdapter adapter;

    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeContainer;
    @BindView(R.id.call_records) RecyclerView mRecyclerView;

    private Unbinder unbinder;

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

        setupSwipeContainer();
        setupRecyclerView();

        PagedList.Config config = new PagedList.Config.Builder().setPageSize(CALL_RECORDS_PAGE_SIZE).build();
        new LivePagedListBuilder(factory, config).build().observe(this, this);
    }

    private void setupSwipeContainer() {
        swipeContainer.setColorSchemeColors(getResources().getColor(R.color.color_refresh));
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.post(() -> swipeContainer.setRefreshing(true));
    }

    private void setupRecyclerView() {
        adapter.setActivity(getActivity());
        mRecyclerView.setAdapter(adapter);
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
        if (factory == null || factory.getPostLiveData() == null || factory.getPostLiveData().getValue() == null) {
            return;
        }

        factory.getPostLiveData().getValue().invalidate();
    }

    @Override
    public void onChanged(PagedList<CallRecord> callRecords) {
        adapter.submitList(callRecords);
        swipeContainer.setRefreshing(false);
    }

    public void fragmentIsVisible() {
    }
}
