package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.voipgrid.vialer.EmptyView;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;
import com.voipgrid.vialer.util.NetworkStateViewHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private OnFragmentInteractionListener mListener;
    private CallRecordAdapter mAdapter;
    private List<CallRecord> mCallRecords = new ArrayList<>();
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private AnalyticsHelper mAnalyticsHelper;
    private ConnectivityHelper mConnectivityHelper;
    private JsonStorage mJsonStorage;
    private NetworkStateViewHelper mNetworkStateViewHelper;

    private String mFilter;
    private boolean mHaveNetworkRecords;

    public static CallRecordFragment newInstance(String filter) {
        CallRecordFragment fragment = new CallRecordFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_FILTER, filter);
        fragment.setArguments(arguments);
        return fragment;
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
            displayCachedRecords(records);
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
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CallRecordFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* set the AnalyticsHelper */
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getActivity().getApplication()).getDefaultTracker()
        );

        mConnectivityHelper = ConnectivityHelper.get(getActivity());

        mJsonStorage = new JsonStorage(getActivity());
        mFilter = getArguments().getString(ARG_FILTER);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_call_records, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNetworkStateViewHelper = new NetworkStateViewHelper(
                getActivity(), (TextView) view.findViewById(R.id.dialer_warning));

        mAdapter = new CallRecordAdapter(getActivity(), mCallRecords);

        /* setup swipe refresh layout */
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.color_refresh));
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

        loadCallRecordsFromApi();
        loadCallRecordsFromCache();
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        mNetworkStateViewHelper.updateNetworkStateView();
        mNetworkStateViewHelper.startListening();
        mAdapter.mCallAlreadySetup = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            try {
                mListener = (OnFragmentInteractionListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(
                        activity.toString() + " must implement OnFragmentInteractionListener"
                );
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mNetworkStateViewHelper.stopListening();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
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
        SystemUser systemUser = (SystemUser) mJsonStorage.get(SystemUser.class);
        Api api = ServiceGenerator.createService(
                getContext(),
                Api.class,
                getString(R.string.api_url),
                systemUser.getEmail(),
                systemUser.getPassword());

        Call<VoipGridResponse<CallRecord>> call = api.getRecentCalls(
                50, 0, CallRecord.getLimitDate()
        );
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
        } else if(mAdapter.getCount() == 0) {
            // List is empty, but adapter view may not be. Since this method is only called in
            // success cases, ignore this case.
            String emptyText;

            if (mFilter != null && mFilter.equals(FILTER_MISSED_RECORDS)) {
                emptyText = getString(R.string.empty_view_missed_message);
            } else {
                emptyText = getString(R.string.empty_view_default_message);
            }

            setEmptyView(
                    new EmptyView(getActivity(), emptyText),
                    true
            );
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Apply filter on call records or return the call record list when the filter is null
     * @param callRecords List of CallRecord instances
     * @return List of CallRecord instances.
     */
    private List<CallRecord> filter(List<CallRecord> callRecords) {
        if (mFilter != null && callRecords != null && callRecords.size() > 0) {
            List<CallRecord> filtered = new ArrayList<>();

            if (mFilter.equals(FILTER_MISSED_RECORDS)) {
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
    public void onResponse(Call<VoipGridResponse<CallRecord>> call,
                           Response<VoipGridResponse<CallRecord>> response) {
        if (response.isSuccess() && response.body() != null) {
            mHaveNetworkRecords = true;
            List<CallRecord> records = response.body().getObjects();
            displayCallRecords(records);
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
        String message = getString(R.string.empty_view_default_message);

        // Check if authorized.
        if(response != null && (response.code() == 401 || response.code() == 403)) {
            message = getString(R.string.empty_view_unauthorized_message);
        }
        if (mAdapter.getCount() == 0) {
            setEmptyView(new EmptyView(getActivity(), message), true);
        } else {
            /* adapter has cached values and we're not about to overwrite them. However,
            we do want to notify the user. */
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setEmptyView(EmptyView emptyView, boolean visible) {
        if(getView() != null) {
            ViewGroup view = (ViewGroup) getView().findViewById(R.id.empty_view);
            if (view.getChildCount() > 0) {
                view.removeAllViews();
            }
            if (emptyView != null) {
                view.addView(emptyView);
            }
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String id);
    }

}
