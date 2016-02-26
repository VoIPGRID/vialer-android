package com.voipgrid.vialer.twostepcall;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.TwoStepCallStatus;
import com.voipgrid.vialer.models.ClickToDialParams;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

public class TwoStepCallActivity extends Activity implements View.OnClickListener, Callback<Object> {

    public static final String NUMBER_TO_CALL = "number-to-call";
    private boolean cancelCall = false;

    private TextView mDialerWarningTextView;
    private TextView mStatusTextView;

    private AnalyticsHelper mAnalyticsHelper;
    private Api mApi;
    private ConnectivityHelper mConnectivityHelper;
    private Preferences mPreferences;
    private JsonStorage mJsonStorage;
    private SystemUser mSystemUser;
    private TwoStepCallTask mTwoStepCallTask;
    private TwoStepCallView mTwoStepCallView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_step_call);

        /* set the AnalyticsHelper */
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );

        mJsonStorage = new JsonStorage(this);

        mPreferences = new Preferences(this);

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mDialerWarningTextView = (TextView) findViewById(R.id.dialer_warning);

        mSystemUser = (SystemUser) new JsonStorage(this).get(SystemUser.class);

        mApi = ServiceGenerator.createService(
                this,
                mConnectivityHelper,
                Api.class,
                getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(
                        this,
                        mSystemUser.getEmail(),
                        mSystemUser.getPassword())
                )
        );

        String numberToCall = getIntent().getStringExtra(NUMBER_TO_CALL);

        mTwoStepCallTask = new TwoStepCallTask(mApi, mSystemUser.getMobileNumber(), numberToCall);
        mTwoStepCallTask.execute();

        mStatusTextView = ((TextView) findViewById(R.id.status_text_view));

        mTwoStepCallView = (TwoStepCallView) findViewById(R.id.two_step_call_view);
        mTwoStepCallView.setOutgoingNumber(mSystemUser.getOutgoingCli());
        mTwoStepCallView.setNumberA(mSystemUser.getMobileNumber());
        mTwoStepCallView.setNumberB(numberToCall);
        updateStateView(TwoStepCallUtils.STATE_INITIAL);

        mAnalyticsHelper.send(
                getString(R.string.analytics_dimension),
                getString(R.string.analytics_event_category_call),
                getString(R.string.analytics_event_action_outbound),
                getString(R.string.analytics_event_label_connect_a_b)
        );

        ((TextView) findViewById(R.id.name_text_view)).setText(numberToCall);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDialerWarningTextView.setVisibility(View.VISIBLE);
        if(!mConnectivityHelper.hasNetworkConnection()) {
            mDialerWarningTextView.setText(R.string.dialer_warning_no_connection);
            mDialerWarningTextView.setTag(getString(R.string.dialer_warning_no_connection_message));
        } else if(!mConnectivityHelper.hasFastData() && mPreferences.canUseSip()) {
            mDialerWarningTextView.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarningTextView
                    .setTag(getString(R.string.dialer_warning_a_b_connect_connectivity_message));
        } else if(!mJsonStorage.has(PhoneAccount.class) && mPreferences.canUseSip()) {
            mDialerWarningTextView.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarningTextView
                    .setTag(getString(R.string.dialer_warning_a_b_connect_account_message));
        } else {
            mDialerWarningTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.two_step_button_hangup:
                // Already cancelling a call.
                if (cancelCall) {
                    break;
                }

                String callId = mTwoStepCallTask.getCallId();

                if (callId != null) {  // callId exists so cancel right away
                    mApi.twoStepCallCancel(callId, this);
                    cancelCall = true;
                } else {  // set cancelCall so the two way task will cancel it once it's created
                    cancelCall = true;
                }

                // Update view to reflect cancelling process.
                updateStateView(TwoStepCallUtils.STATE_CANCELLING);
                break;
        }
    }

    @Override
    public void success(Object o, Response response) {
        // Response code of successful cancel request.
        if (response.getStatus() == 204) {
            // Cancel the status update task.
            mTwoStepCallTask.cancel(false);
            // Update view.
            updateStateView(TwoStepCallUtils.STATE_CANCELLED);
            cancelButtonVisible(false);

            // Redirect user back to previous activity after 5 seconds.
            finishWithDelay();
        }
    }


    @Override
    public void failure(RetrofitError error) {
        // No longer cancelling a call.
        cancelCall = false;
        // Failed to cancel call, enable button again.
        cancelButtonVisible(true);
    }

    /**
     * Function to set the cancel buttons visibility.
     * @param visible
     */
    private void cancelButtonVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        findViewById(R.id.two_step_button_hangup).setVisibility(visibility);
    }

    /**
     * Function to update the root view to given state.
     * @param state Present in TwoStepCallUtils.
     */
    private void updateStateView(String state) {
        mTwoStepCallView.setState(state);
        mStatusTextView.setText(TwoStepCallUtils.getStateText(this, state));
        if (TwoStepCallUtils.getFailedStates().contains(state)) {
            cancelButtonVisible(false);
        }
    }

    /**
     * Finish the current activity with a delay.
     */
    private void finishWithDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 3000);
    }


    public class TwoStepCallTask extends AsyncTask<Void, String, Boolean> {
        private Api mApi;

        private String mCallId;
        private String mNumberA, mNumberB;

        public TwoStepCallTask(Api api, String numberA, String numberB) {
            mApi = api;
            mNumberA = numberA;
            mNumberB = numberB;
        }

        public String getCallId() {
            return mCallId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            TwoStepCallStatus status;
            try {
                status = mApi.twoStepCall(new ClickToDialParams(mNumberA, mNumberB));
            } catch (RetrofitError error) {
                int statusCode = error.getResponse().getStatus();
                if (statusCode == 400) {
                    publishProgress(TwoStepCallUtils.STATE_INVALID_NUMBER);
                } else {
                    publishProgress(TwoStepCallUtils.STATE_FAILED);
                }
                return false;
            }
            mCallId = status.getCallId();
            // If cancel has been pressed before the call was made cancel it here.
            if (cancelCall) {
                mApi.twoStepCallCancel(mCallId, TwoStepCallActivity.this);
            }
            // We continue with the task because if the cancel fails there is no way to inform
            // the user with the status of the uncancelled call.
            mNumberA = status.getaNumber();
            mNumberB = status.getbNumber();
            String message = status.getStatus();
            return handleMessage(message, mNumberA, mNumberB);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            // Called after doInBackGround and NOT called when the task is cancelled.
            finishWithDelay();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if(values.length > 0 && !cancelCall) {
                // We do not want to update the view if we are in the process of cancelling a call.
                updateStateView(values[0]);
            }
        }

        private boolean handleMessage(String... values) {
            if(values.length > 0) {
                String message = values[0];
                if (message != null) {
                    values[0] = TwoStepCallUtils.getViewState(message);
                }
                publishProgress(values);

                if (message == null ||
                        message.equals(TwoStepCallStatus.STATE_CALLING_A) ||
                        message.equals(TwoStepCallStatus.STATE_CALLING_B) ||
                        message.equals(TwoStepCallStatus.STATE_CONNECTED)) {
                    try {
                        Thread.sleep(1000);
                        TwoStepCallStatus status = mApi.twoStepCall(mCallId);
                        handleMessage(status.getStatus());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }
    }
}
