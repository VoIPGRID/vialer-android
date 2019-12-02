package com.voipgrid.vialer.sip.pjsip;

import static org.pjsip.pjsua2.pj_constants_.PJ_TRUE;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_REINIT_MEDIA;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_VIA;

import androidx.annotation.NonNull;

import android.content.Context;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.util.UserAgent;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.MediaConfig;
import org.pjsip.pjsua2.StringVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjmedia_srtp_use;
import org.pjsip.pjsua2.pjsip_transport_type_e;

/**
 * Class that holds the sip backend (Endpoint + SipAccount).
 */
public class PjsipConfigurator {

    private final Context context;
    private Logger logger = new Logger(this);
    private PjsipLogWriter mSipLogWriter;

    private static final String TRANSPORT_TYPE_SECURE = "tls";
    private static final String TRANSPORT_TYPE_STANDARD = "tcp";

    public PjsipConfigurator(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Create the Endpoint and init/start the Endpoint.
     * @return
     * @throws LibraryInitFailedException
     */
    @NonNull
    public Pjsip.VialerEndpoint initializeEndpoint(@NonNull Pjsip.VialerEndpoint endpoint) throws LibraryInitFailedException {
        logger.d("createEndpoint");
        EpConfig endpointConfig = new EpConfig();

        // Set echo cancellation options for endpoint.
        MediaConfig mediaConfig = createMediaConfig(endpointConfig);
        endpointConfig.setMedConfig(mediaConfig);
        try {
            endpoint.libCreate();
        } catch (Exception e) {
            logger.e("Unable to create the PJSIP library");
            logger.e("" + Log.getStackTraceString(e));
            e.printStackTrace();
            throw new LibraryInitFailedException();
        }

        if (BuildConfig.DEBUG || User.remoteLogging.isEnabled()) {
            setSipLogging(endpointConfig);
        }

        UaConfig uaConfig = endpointConfig.getUaConfig();
        uaConfig.setUserAgent(new UserAgent(context).generate());
        uaConfig.setMainThreadOnly(true);
        configureStunServer(uaConfig);

        try {
            endpoint.libInit(endpointConfig);
        } catch (Exception e) {
            logger.e("Unable to init the PJSIP library");
            logger.e("" + Log.getStackTraceString(e));
            e.printStackTrace();
            throw new LibraryInitFailedException();
        }

        TransportConfig transportConfig = createTransportConfig();
        try {
            endpoint.transportCreate(getTransportType(), transportConfig);
            endpoint.libStart();
        } catch (Exception exception) {
            logger.e("Unable to start the PJSIP library");
            logger.e("" + Log.getStackTraceString(exception));
            throw new LibraryInitFailedException();
        }

        setCodecPriority(endpoint);

        return endpoint;
    }

    /**
     * Create the AccountConfig with the PhoneAccount credentials.
     * @return
     */
    @NonNull
    public AccountConfig createAccountConfig(@NonNull PhoneAccount account) {
        AuthCredInfo credInfo = new AuthCredInfo(
                context.getString(R.string.sip_auth_scheme),
                context.getString(R.string.sip_auth_realm),
                account.getAccountId(),
                0,
                account.getPassword()
        );

        String transportString = getTransportString();
        String sipAccountRegId = SipUri.sipAddress(context, account.getAccountId()) + transportString;
        String sipRegistrarUri = SipUri.prependSIPUri(context, getSipHost()) + transportString;

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
        mSipLogWriter = new PjsipLogWriter();
        mSipLogWriter.enabledRemoteLogging(logger);
        logConfig.setWriter(mSipLogWriter);
        logConfig.setDecor(logConfig.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue())
        );
    }

    @NonNull
    private String getSipTransportType() {
        if (!shouldUseTls()) {
            LogHelper.using(logger).logNoTlsReason();

            return TRANSPORT_TYPE_STANDARD;
        }

        return TRANSPORT_TYPE_SECURE;
    }

    /**
     * Set the priority of codecs to use.
     */
    private void setCodecPriority(@NonNull Pjsip.VialerEndpoint endpoint) {
        try {
            PjsipCodecPriorityMap codecPriorityMap = PjsipCodecPriorityMap.get();
            CodecInfoVector codecList = endpoint.codecEnum();
            String codecId;
            CodecInfo info;
            Short prio;

            for (int i = 0; i < codecList.size(); i++) {
                info = codecList.get(i);
                codecId = info.getCodecId();
                prio = codecPriorityMap.findCodecPriority(codecId);
                endpoint.codecSetPriority(codecId, prio != null ? prio : PjsipCodecPriorityMap.CODEC_DISABLED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clean/destroy all the resources the proper way.
     */
    public void cleanUp() {
        if (mSipLogWriter != null) {
            mSipLogWriter.disableRemoteLogging();
        }
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
        if (!User.voip.getHasTlsEnabled()) {
            logger.i("User has disabled using STUN via settings menu");
            return;
        }

        String[] stunHosts = context.getResources().getStringArray(R.array.stun_hosts);

        if(stunHosts.length <= 0) return;

        StringVector stun = new StringVector();

        for(String stunHost : stunHosts) {
            logger.i("Configuring STUN server: " + stunHost);
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
    private static boolean shouldUseTls() {
        return SecureCalling.fromContext(VialerApplication.get()).isEnabled();
    }
}
