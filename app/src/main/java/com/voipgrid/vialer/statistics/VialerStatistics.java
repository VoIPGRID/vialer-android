package com.voipgrid.vialer.statistics;


import static com.voipgrid.vialer.fcm.RemoteMessageData.ATTEMPT;
import static com.voipgrid.vialer.fcm.RemoteMessageData.MESSAGE_START_TIME;
import static com.voipgrid.vialer.fcm.RemoteMessageData.REQUEST_TOKEN;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_ACCOUNT_CONNECTION_TYPE;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_APP_STATUS;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_APP_VERSION;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_BLUETOOTH_AUDIO_ENABLED;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_BLUETOOTH_DEVICE_NAME;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CALL_DIRECTION;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CALL_DURATION;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CALL_ID;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CALL_SETUP_SUCCESSFUL;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CLIENT_COUNTRY;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CODEC;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_CONNECTION_TYPE;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_DEVICE_MANUFACTURER;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_DEVICE_MODEL;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_FAILED_REASON;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_HANGUP_REASON;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_LOG_ID;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_MIDDLEWARE_ATTEMPTS;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_MIDDLEWARE_KEY;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_MOS;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_NETWORK;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_NETWORK_OPERATOR;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_OS;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_OS_VERSION;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_RX_PACKETS;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_SIP_USER_ID;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_TIME_TO_INITIAL_RESPONSE;
import static com.voipgrid.vialer.statistics.StatsConstants.KEY_TX_PACKETS;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_ACCOUNT_CONNECTION_TYPE_TCP;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_ACCOUNT_CONNECTION_TYPE_TLS;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_BLUETOOTH_AUDIO_ENABLED_TRUE;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_CALL_DIRECTION_INCOMING;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_CALL_SETUP_FAILED;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_CALL_SETUP_SUCCESSFUL;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_GSM_CALL_IN_PROGRESS;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_INSUFFICIENT_NETWORK;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_NO_CALL_RECEIVED_FROM_ASTERISK;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_REASON_COMPLETED_ELSEWHERE;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_REASON_DECLINED;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_REASON_NO_AUDIO;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_REASON_NO_AUDIO_RECEIVED;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_REASON_NO_AUDIO_SENT;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_REASON_ORIGINATOR_CANCELLED;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_FAILED_VIALER_CALL_IN_PROGRESS;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_HANGUP_REASON_REMOTE;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_HANGUP_REASON_USER;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_NETWORK_WIFI;
import static com.voipgrid.vialer.statistics.StatsConstants.VALUE_OS;

import android.content.Context;

import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.GsonBuilder;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.Middleware;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.media.monitoring.PacketStats;
import com.voipgrid.vialer.sip.SipCall;
import com.voipgrid.vialer.statistics.providers.BluetoothDataProvider;
import com.voipgrid.vialer.statistics.providers.DefaultDataProvider;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VialerStatistics {

    private final DefaultDataProvider mDefaultDataProvider;
    private final BluetoothDataProvider mBluetoothDataProvider;
    private final Logger mLogger;
    private final Middleware mMiddleware;

    private Map<String, String> payload;

    private static VialerStatistics get() {
        Context context = VialerApplication.get();

        return new VialerStatistics(ServiceGenerator.createRegistrationService(context));
    }

    private VialerStatistics(Middleware middleware) {
        mMiddleware = middleware;
        mLogger = new Logger(this.getClass());
        mDefaultDataProvider = new DefaultDataProvider();
        mBluetoothDataProvider = new BluetoothDataProvider();
        resetPayload();
    }

    public static void pushNotificationWasReceived(RemoteMessage middlewarePayload) {
        VialerStatistics
                .get()
                .withDefaults()
                .withMiddlewareInformation(middlewarePayload)
                .send();
    }

    public static void callWasSuccessfullySetup(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_SUCCESSFUL)
                .send();
    }

    public static void incomingCallFailedDueToSipError(String requestToken, String messageStartTime, String attempt, int sipErrorCode) {
        VialerStatistics
                .get()
                .withDefaults()
                .withMiddlewareInformation(requestToken, messageStartTime, attempt)
                .withBluetoothInformation()
                .addValue(KEY_FAILED_REASON, String.valueOf(sipErrorCode))
                .send();
    }

    public static void callFailedDueToNoAudio(SipCall sipCall, boolean hasReceivedAudio, boolean hasSentAudio) {
        String failedReason;

        if (!hasReceivedAudio && !hasSentAudio) {
            failedReason = VALUE_FAILED_REASON_NO_AUDIO;
        }
        else if (!hasReceivedAudio) {
            failedReason = VALUE_FAILED_REASON_NO_AUDIO_RECEIVED;
        }
        else {
            failedReason = VALUE_FAILED_REASON_NO_AUDIO_SENT;
        }

        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_FAILED_REASON, failedReason)
                .send();
    }

    public static void noCallReceivedFromAsteriskAfterOkToMiddleware(String requestToken, String messageStartTime, String attempt) {
        VialerStatistics
                .get()
                .withDefaults()
                .withMiddlewareInformation(requestToken, messageStartTime, attempt)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_NO_CALL_RECEIVED_FROM_ASTERISK)
                .send();
    }

    public static void incomingCallFailedDueToInsufficientNetwork(RemoteMessage middlewarePayload) {
        VialerStatistics
                .get()
                .withDefaults()
                .withMiddlewareInformation(middlewarePayload)
                .withBluetoothInformation()
                .addValue(KEY_CALL_DIRECTION, VALUE_CALL_DIRECTION_INCOMING)
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_INSUFFICIENT_NETWORK)
                .send();
    }

    public static void incomingCallFailedDueToOngoingGsmCall(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_GSM_CALL_IN_PROGRESS)
                .send();
    }

    public static void incomingCallFailedDueToOngoingGsmCall(RemoteMessage middlewarePayload) {
        VialerStatistics
                .get()
                .withDefaults()
                .withMiddlewareInformation(middlewarePayload)
                .withBluetoothInformation()
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_GSM_CALL_IN_PROGRESS)
                .send();
    }

    public static void incomingCallFailedDueToOngoingVialerCall(RemoteMessage middlewarePayload) {
        VialerStatistics
                .get()
                .withDefaults()
                .withMiddlewareInformation(middlewarePayload)
                .addValue(KEY_CALL_DIRECTION, VALUE_CALL_DIRECTION_INCOMING)
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_VIALER_CALL_IN_PROGRESS)
                .send();
    }

    public static void incomingCallFailedDueToOngoingVialerCall(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_VIALER_CALL_IN_PROGRESS)
                .send();
    }

    public static void userDeclinedIncomingCall(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_CALL_DIRECTION, VALUE_CALL_DIRECTION_INCOMING)
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_REASON_DECLINED)
                .send();
    }

    public static void incomingCallWasCompletedElsewhere(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .addValue(KEY_CALL_DIRECTION, VALUE_CALL_DIRECTION_INCOMING)
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_REASON_COMPLETED_ELSEWHERE)
                .send();
    }

    public static void incomingCallWasCancelledByOriginator(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .addValue(KEY_CALL_DIRECTION, VALUE_CALL_DIRECTION_INCOMING)
                .addValue(KEY_CALL_SETUP_SUCCESSFUL, VALUE_CALL_SETUP_FAILED)
                .addValue(KEY_FAILED_REASON, VALUE_FAILED_REASON_ORIGINATOR_CANCELLED)
                .send();
    }

    public static void userDidHangUpCall(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_HANGUP_REASON, VALUE_HANGUP_REASON_USER)
                .addValue(KEY_CALL_DURATION, String.valueOf(sipCall.getCallDurationInMilliseconds()))
                .send();
    }

    public static void remoteDidHangUpCall(SipCall sipCall) {
        VialerStatistics
                .get()
                .withDefaults()
                .withCallInformation(sipCall)
                .withBluetoothInformation()
                .addValue(KEY_HANGUP_REASON, VALUE_HANGUP_REASON_REMOTE)
                .addValue(KEY_CALL_DURATION, String.valueOf(sipCall.getCallDurationInMilliseconds()))
                .send();
    }

    private VialerStatistics withMiddlewareInformation(RemoteMessage middlewarePayload) {
        Map<String, String> data = middlewarePayload.getData();
        withMiddlewareInformation(data.get(REQUEST_TOKEN), data.get(MESSAGE_START_TIME), data.get(ATTEMPT));

        return this;
    }

    private VialerStatistics withMiddlewareInformation(String requestToken, String messageStartTime, String attempt) {
        addValue(KEY_MIDDLEWARE_KEY, requestToken);
        addValue(KEY_TIME_TO_INITIAL_RESPONSE, String.valueOf(calculateTimeToInitialResponse(messageStartTime)));
        addValue(KEY_MIDDLEWARE_ATTEMPTS,attempt);

        return this;
    }

    private VialerStatistics withDefaults() {
        addValue(KEY_OS, VALUE_OS);
        addValue(KEY_OS_VERSION, mDefaultDataProvider.getOSVersion());
        addValue(KEY_APP_VERSION, mDefaultDataProvider.getAppVersion());
        addValue(KEY_APP_STATUS, mDefaultDataProvider.getAppStatus());
        addValue(KEY_NETWORK, mDefaultDataProvider.getNetwork());
        if (!mDefaultDataProvider.getNetwork().equals(VALUE_NETWORK_WIFI)) {
            addValue(KEY_NETWORK_OPERATOR, mDefaultDataProvider.getNetworkOperator());
        }
        addValue(KEY_DEVICE_MANUFACTURER, mDefaultDataProvider.getDeviceManufacturer().toLowerCase());
        addValue(KEY_DEVICE_MODEL, mDefaultDataProvider.getDeviceModel().toLowerCase());
        addValue(KEY_CLIENT_COUNTRY, mDefaultDataProvider.getClientCountry());
        addValue(KEY_SIP_USER_ID, mDefaultDataProvider.getSipUserId());

        if (mDefaultDataProvider.getLogId() != null) {
            addValue(KEY_LOG_ID, mDefaultDataProvider.getLogId());
        }

        return this;
    }

    private VialerStatistics withCallInformation(SipCall call) {
        if (call.getMiddlewareKey() != null && !call.getMiddlewareKey().isEmpty()) {
            addValue(KEY_MIDDLEWARE_KEY, call.getMiddlewareKey());
        }

        addValue(KEY_CALL_ID, call.getAsteriskCallId());
        addValue(KEY_CALL_DIRECTION, call.getCallDirection());
        addValue(KEY_CONNECTION_TYPE, call.getTransport() != null ? call.getTransport().toUpperCase() : "");
        addValue(KEY_ACCOUNT_CONNECTION_TYPE, SecureCalling.fromContext(VialerApplication.get()).isEnabled() ? VALUE_ACCOUNT_CONNECTION_TYPE_TLS : VALUE_ACCOUNT_CONNECTION_TYPE_TCP);
        addValue(KEY_CODEC, call.getCodec());

        if (call.getMessageStartTime() != null) {
            addValue(KEY_TIME_TO_INITIAL_RESPONSE, String.valueOf(calculateTimeToInitialResponse(call.getMessageStartTime())));
        }

        PacketStats packetStats = call.getLastMediaPacketStats();

        if (packetStats != null) {
            addValue(KEY_RX_PACKETS, String.valueOf(packetStats.getReceived()));
            addValue(KEY_TX_PACKETS, String.valueOf(packetStats.getSent()));
        }

        if (call.hasCalculatedMos()) {
            addValue(KEY_MOS, String.valueOf(call.getMos()));
        }

        return this;
    }

    /**
     * Convert the initial response time from the middleware (scientific notation, microseconds) to a standard long
     * in milliseconds and then find the delta between that and the current time.
     *
     * @param startTime
     * @return
     */
    private long calculateTimeToInitialResponse(String startTime) {
        if (startTime == null) {
            mLogger.i("Message start time is null");
            return 0;
        }

        long startTimeInMilliseconds = Long.parseLong(new BigDecimal(startTime).toPlainString().replace(".", "")) / 10000;

        return System.currentTimeMillis() - startTimeInMilliseconds;
    }

    private VialerStatistics withBluetoothInformation() {
        String usingBluetooth = mBluetoothDataProvider.getBluetoothAudioEnabled();
        if (usingBluetooth.equals(VALUE_BLUETOOTH_AUDIO_ENABLED_TRUE)) {
            addValue(KEY_BLUETOOTH_DEVICE_NAME, mBluetoothDataProvider.getBluetoothDeviceName());
        }
        addValue(KEY_BLUETOOTH_AUDIO_ENABLED, usingBluetooth);

        return this;
    }

    private VialerStatistics addValue(String key, String value) {
        payload.put(key, value);

        return this;
    }

    private void send() {
        mMiddleware.metrics(payload).enqueue(new VialerStatisticsRequestCallback(mLogger));
        log();
        resetPayload();
    }

    private void log() {
        String[] fieldsToAnonymize = {KEY_SIP_USER_ID, KEY_CALL_ID};

        Map<String, String> payload = new HashMap<>(this.payload);

        for (String field : fieldsToAnonymize) {
            if (!payload.containsKey(field)) {
                continue;
            }

            payload.put(field, "<ANONYMIZED>");
        }

        mLogger.i(
                new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(payload)
        );
    }

    private void resetPayload() {
        payload = new LinkedHashMap<>();
    }

    public Map<String, String> build() {
        return payload;
    }

    private static class VialerStatisticsRequestCallback implements Callback<Void> {

        private final Logger mLogger;

        private VialerStatisticsRequestCallback(Logger logger) {
            mLogger = logger;
        }

        @Override
        public void onResponse(Call<Void> call, Response<Void> response) {
            if (!response.isSuccessful()) {
                mLogger.e("Failed to upload vialer statistics, with status code: " + response.code());
            }
        }

        @Override
        public void onFailure(Call<Void> call, Throwable t) {
            mLogger.e("Failed to upload vialer statistics with exception: " + t.getMessage());
        }
    }
}
