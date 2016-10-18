package com.voipgrid.vialer.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.fcm.FcmListenerService;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.ConnectivityHelper;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.MediaConfig;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjsip_transport_type_e;
import org.pjsip.pjsua2.pjsua_call_flag;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;

import static com.voipgrid.vialer.api.ServiceGenerator.getUserAgentHeader;

/**
 * Class that holds the sip backend (Endpoint + SipAccount).
 */
public class SipConfig implements AccountStatus {
    private static final String TAG = SipConfig.class.getSimpleName();

    private Endpoint mEndpoint;
    private PhoneAccount mPhoneAccount;
    private RemoteLogger mRemoteLogger;
    private SipAccount mSipAccount;
    private SipLogWriter mSipLogWriter;
    private SipService mSipService;

    private boolean mReRegisterAccount = false;
    private boolean mHasRespondedToMiddleware = false;
    private int mCurrentTransportId;
    private Long mLatestConnectionType;
    private static Map<String, Short> sCodecPrioMapping;

    private BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleNetworkStateChange(context);
        }
    };

    static {
        sCodecPrioMapping = new HashMap<>();
        sCodecPrioMapping.put("iLBC/8000/1", (short) 210);
        sCodecPrioMapping.put("PCMA/8000/1", (short) 209);
        sCodecPrioMapping.put("G722/16000/1", (short) 208);
        sCodecPrioMapping.put("PCMU/8000/1", (short) 0);
        sCodecPrioMapping.put("speex/8000/1", (short) 0);
        sCodecPrioMapping.put("speex/16000/1", (short) 0);
        sCodecPrioMapping.put("speex/32000/1", (short) 0);
        sCodecPrioMapping.put("GSM/8000/1", (short) 0);
    }

    public SipConfig(SipService sipService, PhoneAccount phoneAccount) {
        mSipService = sipService;
        mPhoneAccount = phoneAccount;
        mRemoteLogger = mSipService.getRemoteLogger();
    }

    public Endpoint getEndpoint() {
        return mEndpoint;
    }

    public SipAccount getSipAccount() {
        return mSipAccount;
    }

    /**
     * Function to init the PJSIP library and setup all credentials.
     * @throws LibraryInitFailedException
     */
    public void initLibrary() throws LibraryInitFailedException {
        loadPjsip();
        mEndpoint = createEndpoint();
        setCodecPrio();
        mSipAccount = createSipAccount();
        // Start listening for network changes after everything is setup.
        mLatestConnectionType = ConnectivityHelper.get(mSipService).getConnectionType();
        startNetworkingListener();
    }

    /**
     * Load the PJSIP library.
     * @throws LibraryInitFailedException
     */
    private void loadPjsip() throws LibraryInitFailedException {
        mRemoteLogger.d(TAG + " Loading PJSIP");
        try {
            System.loadLibrary("pjsua2");
        } catch (UnsatisfiedLinkError error) { /* Can not load PJSIP library */
            Log.e(TAG, error.getMessage());
            mRemoteLogger.e(TAG + " " + Log.getStackTraceString(error));
            throw new LibraryInitFailedException();
        }
    }

    /**
     * Function to create the transport config.
     * @return
     */
    private TransportConfig createTransportConfig() {
        TransportConfig config = new TransportConfig();
        return config;
    }

    /**
     * Function to generate a transport string based on a setting.
     * @return
     */
    private String getTransportString() {
        String sipTransport = mSipService.getString(R.string.sip_transport_type);
        String tcp = "";

        if (sipTransport.equals("tcp")) {
            tcp = ";transport=tcp";
        }
        return tcp;
    }

    /**
     * Function to get the transport type based on a setting.
     * @return
     */
    private pjsip_transport_type_e getTransportType() {
        String sipTransport = mSipService.getString(R.string.sip_transport_type);

        pjsip_transport_type_e transportType = pjsip_transport_type_e.PJSIP_TRANSPORT_UDP;
        if (sipTransport.equals("tcp")) {
            transportType = pjsip_transport_type_e.PJSIP_TRANSPORT_TCP;
        }
        return transportType;
    }

    /**
     * Create the MediaConfig with echo cancellation settings.
     * @param endpointConfig
     * @return
     */
    private MediaConfig createMediaConfig(EpConfig endpointConfig) {
        MediaConfig mediaConfig = endpointConfig.getMedConfig();
        mediaConfig.setEcOptions(SipConstants.WEBRTC_ECHO_CANCELLATION);
        mediaConfig.setEcTailLen(SipConstants.ECHO_CANCELLATION_TAIL_LENGTH);
        return mediaConfig;
    }

    /**
     * Configure the logger for the PJSIP library.
     * @param endpointConfig
     */
    private void setSipLogging(EpConfig endpointConfig) {
        endpointConfig.getLogConfig().setLevel(SipConstants.SIP_LOG_LEVEL);
        endpointConfig.getLogConfig().setConsoleLevel(SipConstants.SIP_CONSOLE_LOG_LEVEL);
        LogConfig logConfig = endpointConfig.getLogConfig();
        mSipLogWriter = new SipLogWriter();
        if (mSipService.getPreferences().remoteLoggingIsActive()) {
            mSipLogWriter.enabledRemoteLogging(mRemoteLogger);
        }
        logConfig.setWriter(mSipLogWriter);
        logConfig.setDecor(logConfig.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue())
        );
    }

    /**
     * Create the Endpoint and init/start the Endpoint.
     * @return
     * @throws LibraryInitFailedException
     */
    private Endpoint createEndpoint() throws LibraryInitFailedException {
        mRemoteLogger.d(TAG + " createEndpoint");
        Endpoint endpoint = new Endpoint();
        EpConfig endpointConfig = new EpConfig();

        // Set echo cancellation options for endpoint.
        MediaConfig mediaConfig = createMediaConfig(endpointConfig);
        endpointConfig.setMedConfig(mediaConfig);

        try {
            endpoint.libCreate();
        } catch (Exception e) {
            Log.e(TAG, "Unable to create the PJSIP library");
            mRemoteLogger.e(TAG + " " + Log.getStackTraceString(e));
            e.printStackTrace();
            throw new LibraryInitFailedException();
        }

        if (BuildConfig.DEBUG || mSipService.getPreferences().remoteLoggingIsActive()) {
            setSipLogging(endpointConfig);
        }

        UaConfig uaConfig = endpointConfig.getUaConfig();
        uaConfig.setUserAgent(getUserAgentHeader(mSipService));

        try {
            endpoint.libInit(endpointConfig);
        } catch (Exception e) {
            Log.e(TAG, "Unable to init the PJSIP library");
            mRemoteLogger.e(TAG + " " + Log.getStackTraceString(e));
            e.printStackTrace();
            throw new LibraryInitFailedException();
        }

        TransportConfig transportConfig = createTransportConfig();

        try {
            mCurrentTransportId = endpoint.transportCreate(getTransportType(), transportConfig);
            endpoint.libStart();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to start the PJSIP library");
            mRemoteLogger.e(TAG + " " + Log.getStackTraceString(exception));
            throw new LibraryInitFailedException();
        }
        return endpoint;
    }

    /**
     * Create the AccountConfig with the PhoneAccount credentials.
     * @return
     */
    private AccountConfig createAccountConfig() {
        AuthCredInfo credInfo = new AuthCredInfo(
                mSipService.getString(R.string.sip_auth_scheme),
                mSipService.getString(R.string.sip_auth_realm),
                mPhoneAccount.getAccountId(),
                0,
                mPhoneAccount.getPassword()
        );

        String transportString = getTransportString();
        String sipAccountRegId = SipUri.sipAddress(mSipService, mPhoneAccount.getAccountId()) + transportString;
        String sipRegistrarUri = SipUri.prependSIPUri(mSipService, mSipService.getString(R.string.sip_host)) + transportString;

        AccountConfig config = new AccountConfig();
        config.setIdUri(sipAccountRegId);
        config.getRegConfig().setRegistrarUri(sipRegistrarUri);
        config.getSipConfig().getAuthCreds().add(credInfo);
        config.getSipConfig().getProxies().add(sipRegistrarUri);

        return config;
    }

    /**
     * Create the SipAccount for the PhoneAccount.
     * @return
     */
    private SipAccount createSipAccount() {
        mRemoteLogger.d(TAG + " createSipAccount");
        AccountConfig accountConfig = createAccountConfig();
        SipAccount sipAccount = null;
        try {
            sipAccount = new SipAccount(mSipService, accountConfig, this);
        } catch (Exception e) {
            mRemoteLogger.e(TAG + " " + Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return sipAccount;
    }

    /**
     * Set the priority of codecs to use.
     */
    private void setCodecPrio() {
        try {
            CodecInfoVector codecList = mEndpoint.codecEnum();
            String codecId;
            CodecInfo info;
            Short prio;

            for (int i = 1; i < codecList.size(); i++) {
                info = codecList.get(i);
                codecId = info.getCodecId();
                prio = sCodecPrioMapping.get(codecId);
                if (prio != null) {
                    mEndpoint.codecSetPriority(codecId, prio);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clean/destroy all the resources the proper way.
     */
    public void cleanUp() {
        stopNetworkingListener();

        // Prematurely disable logging to avoid crashing the logger on non existing objects.
        if (mSipLogWriter != null) {
            mSipLogWriter.disableRemoteLogging();
        }

        // Destroy Endpoint.
        if(mEndpoint != null) {
            try {
                mEndpoint.libDestroy();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mEndpoint.delete();
                mEndpoint = null;
            }
        }
    }

    private void startNetworkingListener() {
        mSipService.registerReceiver(mNetworkStateReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void stopNetworkingListener() {
        mSipService.unregisterReceiver(mNetworkStateReceiver);
    }

    /**
     * When there is a network connection change start with un-registration.
     * This is needed to tell asterisks that we are connected to a different ip address.
     *
     * @param context
     */
    private void handleNetworkStateChange(Context context) {
        mRemoteLogger.d(TAG + " handleNetworkStateChange");
        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(context);
        Long connectionType = connectivityHelper.getConnectionType();

        if (!mLatestConnectionType.equals(connectionType) && connectivityHelper.hasNetworkConnection()) {
            mReRegisterAccount = true;
            // Renew sip registration. Unregister would come from the new IP anyway so that
            // would not make a lot of sense so we update our existing registration from the
            // new IP.
            try {
                mSipAccount.setRegistration(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mLatestConnectionType = connectionType;
    }

    /**
     * Function to re-register to handle IP changes.
     */
    private void reRegister() {
        // TODO Think about reinvites during settings up transfer.
        try {
            // Set flag for updating contact header IP.
            CallOpParam callOpParam = new CallOpParam(true);
            CallSetting callSetting = callOpParam.getOpt();
            callSetting.setFlag(pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT.swigValue());
            mSipService.getCurrentCall().reinvite(callOpParam);
            mReRegisterAccount = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Response to the middleware on a incoming call to notify asterisk we are ready to accept
     * calls.
     */
    private void respondToMiddleware() {
        Intent incomingCallDetails = mSipService.getIncomingCallDetails();

        if (incomingCallDetails == null) {
            mRemoteLogger.w("Trying to respond to middleware with no details");
            return;
        }

        String url = incomingCallDetails.getStringExtra(SipConstants.EXTRA_RESPONSE_URL);
        String messageStartTime = incomingCallDetails.getStringExtra(FcmListenerService.MESSAGE_START_TIME);
        String token = incomingCallDetails.getStringExtra(SipConstants.EXTRA_REQUEST_TOKEN);

        // Set responded as soon as possible to avoid duplicate requests due to multiple
        // onAccountRegistered calls in a row.
        mHasRespondedToMiddleware = true;

        AnalyticsHelper analyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) mSipService.getApplication()).getDefaultTracker()
        );

        Registration registrationApi = ServiceGenerator.createService(
                mSipService,
                Registration.class,
                url
        );

        ConnectivityHelper connectivityHelper = ConnectivityHelper.get(mSipService);
        String connectionType = connectivityHelper.getConnectionTypeString();
        String analyticsLabel;
        if (connectionType.equals(connectivityHelper.CONNECTION_WIFI)) {
            analyticsLabel = mSipService.getString(R.string.analytics_event_label_wifi);
        } else {
            analyticsLabel = mSipService.getString(R.string.analytics_event_label_4g);
        }

        // Accepted event.
        analyticsHelper.sendEvent(
                mSipService.getString(R.string.analytics_event_category_middleware),
                mSipService.getString(R.string.analytics_event_action_middleware_accepted),
                analyticsLabel
        );

        long startTime = (long) (Double.parseDouble(messageStartTime) * 1000);  // To ms.
        long startUpTime = System.currentTimeMillis() - startTime;

        // Response timing.
        analyticsHelper.sendTiming(
                mSipService.getString(R.string.analytics_event_category_middleware),
                mSipService.getString(R.string.analytics_event_name_call_response),
                startUpTime
        );

        retrofit2.Call<ResponseBody> call = registrationApi.reply(token, true, messageStartTime);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccess()) {
                    mRemoteLogger.w(
                            "Unsuccessful response to middleware: " + Integer.toString(response.code()));
                    mSipService.stopSelf();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                mRemoteLogger.w("Failed sending response to middleware");
                mSipService.stopSelf();
            }
        });
    }

    @Override
    public void onAccountRegistered(Account account, OnRegStateParam param) {
        mRemoteLogger.d(TAG + " onAccountRegistered");

        try {
            // After registration setup new transport to reflect learned external IP to be used
            // by sip headers.
            mEndpoint.transportClose(mCurrentTransportId);
            mCurrentTransportId = mEndpoint.transportCreate(getTransportType(), createTransportConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // After the account has registered try to re-invite the current call
        // so the contact uri will be updated for the call.
        if (mReRegisterAccount) {
            reRegister();
        }

        // Check if it is an incoming call and we did not respond to the middleware already.
        if (mSipService.getInitialCallType().equals(SipConstants.ACTION_VIALER_INCOMING) && !mHasRespondedToMiddleware) {
            respondToMiddleware();
        }
    }

    @Override
    public void onAccountUnregistered(Account account, OnRegStateParam param) {
        mRemoteLogger.d(TAG + " onAccountUnRegistered");
    }

    @Override
    public void onAccountInvalidState(Account account, Throwable fault) {
        mRemoteLogger.d(TAG + " onAccountInvalidState");
    }

    /**
     * Public inner class used for exception handling.
     */
    public class LibraryInitFailedException extends Exception {

    }
}
