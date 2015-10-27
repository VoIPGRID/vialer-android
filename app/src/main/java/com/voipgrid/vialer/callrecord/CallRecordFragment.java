package com.voipgrid.vialer.callrecord;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.voipgrid.vialer.EmptyView;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.CallRecord;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.VoipGridResponse;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.DialHelper;
import com.voipgrid.vialer.util.Storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

/**
 * A fragment representing a list of call records.
 */
public class CallRecordFragment extends ListFragment implements
        Callback<VoipGridResponse<CallRecord>>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = CallRecordFragment.class.getSimpleName();

    private static final String ARG_FILTER = "filter";

    public static final String FILTER_MISSED_RECORDS = "missed-records";

    private OnFragmentInteractionListener mListener;

    private CallRecordAdapter mAdapter;

    private List<CallRecord> mCallRecords = new ArrayList<>();

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mDialerWarning;

    private ConnectivityHelper mConnectivityHelper;

    private Storage mStorage;

    private AnalyticsHelper mAnalyticsHelper;

    private String mFilter;
    private Preferences mPreferences;

    private boolean mHaveNetworkRecords;

    public static CallRecordFragment newInstance(String filter) {
        CallRecordFragment fragment = new CallRecordFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_FILTER, filter);
        fragment.setArguments(arguments);
        return fragment;
    }

    private class AsyncCallRecordLoader extends AsyncTask<Void, Void, List<CallRecord>> {
        private Storage<CallRecord[]> mStorage;
        public AsyncCallRecordLoader() {
            mStorage = new Storage<>(getActivity());
        }

        protected List<CallRecord> doInBackground(Void args[]) {
            CallRecord[] records = mStorage.get(CallRecord[].class);
            Log.d(TAG, "Loaded callrecords from cache");
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
        private Storage<CallRecord[]> mStorage;
        public AsyncCallRecordSaver(List<CallRecord> records) {
            mRecords = records;
            mStorage = new Storage<CallRecord[]>(getActivity());
        }

        protected Void doInBackground(Void args[]) {
            CallRecord[] records = new CallRecord[mRecords.size()];
            mRecords.toArray(records);
            mStorage.save(records);
            Log.d(TAG, "Saved callrecords to cache");
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

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE));

        mStorage = new Storage(getActivity());

        mFilter = getArguments().getString(ARG_FILTER);
        
        mPreferences = new Preferences(getContext());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_records, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDialerWarning = (TextView) view.findViewById(R.id.dialer_warning);

        mAdapter = new CallRecordAdapter(view.getContext(), mCallRecords);

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
        /* check network state and show a warning if needed */
        mDialerWarning.setVisibility(View.VISIBLE);
        if(!mConnectivityHelper.hasNetworkConnection()) {
            mDialerWarning.setText(R.string.dialer_warning_no_connection);
            mDialerWarning.setTag(getString(R.string.dialer_warning_no_connection_message));
        } else if(!mConnectivityHelper.hasFastData() && mPreferences.canUseSip()) {
            mDialerWarning.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarning.setTag(getString(R.string.dialer_warning_a_b_connect_connectivity_message));
        } else if(!mStorage.has(PhoneAccount.class) && mPreferences.canUseSip()) {
            mDialerWarning.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarning.setTag(getString(R.string.dialer_warning_a_b_connect_account_message));
        } else {
            mDialerWarning.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.

            new DialHelper(getActivity(),mStorage,mConnectivityHelper, mAnalyticsHelper).
                    callNumber(mAdapter.getItem(position).getDialedNumber(), "");
        }
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
        SystemUser systemUser = (SystemUser) mStorage.get(SystemUser.class);
        Api api = ServiceGenerator.createService(
                mConnectivityHelper,
                Api.class,
                getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(
                        getContext(),
                        systemUser.getEmail(),
                        systemUser.getPassword()
                ))
        );

        api.getRecentCalls(50, 0, CallRecord.getLimitDate(), this);
    }

    /* Load from local cache first */
    private void loadCallRecordsFromCache() {
        new AsyncCallRecordLoader().execute();
    }

    private void displayCachedRecords(List<CallRecord> records) {
        if (mHaveNetworkRecords) return; /* cached results arrived later than the network results */
        displayCallRecords(records);
    }

    private void displayCallRecords(List<CallRecord> records) {
        List<CallRecord> filtered = filter(records);
        if(filtered != null && filtered.size() > 0) {
            mAdapter.setCallRecords(filtered);
            setEmptyView(null, false);
        } else if(mAdapter.getCount() == 0) {
            /* List is empty, but adapter view may not be. Since this method is only called in success cases, ignore this case.   */
            setEmptyView(
                    new EmptyView(getActivity(), getString(R.string.empty_view_default_message)),
                    true
            );
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Callback on succesfull request. When call records are available add them to the adapter
     * otherwise display the empty view with an error message.
     * @param voipGridResponse
     * @param response
     */
    @Override
    public void success(VoipGridResponse<CallRecord> voipGridResponse, Response response) {
        mHaveNetworkRecords = true;
        List<CallRecord> records = voipGridResponse.getObjects();
        displayCallRecords(records);
        /* save the records to cache, if there are any */
        if (filter(records).size() > 0) {
            new AsyncCallRecordSaver(records).execute();
        }
    }


    /**
     * Apply filter on call records or return the call record list when the filter is null
     * @param callRecords
     * @return
     */
    private List<CallRecord> filter(List<CallRecord> callRecords) {
        if(mFilter != null && callRecords != null && callRecords.size() > 0) {
            List<CallRecord> filtered = new ArrayList<>();
            if(mFilter.equals(FILTER_MISSED_RECORDS)) {
                for(int i=0, size = callRecords.size(); i < size; i++) {
                    CallRecord callRecord = callRecords.get(i);
                    if(callRecord.getDirection().equals(CallRecord.DIRECTION_INBOUND) &&
                            callRecord.getDuration() == 0) {
                        filtered.add(callRecord);
                    }
                }
            }
            return filtered;
        }
        return callRecords;
    }

    /**
     * Request failed. Display the empty view with an error message.
     * @param error
     */
    @Override
    public void failure(RetrofitError error) {
        String message = getString(R.string.empty_view_default_message);
        Response response = error.getResponse();
        if(response != null && response.getStatus() == 401) { //UNAUTHORIZED
            message = getString(R.string.empty_view_unauthorized_message);
        }
        if (mAdapter.getCount() == 0) {
            setEmptyView(new EmptyView(getActivity(), message), true);
        } else {
            /* adapter has cached values and we're not about to overwrite them. However, we do want to notify the user. */
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
        public void onFragmentInteraction(String id);
    }

}
