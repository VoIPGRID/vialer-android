package com.voipgrid.vialer;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;

import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.TwoStepCallStatus;
import com.voipgrid.vialer.models.ClickToDialParams;
import com.voipgrid.vialer.twostepcall.widget.TwoStepCallView;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.Storage;

import retrofit.client.OkClient;

public class TwoStepCallActivity extends Activity {

    public static final String NUMBER_TO_CALL = "number-to-call";

    private TextView mDialerWarningTextView;
    private TextView mStatusTextView;

    private AnalyticsHelper mAnalyticsHelper;
    private ConnectivityHelper mConnectivityHelper;
    private Preferences mPreferences;
    private Storage mStorage;
    private TwoStepCallView mTwoStepCallView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_step_call);

        /* set the AnalyticsHelper */
        mAnalyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) getApplication()).getDefaultTracker()
        );

        mStorage = new Storage(this);

        mPreferences = new Preferences(this);

        mConnectivityHelper = new ConnectivityHelper(
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE)
        );

        mDialerWarningTextView = (TextView) findViewById(R.id.dialer_warning);

        SystemUser systemUser = (SystemUser) new Storage(this).get(SystemUser.class);

        Api api = ServiceGenerator.createService(
                mConnectivityHelper,
                Api.class,
                getString(R.string.api_url),
                new OkClient(ServiceGenerator.getOkHttpClient(
                        this,
                        systemUser.getEmail(),
                        systemUser.getPassword())
                )
        );

        String numberToCall = getIntent().getStringExtra(NUMBER_TO_CALL);

        new TwoStepCallTask(api, systemUser.getMobileNumber(), numberToCall).execute();

        mTwoStepCallView = (TwoStepCallView) findViewById(R.id.two_step_call_view);
        mTwoStepCallView.setOutgoingNumber(systemUser.getOutgoingCli());

        mAnalyticsHelper.send(
                getString(R.string.analytics_dimension),
                getString(R.string.analytics_event_category_call),
                getString(R.string.analytics_event_action_outbound),
                getString(R.string.analytics_event_label_connect_a_b)
        );

        ((TextView) findViewById(R.id.name_text_view)).setText(numberToCall);
        mStatusTextView = ((TextView) findViewById(R.id.status_text_view));
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
        } else if(!mStorage.has(PhoneAccount.class) && mPreferences.canUseSip()) {
            mDialerWarningTextView.setText(R.string.dialer_warning_a_b_connect);
            mDialerWarningTextView
                    .setTag(getString(R.string.dialer_warning_a_b_connect_account_message));
        } else {
            mDialerWarningTextView.setVisibility(View.GONE);
        }
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

        @Override
        protected Boolean doInBackground(Void... params) {
            TwoStepCallStatus status = mApi.twoStepCall(new ClickToDialParams(mNumberA, mNumberB));
            mCallId = status.getCallId();
            mNumberA = status.getaNumber();
            mNumberB = status.getbNumber();
            String message = status.getStatus();
            return handleMessage(message, mNumberA, mNumberB);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if(values.length > 0) {
                mTwoStepCallView.setState(values[0]);
                mTwoStepCallView.setNumberA(mNumberA);
                mTwoStepCallView.setNumberB(mNumberB);
                mStatusTextView.setText(getStateText(values[0]));
            }
        }

        private boolean handleMessage(String... values) {
            if(values.length > 0) {
                String message = values[0];
                if (message != null) {
                    values[0] = getViewState(message);
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

        private String getViewState(String message) {
            if(message != null) {
                switch (message) {
                    case TwoStepCallStatus.STATE_CALLING_A:
                        return TwoStepCallView.STATE_CALLING_A;
                    case TwoStepCallStatus.STATE_CALLING_B:
                        return TwoStepCallView.STATE_CALLING_B;
                    case TwoStepCallStatus.STATE_FAILED_A:
                        return TwoStepCallView.STATE_FAILED_A;
                    case TwoStepCallStatus.STATE_FAILED_B:
                        return TwoStepCallView.STATE_FAILED_B;
                    case TwoStepCallStatus.STATE_CONNECTED:
                        return TwoStepCallView.STATE_CONNECTED;
                    case TwoStepCallStatus.STATE_DISCONNECTED:
                        return TwoStepCallView.STATE_DISCONNECTED;
                }
            }
            return TwoStepCallView.STATE_INITIAL;
        }

        private String getStateText(String message) {
            if(message != null) {
                int resource = 0;
                switch (message) {
                    case TwoStepCallView.STATE_CALLING_A:
                        resource = R.string.two_step_call_state_dialing_a; break;
                    case TwoStepCallView.STATE_CALLING_B:
                        resource =  R.string.two_step_call_state_dialing_b; break;
                    case TwoStepCallView.STATE_FAILED_A:
                        resource =  R.string.two_step_call_state_failed_a; break;
                    case TwoStepCallView.STATE_FAILED_B:
                        resource =  R.string.two_step_call_state_failed_b; break;
                    case TwoStepCallView.STATE_CONNECTED:
                        resource =  R.string.two_step_call_state_connected; break;
                    case TwoStepCallView.STATE_DISCONNECTED:
                        resource =  R.string.two_step_call_state_disconnected; break;
                }

                if(resource > 0) {
                    return getResources().getString(resource);
                }
            }
            return null;
        }
    }
}
