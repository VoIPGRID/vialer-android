package com.voipgrid.vialer.sip;

import static org.pjsip.pjsua2.pj_constants_.PJ_TRUE;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_REINIT_MEDIA;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_VIA;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.analytics.AnalyticsApplication;
import com.voipgrid.vialer.analytics.AnalyticsHelper;
import com.voipgrid.vialer.api.Registration;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.fcm.RemoteMessageData;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.logging.sip.SipLogHandler;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.UserAgent;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IpChangeParam;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.MediaConfig;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjmedia_srtp_use;
import org.pjsip.pjsua2.pjsip_transport_type_e;
import org.pjsip.pjsua2.pjsua_call_flag;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Class that holds the sip backend (Endpoint + SipAccount).
 */
public class SipConfig implements AccountStatus {

    private static final String TAG = SipConfig.class.getSimpleName();

    private VialerEndpoint mEndpoint;
    private PhoneAccount mPhoneAccount;
    private RemoteLogger mRemoteLogger;
    private SipAccount mSipAccount;
    private SipLogWriter mSipLogWriter;
    private SipService mSipService;
    private Preferences mPreferences;

    private boolean mHasRespondedToMiddleware = false;
    private int mCurrentTransportId;
    private static Map<String, Short> sCodecPrioMapping;
    private boolean isChangingNetwork = false;

    public static final short CODEC_DISABLED = (short) 0;
    public static final short CODEC_PRIORITY_MAX = (short) 255;

    public static final String TRANSPORT_TYPE_SECURE = "tls";
    public static final String TRANSPORT_TYPE_STANDARD = "tcp";

    static {
        sCodecPrioMapping = new HashMap<>();
        sCodecPrioMapping.put("ilbc/8000", CODEC_PRIORITY_MAX);
    }

    public SipConfig(SipService sipService, PhoneAccount phoneAccount) {
        mSipService = sipService;
        mPhoneAccount = phoneAccount;
        mRemoteLogger = mSipService.getRemoteLogger();
        mPreferences = new Preferences(VialerApplication.get());
    }

    public VialerEndpoint getEndpoint() {
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
        startNetworkingListener();
    }

    private static final int NETWORK_SWITCH_DELAY_MS = 500;

    private BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isChangingNetwork) return;

            mRemoteLogger.d("Received a network change: " + intent.getAction());

            if(isInitialStickyBroadcast()) {
                mRemoteLogger.i("Ignoring network change as broadcast is old (sticky).");
                return;
            }

            isChangingNetwork = true;

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                mRemoteLogger.d("Wait " + NETWORK_SWITCH_DELAY_MS + "ms before doing the network switch");
                doIpSwitch();
                isChangingNetwork = false;
            }, NETWORK_SWITCH_DELAY_MS);
        }
    };

    /**
     * When there is a change in the network make use of the PJSIP handleIpChange
     * functionality to handle the change in the network.
     */
    private void doIpSwitch() {
        mRemoteLogger.v("doIpSwitch()");
        IpChangeParam ipChangeParam = new IpChangeParam();
        ipChangeParam.setRestartListener(false);

        SipCall sipCall = null;
        String currentCallState = "";
        if (mSipService != null && mSipService.getCurrentCall() != null) {
            sipCall = mSipService.getCurrentCall();
            sipCall.setIsIPChangeInProgress(true);
            currentCallState = sipCall.getCurrentCallState();
        }

        if (sipCall == null) {
            return;
        }

        mRemoteLogger.i("Make PJSIP handle the ip address change.");
        try {
            mEndpoint.handleIpChange(ipChangeParam);
        } catch (Exception e) {
            mRemoteLogger.w("PJSIP failed to change the ip address");
            e.printStackTrace();
        }
    }

    private void startNetworkingListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(SipLogHandler.NETWORK_UNAVAILABLE_BROADCAST);
        mSipService.registerReceiver(
                mNetworkStateReceiver,
                filter
        );
    }

    private void stopNetworkingListener() {
        try {
            mSipService.unregisterReceiver(mNetworkStateReceiver);
        } catch(IllegalArgumentException e) {
            mRemoteLogger.w("Trying to unregister mNetworkStateReceiver not registered.");
        }
    }

    /**
     * Load the PJSIP library.
     * @throws LibraryInitFailedException
     */
    private void loadPjsip() throws LibraryInitFailedException {
        mRemoteLogger.d("Loading PJSIP");
        try {
            System.loadLibrary("pjsua2");
        } catch (UnsatisfiedLinkError error) { /* Can not load PJSIP library */
            mRemoteLogger.e("" + Log.getStackTraceString(error));
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
        return ";transport=" + getSipTransportType();
    }

    /**
     * Function to get the transport type based on a setting.
     * @return
     */
    private pjsip_transport_type_e getTransportType() {
        String sipTransport = getSipTransportType();

        pjsip_transport_type_e transportType = pjsip_transport_type_e.PJSIP_TRANSPORT_UDP;
        if (sipTransport.equals(TRANSPORT_TYPE_STANDARD)) {
            transportType = pjsip_transport_type_e.PJSIP_TRANSPORT_TCP;
        } else if (sipTransport.equals(TRANSPORT_TYPE_SECURE)) {
            transportType = pjsip_transport_type_e.PJSIP_TRANSPORT_TLS;
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
        mSipLogWriter.enabledRemoteLogging(mRemoteLogger);
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
    private VialerEndpoint createEndpoint() throws LibraryInitFailedException {
        mRemoteLogger.d("createEndpoint");
        VialerEndpoint endpoint = new VialerEndpoint();
        EpConfig endpointConfig = new EpConfig();

        // Set echo cancellation options for endpoint.
        MediaConfig mediaConfig = createMediaConfig(endpointConfig);
        endpointConfig.setMedConfig(mediaConfig);
        try {
            endpoint.libCreate();
        } catch (Exception e) {
            Log.e(TAG, "Unable to create the PJSIP library");
            mRemoteLogger.e("" + Log.getStackTraceString(e));
            e.printStackTrace();
            throw new LibraryInitFailedException();
        }

        if (BuildConfig.DEBUG || mSipService.getPreferences().remoteLoggingIsActive()) {
            setSipLogging(endpointConfig);
        }

        UaConfig uaConfig = endpointConfig.getUaConfig();
        uaConfig.setUserAgent(new UserAgent(mSipService).generate());

        configureStunServer(uaConfig);

        try {
            endpoint.libInit(endpointConfig);
        } catch (Exception e) {
            Log.e(TAG, "Unable to init the PJSIP library");
            mRemoteLogger.e("" + Log.getStackTraceString(e));
            e.printStackTrace();
            throw new LibraryInitFailedException();
        }

        TransportConfig transportConfig = createTransportConfig();
        try {
            mCurrentTransportId = endpoint.transportCreate(getTransportType(), transportConfig);
            endpoint.libStart();
        } catch (Exception exception) {
            Log.e(TAG, "Unable to start the PJSIP library");
            mRemoteLogger.e("" + Log.getStackTraceString(exception));
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
        String sipRegistrarUri = SipUri.prependSIPUri(mSipService, getSipHost()) + transportString;

        AccountConfig config = new AccountConfig();
        config.setIdUri(sipAccountRegId);
        config.getRegConfig().setRegistrarUri(sipRegistrarUri);
        config.getSipConfig().getAuthCreds().add(credInfo);
        config.getSipConfig().getProxies().add(sipRegistrarUri);

        // TLS Configuration
        if (shouldUseTls()) {
            config.getMediaConfig().setSrtpSecureSignaling(1);
            config.getMediaConfig().setSrtpUse(pjmedia_srtp_use.PJMEDIA_SRTP_MANDATORY);
        }

        // IP Address account configuration
        config.getIpChangeConfig().setShutdownTp(false);
        config.getIpChangeConfig().setHangupCalls(false);
        config.getIpChangeConfig().setReinviteFlags(PJSUA_CALL_UPDATE_CONTACT.swigValue() | PJSUA_CALL_REINIT_MEDIA.swigValue() | PJSUA_CALL_UPDATE_VIA.swigValue());

        config.getNatConfig().setContactRewriteUse(PJ_TRUE.swigValue());
        config.getNatConfig().setContactRewriteMethod(4);


        return config;
    }

    @NonNull
    private String getSipTransportType() {
        if (!shouldUseTls()) {
            LogHelper.using(mRemoteLogger).logNoTlsReason();

            return TRANSPORT_TYPE_STANDARD;
        }

        return TRANSPORT_TYPE_SECURE;
    }

    /**
     * Create the SipAccount for the PhoneAccount.
     * @return
     */
    private SipAccount createSipAccount() {
        mRemoteLogger.d("createSipAccount");
        AccountConfig accountConfig = createAccountConfig();
        SipAccount sipAccount = null;
        try {
            sipAccount = new SipAccount(mSipService, accountConfig, this);
        } catch (Exception e) {
            mRemoteLogger.e("" + Log.getStackTraceString(e));
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

            for (int i = 0; i < codecList.size(); i++) {
                info = codecList.get(i);
                codecId = info.getCodecId();
                prio = findCodecPriority(codecId);
                mEndpoint.codecSetPriority(codecId, prio != null ? prio : CODEC_DISABLED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches and normalizes the codec priority mapping and attempts to find the mapped priority.
     *
     * @param codecId
     * @return Short The codec's priority.
     */
    private Short findCodecPriority(String codecId) {
        for(String codec : sCodecPrioMapping.keySet()) {
            if(codecId.toLowerCase().contains(codec.toLowerCase())) {
                return sCodecPrioMapping.get(codec);
            }
        }

        return null;
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
        String messageStartTime = incomingCallDetails.getStringExtra(RemoteMessageData.MESSAGE_START_TIME);
        String token = incomingCallDetails.getStringExtra(SipConstants.EXTRA_REQUEST_TOKEN);
        String attempt = incomingCallDetails.getStringExtra(RemoteMessageData.ATTEMPT);

        // Set responded as soon as possible to avoid duplicate requests due to multiple
        // onAccountRegistered calls in a row.
        mHasRespondedToMiddleware = true;

        AnalyticsHelper analyticsHelper = new AnalyticsHelper(
                ((AnalyticsApplication) mSipService.getApplication()).getDefaultTracker()
        );

        Registration registrationApi = ServiceGenerator.createRegistrationService(mSipService);

        String analyticsLabel = ConnectivityHelper.get(mSipService).getAnalyticsLabel();

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
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    mRemoteLogger.w(
                            "Unsuccessful response to middleware: " + Integer.toString(response.code()));
                    mSipService.stopSelf();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                mRemoteLogger.w("Failed sending response to middleware");
                mSipService.stopSelf();
            }
        });

        CallSetupChecker.withPushMessageInformation(token, messageStartTime, attempt).start(mSipService);
    }

    @Override
    public void onAccountRegistered(Account account, OnRegStateParam param) {
        mRemoteLogger.d("onAccountRegistered");

        if (mSipService.getCurrentCall() != null) {
            SipCall sipCall = mSipService.getCurrentCall();
            if (sipCall.isIpChangeInProgress() && sipCall.getCurrentCallState().equals(SipConstants.CALL_INCOMING_RINGING)) {
                CallOpParam callOpParam = new CallOpParam();
                callOpParam.setOptions(pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT.swigValue());
                try {
                    sipCall.reinvite(callOpParam);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Check if it is an incoming call and we did not respond to the middleware already.
        if (mSipService.getInitialCallType().equals(SipConstants.ACTION_CALL_INCOMING) && !mHasRespondedToMiddleware) {
            respondToMiddleware();
        }
    }

    @Override
    public void onAccountUnregistered(Account account, OnRegStateParam param) {
        mRemoteLogger.d("onAccountUnRegistered");
    }

    @Override
    public void onAccountInvalidState(Account account, Throwable fault) {
        mRemoteLogger.d("onAccountInvalidState");
    }

    /**
     * Public inner class used for exception handling.
     */
    public class LibraryInitFailedException extends Exception {

    }

    /**
     * Use a stun server if one has been configured.
     *
     * @param uaConfig
     */
    private void configureStunServer(UaConfig uaConfig) {
        if (!mPreferences.hasStunEnabled()) {
            mRemoteLogger.i("User has disabled using STUN via settings menu");
            return;
        }

        String[] stunHosts = mSipService.getResources().getStringArray(R.array.stun_hosts);

        if(stunHosts.length <= 0) return;

        StringVector stun = new StringVector();

        for(String stunHost : stunHosts) {
            mRemoteLogger.i("Configuring STUN server: " + stunHost);
            stun.add(stunHost);
        }

        uaConfig.setStunServer(stun);
    }

    /**
     * Find the current SIP domain that should be used for all calls.
     *
     * @return The domain as a string
     */
    @NonNull
    public static String getSipHost() {
        return VialerApplication.get().getString(shouldUseTls() ? R.string.sip_host_secure : R.string.sip_host);
    }

    /**
     * Determine if TLS should be used for all calls.
     *
     * @return TRUE if TLS should be used
     */
    public static boolean shouldUseTls() {
        return SecureCalling.fromContext(VialerApplication.get()).isEnabled();
    }
}
