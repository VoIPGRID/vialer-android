package com.voipgrid.vialer.fcm;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Listen to messages from FCM. The backend server sends us FCM notifications when we have
 * incoming calls.
 */
public class FcmMessagingService extends FirebaseMessagingService {
    // Message format constants.
    private final static String MESSAGE_TYPE = "type";

    public final static String CALL_REQUEST_TYPE = "call";
    public final static String MESSAGE_REQUEST_TYPE = "message";

    public final static String RESPONSE_URL = "response_api";
    public final static String REQUEST_TOKEN = "unique_key";
    public final static String PHONE_NUMBER = "phonenumber";
    public final static String CALLER_ID = "caller_id";
    public static final String SUPPRESSED = "supressed";
    public static final String ATTEMPT = "attempt";

    // Extra field for notification throughput logging.
    public static final String MESSAGE_START_TIME = "message_start_time";

    private RemoteLogger mRemoteLogger;

    @Override
    public void onCreate() {
        super.onCreate();
        mRemoteLogger = new RemoteLogger(FcmMessagingService.class).enableConsoleLogging();
        mRemoteLogger.d("onCreate");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        mRemoteLogger.d("onMessageReceived");
        Map<String, String> data = remoteMessage.getData();
        String requestType = data.get(MESSAGE_TYPE);

        LogHelper.using(mRemoteLogger).logMiddlewareMessageReceived(remoteMessage, requestType);

        if (requestType == null) {
            mRemoteLogger.e("No requestType");
            return;
        }

        if (requestType.equals(CALL_REQUEST_TYPE)) {
            AnalyticsHelper analyticsHelper = new AnalyticsHelper(
                    ((AnalyticsApplication) getApplication()).getDefaultTracker()
            );
            ConnectivityHelper connectivityHelper = ConnectivityHelper.get(this);

            mRemoteLogger.d("SipService Active: " + SipService.sipServiceActive);
            mRemoteLogger.d("CurrentConnection: " + connectivityHelper.getConnectionTypeString());
            mRemoteLogger.d("Payload: " + data.toString());

            boolean connectionSufficient = connectivityHelper.hasNetworkConnection() && connectivityHelper.hasFastData();
            // Device can ben in Idle mode when it's been idling to long. This means that network connectivity
            // is reduced. So we check if we are in that mode and the connection is insufficient.
            // just return and don't reply to the middleware for now.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                boolean isDeviceIdleMode = powerManager.isDeviceIdleMode();
                mRemoteLogger.d("is device in idle mode: " + isDeviceIdleMode);
                if (isDeviceIdleMode && !connectionSufficient) {
                    mRemoteLogger.e("Device in idle mode and connection insufficient. For now do nothing wait for next middleware push.");
                    return;
                }
            }

            if (!connectionSufficient) {
                mRemoteLogger.e("Connection is insufficient. For now do nothing and wait for next middleware push");
                return;
            }

            // Check to see if there is not already a sipServiceActive and the current connection is
            // fast enough to support VoIP call.
            if (!SipService.sipServiceActive) {
                String number = data.get(PHONE_NUMBER);

                // Is the current number suppressed.
                if (number != null && (number.equalsIgnoreCase(SUPPRESSED) || number.toLowerCase().contains("xxxx"))) {
                    number = getString(R.string.supressed_number);
                }

                String callerId = data.get(CALLER_ID) != null ? data.get(CALLER_ID) : "";
                String responseUrl = data.get(RESPONSE_URL) != null ? data.get(RESPONSE_URL) : "";
                String requestToken = data.get(REQUEST_TOKEN) != null ? data.get(REQUEST_TOKEN) : "";
                String messageStartTime = data.get(MESSAGE_START_TIME) != null ? data.get(MESSAGE_START_TIME) : "";

                mRemoteLogger.d("Payload processed, calling startService method");

                // First start the SIP service with an incoming call.
                startSipService(
                        number,
                        callerId,
                        responseUrl,
                        requestToken,
                        messageStartTime
                );
            } else {
                mRemoteLogger.d("Reject due to lack of connection");
                // Inform the middleware the incoming call is received but the app can not handle
                // the sip call because there is no LTE or Wifi connection available at this
                // point.
                String analyticsLabel = connectivityHelper.getAnalyticsLabel();

                analyticsHelper.sendEvent(
                        getString(R.string.analytics_event_category_middleware),
                        getString(R.string.analytics_event_action_middleware_rejected),
                        analyticsLabel
                );
                replyServer(
                        data.get(RESPONSE_URL) != null ? data.get(RESPONSE_URL) : "",
                        data.get(REQUEST_TOKEN) != null ? data.get(REQUEST_TOKEN) : "",
                        data.get(MESSAGE_START_TIME) != null ? data.get(MESSAGE_START_TIME) : "",
                        false
                );
            }
        } else if (requestType.equals(MESSAGE_REQUEST_TYPE)){
            mRemoteLogger.d("Code not implemented");
            // TODO implement this message.
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
        mRemoteLogger.d("replyServer");
        Registration registrationApi = ServiceGenerator.createRegistrationService(this);

        Call<ResponseBody> call = registrationApi.reply(requestToken, isAvailable, messageStartTime);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

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
        mRemoteLogger.d("startSipService");
        Intent intent = new Intent(this, SipService.class);
        intent.setAction(SipConstants.ACTION_CALL_INCOMING);

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
        intent.putExtra(FcmMessagingService.MESSAGE_START_TIME, messageStartTime);

        startService(intent);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        mRemoteLogger.d("Message deleted on the FCM server.");
    }
}
