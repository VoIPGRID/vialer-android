package com.voipgrid.vialer.fcm;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Listen to messages from FCM. The backend server sends us FCM notifications when we have
 * incoming calls.
 */
public class FcmMessagingService extends FirebaseMessagingService {

    /**
     * The number of times the middleware will attempt to send a push notification
     * before it gives up.
     *
     */
    private static final int MAX_MIDDLEWARE_PUSH_ATTEMPTS = 8;

    /**
     * Stores the last call we have SUCCESSFULLY handled and started the SipService
     * for.
     */
    private static String sLastHandledCall;

    public static final String VOIP_HAS_BEEN_DISABLED = "com.voipgrid.vialer.voip_disabled";

    private Logger mLogger;
    private AnalyticsHelper mAnalyticsHelper;
    private ConnectivityHelper mConnectivityHelper;
    private PowerManager mPowerManager;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        VialerStatistics.pushNotificationWasReceived(remoteMessage);

        if (!remoteMessageData.hasRequestType()) {
            mLogger.e("No requestType");
            return;
        }

        if (remoteMessageData.isCallRequest()) {
            handleCall(remoteMessage, remoteMessageData);
            return;
        }

        if (remoteMessageData.isMessageRequest()) {
            handleMessage(remoteMessage, remoteMessageData);
            return;
        }
    }




        if (isAVialerCallAlreadyInProgress()) {
            rejectDueToVialerCallAlreadyInProgress(remoteMessage, remoteMessageData);
            return;
        }

        sLastHandledCall = remoteMessageData.getRequestToken();

        mRemoteLogger.d("Payload processed, calling startService method");



        Preferences preferences = new Preferences(this);

        }

        new Preferences(this).setSipEnabled(false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(VOIP_HAS_BEEN_DISABLED));
    }

    /**
     * Performs various tasks that are required when we are rejecting a call due to an insufficient
     * network connection.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private void handleInsufficientConnection(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        if (hasExceededMaximumAttempts(remoteMessageData)) {
            VialerStatistics.incomingCallFailedDueToInsufficientNetwork(remoteMessage);

            String analyticsLabel = mConnectivityHelper.getAnalyticsLabel();

            mAnalyticsHelper.sendEvent(
                    getString(R.string.analytics_event_category_middleware),
                    getString(R.string.analytics_event_action_middleware_rejected),
                    analyticsLabel
            );
        }

        if (isDeviceInIdleMode()) {
            mRemoteLogger.e("Device in idle mode and connection insufficient. For now do nothing wait for next middleware push.");
        }
        else {
            mRemoteLogger.e("Connection is insufficient. For now do nothing and wait for next middleware push");
        }
    }

    /**
     * Check if we have a good enough connection to accept an incoming call.
     *
     * @return TRUE if we have a good enough connection, otherwise FALSE.
     */
    private boolean isConnectionSufficient() {
        return mConnectivityHelper.hasNetworkConnection() && mConnectivityHelper.hasFastData();
    }

    /**
     * Check to see if the SIP service is currently running, this means that there is already a call
     * in progress and we can not accept further calls.
     *
     * @return TRUE if there is an active call, otherwise FALSE
     */
    private boolean isAVialerCallAlreadyInProgress() {
        return SipService.sipServiceActive;
    }

    /**
     * Check if we have reached or exceeded the maximum number of attempts that we
     * accept from the middleware.
     *
     * @param remoteMessageData The remote message data that we are handling.
     * @return TRUE if we have reached or exceeded maximum attempts, otherwise FALSE.
     */
    private boolean hasExceededMaximumAttempts(RemoteMessageData remoteMessageData) {
        return remoteMessageData.getAttemptNumber() >= MAX_MIDDLEWARE_PUSH_ATTEMPTS;
    }

    /**
     * Performs various tasks that are necessary when rejecting a call based on the fact that there is
     * already a Vialer call in progress.
     *
     * @param remoteMessage The remote message that we are handling.
     * @param remoteMessageData The remote message data that we are handling.
     */
    private void rejectDueToVialerCallAlreadyInProgress(RemoteMessage remoteMessage, RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("Reject due to lack of connection");

        replyServer(remoteMessageData, false);

        sendCallFailedDueToOngoingVialerCallMetric(remoteMessage, remoteMessageData.getRequestToken());
    }

    /**
     * Send the vialer metric for ongoing call if appropriate.
     *
     * @param remoteMessage
     * @param requestToken
     */
    private void sendCallFailedDueToOngoingVialerCallMetric(RemoteMessage remoteMessage, String requestToken) {
        if (sLastHandledCall != null && sLastHandledCall.equals(requestToken)) {
            mLogger.i("Push notification (" + sLastHandledCall + ") is being rejected because there is a Vialer call already in progress but not sending metric because it was already handled successfully");
            return;
        }

        VialerStatistics.incomingCallFailedDueToOngoingVialerCall(remoteMessage);
    }

    /**
     * Notify the middleware server that we are, in fact, alive.
     *
     * @param remoteMessageData The remote message data from the remote message that we are handling.
     * @param isAvailable TRUE if the phone is ready to accept the incoming call, FALSE if it is not available.
     */
    private void replyServer(RemoteMessageData remoteMessageData, boolean isAvailable) {
        mLogger.d("replyServer");
        Registration registrationApi = ServiceGenerator.createRegistrationService(this);

        Call<ResponseBody> call = registrationApi.reply(remoteMessageData.getRequestToken(), isAvailable, remoteMessageData.getMessageStartTime());
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
     * Start the SIP service with the relevant data from the push message in the
     * intent.
     *
     * @param remoteMessageData
     */
    private void startSipService(RemoteMessageData remoteMessageData) {
        mLogger.d("startSipService");
        Intent intent = new Intent(this, SipService.class);
        intent.setAction(SipConstants.ACTION_CALL_INCOMING);

        // Set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                this,
                PhoneNumberUtils.format(remoteMessageData.getPhoneNumber())
        );

        intent.setData(sipAddressUri);

        intent.putExtra(SipConstants.EXTRA_RESPONSE_URL, remoteMessageData.getResponseUrl());
        intent.putExtra(SipConstants.EXTRA_REQUEST_TOKEN, remoteMessageData.getRequestToken());
        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, remoteMessageData.getPhoneNumber());
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, remoteMessageData.getCallerId());
        intent.putExtra(RemoteMessageData.MESSAGE_START_TIME, remoteMessageData.getMessageStartTime());

        startService(intent);
    }

    }
}
