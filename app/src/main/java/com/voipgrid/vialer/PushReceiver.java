package com.voipgrid.vialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.fcm.RemoteMessageData;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipInvite;
import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.NotificationHelper;
import com.voipgrid.vialer.util.PhoneNumberUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PushReceiver extends BroadcastReceiver {

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


    private Logger mRemoteLogger;
    private AnalyticsHelper mAnalyticsHelper;
    private ConnectivityHelper mConnectivityHelper;
    private PowerManager mPowerManager;

    public PushReceiver() {
        super();
        mRemoteLogger = new Logger(FcmMessagingService.class);
        mAnalyticsHelper = new AnalyticsHelper(VialerApplication.get().getDefaultTracker());
        mConnectivityHelper = ConnectivityHelper.get(VialerApplication.get());
        mPowerManager = (PowerManager) VialerApplication.get().getSystemService(Context.POWER_SERVICE);
        mRemoteLogger.d("onCreate");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Map<String, String> data = new HashMap<>();
        data.put("message_start_time", String.valueOf(intent.getDoubleExtra("message_start_time", 0)));
        data.put("caller_id", intent.getStringExtra("caller_id"));
        data.put("attempt", intent.getStringExtra("attempt"));
        data.put("phonenumber", intent.getStringExtra("phonenumber"));
        data.put("type", intent.getStringExtra("type"));
        data.put("unique_key", intent.getStringExtra("unique_key"));
        data.put("response_api", intent.getStringExtra("response_api"));

        RemoteMessageData remoteMessageData = new RemoteMessageData(data);
Log.e("TEST123", "Got pushy message:" + remoteMessageData.getCallerId() + remoteMessageData.getPhoneNumber() + remoteMessageData.getRequestToken());

        if (remoteMessageData.isCallRequest()) {
            handleCall(remoteMessageData);
            return;
        }
    }

    /**
     * Handle a push message with a call request type.
     *
     * @param remoteMessageData
     */
    private void handleCall(RemoteMessageData remoteMessageData) {
        logCurrentState(remoteMessageData);

        if (!isConnectionSufficient()) {
            return;
        }

        if (isAVialerCallAlreadyInProgress()) {
            rejectDueToVialerCallAlreadyInProgress(remoteMessageData);
            return;
        }


        sLastHandledCall = remoteMessageData.getRequestToken();

        mRemoteLogger.d("Payload processed, calling startService method");

        startSipService(remoteMessageData);
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
     * @param remoteMessageData The remote message data that we are handling.
     */
    private void rejectDueToVialerCallAlreadyInProgress(RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("Reject due to call already in progress");

        replyServer(remoteMessageData, false);
    }


    /**
     * Notify the middleware server that we are, in fact, alive.
     *
     * @param remoteMessageData The remote message data from the remote message that we are handling.
     * @param isAvailable TRUE if the phone is ready to accept the incoming call, FALSE if it is not available.
     */
    private void replyServer(RemoteMessageData remoteMessageData, boolean isAvailable) {
        mRemoteLogger.d("replyServer");
        Registration registrationApi = ServiceGenerator.createRegistrationService(VialerApplication.get());

        Call<ResponseBody> call = registrationApi.reply(remoteMessageData.getRequestToken(), isAvailable, remoteMessageData.getMessageStartTime());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.e("TEST123", "Middleware OK responded with: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
    Log.e("TEST123", "e", t);
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
        mRemoteLogger.d("startSipService");
        Intent intent = new Intent(VialerApplication.get(), SipService.class);
        intent.setAction(SipConstants.ACTION_CALL_INCOMING);

        // Set a phoneNumberUri as DATA for the intent to SipServiceOld.
        Uri sipAddressUri = SipUri.sipAddressUri(
                VialerApplication.get(),
                PhoneNumberUtils.format(remoteMessageData.getPhoneNumber())
        );

        intent.setData(sipAddressUri);

        intent.putExtra(SipConstants.EXTRA_RESPONSE_URL, remoteMessageData.getResponseUrl());
        intent.putExtra(SipConstants.EXTRA_REQUEST_TOKEN, remoteMessageData.getRequestToken());
        intent.putExtra(SipConstants.EXTRA_PHONE_NUMBER, remoteMessageData.getPhoneNumber());
        intent.putExtra(SipConstants.EXTRA_CONTACT_NAME, remoteMessageData.getCallerId());
        intent.putExtra(RemoteMessageData.MESSAGE_START_TIME, remoteMessageData.getMessageStartTime());

        VialerApplication.get().startService(intent);
    }

    /**
     * Device can ben in Idle mode when it's been idling to long. This means that network connectivity
     * is reduced. So we check if we are in that mode and the connection is insufficient.
     * just return and don't reply to the middleware for now.
     *
     * @return
     */
    private boolean isDeviceInIdleMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mPowerManager.isDeviceIdleMode();
    }

    /**
     * Log some information about our current state to help determine what state the phone is in when
     * a push notification is incoming.
     *
     * @param remoteMessageData
     */
    private void logCurrentState(RemoteMessageData remoteMessageData) {
        mRemoteLogger.d("SipService Active: " + SipService.sipServiceActive);
        mRemoteLogger.d("CurrentConnection: " + mConnectivityHelper.getConnectionTypeString());
        mRemoteLogger.d("Payload: " + remoteMessageData.getRawData().toString());
    }
}

