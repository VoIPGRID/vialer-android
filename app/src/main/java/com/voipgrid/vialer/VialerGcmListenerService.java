package com.voipgrid.vialer;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.google.android.gms.gcm.GcmListenerService;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.MiddlewareHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Listen to messages from GCM. The backend server sends us GCM notifications when we have
 * incoming calls.
 */
public class VialerGcmListenerService extends GcmListenerService implements MiddlewareHelper.Constants {
    // Message format constants.
    private final static String MESSAGE_TYPE = "type";

    private final static String CALL_REQUEST_TYPE = "call";
    private final static String MESSAGE_REQUEST_TYPE = "message";

    private final static String RESPONSE_URL = "response_api";
    private final static String REQUEST_TOKEN = "unique_key";
    private final static String PHONE_NUMBER = "phonenumber";
    private final static String CALLER_ID = "caller_id";
    private static final String SUPPRESSED = "supressed";

    // Extra field for notification throughput logging.
    public static final String MESSAGE_START_TIME = "message_start_time";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String request = data.getString(MESSAGE_TYPE, "");
        if (request.equals(CALL_REQUEST_TYPE)) {

            AnalyticsHelper analyticsHelper = new AnalyticsHelper(
                    ((AnalyticsApplication) getApplication()).getDefaultTracker());

            ConnectivityHelper connectivityHelper = new ConnectivityHelper(
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                    (TelephonyManager) getSystemService(TELEPHONY_SERVICE));
            if(connectivityHelper.hasNetworkConnection() && connectivityHelper.hasFastData()) {

                String number = data.getString(PHONE_NUMBER);
                if(number != null && number.equalsIgnoreCase(SUPPRESSED)) {
                    number = getString(R.string.supressed_number);
                }

                // First start the SIP service with an incoming call.
                startSipService(
                        number,
                        data.getString(CALLER_ID),
                        data.getString(RESPONSE_URL),
                        data.getString(REQUEST_TOKEN),
                        data.getString(MESSAGE_START_TIME)
                );
            } else {
                // Inform the middleware the incoming call is received but the app can not handle
                // the sip call because there is no LTE or Wifi connection available at this
                // point.
                analyticsHelper.sendEvent(
                        getString(R.string.analytics_event_category_middleware),
                        getString(R.string.analytics_event_action_acceptance),
                        getString(R.string.analytics_event_label_middleware_rejected),
                        connectivityHelper.getConnectionType()
                );
                replyServer(
                        data.getString(RESPONSE_URL),
                        data.getString(REQUEST_TOKEN),
                        data.getString(MESSAGE_START_TIME),
                        false
                );
            }

        } else if (request.equals(MESSAGE_REQUEST_TYPE)) {
            // TODO: notify a user of message in payload.
        }
    }

    /**
     * Notify the middleware server that we are, in fact, alive.
     * @param responseUrl the URL of the server
     * @param requestToken unique_key for middleware for recognising SIP connection status updates.
     * @param messageStartTime
     */
    private void replyServer(String responseUrl, String requestToken, String messageStartTime,
                             boolean isAvailable) {
        Registration registrationApi = ServiceGenerator.createService(
                this,
                Registration.class,
                responseUrl
        );

        Call<ResponseBody> call = registrationApi.reply(requestToken, isAvailable, messageStartTime);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    /**
     * @param phoneNumber the number that tried call in.
     * @param callerId pretty name of the phonenumber that tied to call in.
     * @param messageStartTime message roundtrip throughput timestamp handled as String for logging
     *                         purposes.
     */
    private void startSipService(String phoneNumber, String callerId, String url, String token,
                                 String messageStartTime) {
        Intent intent = new Intent(this, SipService.class);
        intent.setAction(SipConstants.ACTION_VIALER_INCOMING);

        // Set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                this,
                PhoneNumberUtils.format(phoneNumber)
        );
        intent.setData(sipAddressUri);

        intent.putExtra(SipConstants.EXTRA_RESPONSE_URL, url);
        intent.putExtra(SipConstants.EXTRA_REQUEST_TOKEN, token);
        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, phoneNumber);
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, callerId);
        intent.putExtra(VialerGcmListenerService.MESSAGE_START_TIME, messageStartTime);

        startService(intent);
    }
}
