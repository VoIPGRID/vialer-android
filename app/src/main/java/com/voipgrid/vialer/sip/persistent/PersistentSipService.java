package com.voipgrid.vialer.sip.persistent;

import static com.voipgrid.vialer.sip.SipConfig.TRANSPORT_TYPE_SECURE;
import static com.voipgrid.vialer.sip.SipConfig.TRANSPORT_TYPE_STANDARD;

import static org.pjsip.pjsua2.pj_constants_.PJ_TRUE;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_REINIT_MEDIA;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_CONTACT;
import static org.pjsip.pjsua2.pjsua_call_flag.PJSUA_CALL_UPDATE_VIA;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.logging.LogHelper;
import com.voipgrid.vialer.sip.CodecPriorityMap;
import com.voipgrid.vialer.sip.SipConfig;
import com.voipgrid.vialer.sip.SipConstants;
import com.voipgrid.vialer.sip.SipLogWriter;
import com.voipgrid.vialer.sip.SipUri;
import com.voipgrid.vialer.sip.VialerEndpoint;
import com.voipgrid.vialer.util.UserAgent;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.CodecInfo;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.MediaConfig;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pjmedia_srtp_use;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PersistentSipService extends Service {

    @Inject SipConfig mSipConfig;
    @Inject PhoneAccount mPhoneAccount;
    private Endpoint mEndpoint;
    private SipAccount mSipAccount;
    private SipLogWriter mSipLogWriter;
    @Inject Preferences mPreferences;

    public PersistentSipService() {
        super();
        VialerApplication.get().component().inject(this);
    }

    public static void start() {
        VialerApplication.get().startService(new Intent(VialerApplication.get(), PersistentSipService.class));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        loadPjsip();
        mEndpoint = createEndpoint();
        setCodecPrio();
        mSipAccount = createSipAccount();
        return START_STICKY;
    }

    private void loadPjsip() {
        try {
            System.loadLibrary("pjsua2");
        } catch (UnsatisfiedLinkError e) {
            Log.e("TEST123", "failed", e);
        }
    }

    private VialerEndpoint createEndpoint() {
        VialerEndpoint endpoint = new VialerEndpoint();
        EpConfig endpointConfig = new EpConfig();

        // Set echo cancellation options for endpoint.
        MediaConfig mediaConfig = createMediaConfig(endpointConfig);
        endpointConfig.setMedConfig(mediaConfig);
        try {
            endpoint.libCreate();
        } catch (Exception e) {
            Log.e("TEST123", "e", e);
        }

        if (BuildConfig.DEBUG || mPreferences.remoteLoggingIsActive()) {
            setSipLogging(endpointConfig);
        }

        UaConfig uaConfig = endpointConfig.getUaConfig();
        uaConfig.setUserAgent(new UserAgent(this).generate());
        uaConfig.setMainThreadOnly(true);


        try {
            endpoint.libInit(endpointConfig);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TEST123", "e", e);
        }

        TransportConfig transportConfig = createTransportConfig();
        try {
            endpoint.transportCreate(getTransportType(), transportConfig);
            endpoint.libStart();
        } catch (Exception e) {
            Log.e("TEST123", "e", e);
        }

        return endpoint;
    }

    /**
     * Create the AccountConfig with the PhoneAccount credentials.
     * @return
     */
    private AccountConfig createAccountConfig() {
        AuthCredInfo credInfo = new AuthCredInfo(
                getString(R.string.sip_auth_scheme),
                getString(R.string.sip_auth_realm),
                "169710098",
                0,
                "KBGVRmRYcrG7wtS"
        );

        String transportString = getTransportString();
        String sipAccountRegId = SipUri.sipAddress(this, "169710098") + transportString;
        String sipRegistrarUri = SipUri.prependSIPUri(this, getSipHost()) + transportString;

        AccountConfig config = new AccountConfig();
        config.setIdUri(sipAccountRegId);
        config.getRegConfig().setRegistrarUri(sipRegistrarUri);
        config.getSipConfig().getAuthCreds().add(credInfo);
        config.getSipConfig().getProxies().add(sipRegistrarUri);
        config.getRegConfig().setTimeoutSec(10);

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

    private String getTransportString() {
        return ";transport=" + getSipTransportType();
    }

    @NonNull
    private String getSipTransportType() {
        if (!shouldUseTls()) {
            return TRANSPORT_TYPE_STANDARD;
        }

        return TRANSPORT_TYPE_SECURE;
    }

    /**
     * Create the SipAccount for the PhoneAccount.
     * @return
     */
    private SipAccount createSipAccount() {
        AccountConfig accountConfig = createAccountConfig();
        SipAccount sipAccount = null;
        try {
            sipAccount = new SipAccount(accountConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sipAccount;
    }

    /**
     * Set the priority of codecs to use.
     */
    private void setCodecPrio() {
        try {
            CodecPriorityMap codecPriorityMap = CodecPriorityMap.get();
            CodecInfoVector codecList = mEndpoint.codecEnum();
            String codecId;
            CodecInfo info;
            Short prio;

            for (int i = 0; i < codecList.size(); i++) {
                info = codecList.get(i);
                codecId = info.getCodecId();
                prio = codecPriorityMap.findCodecPriority(codecId);
                mEndpoint.codecSetPriority(codecId, prio != null ? prio : CodecPriorityMap.CODEC_DISABLED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean shouldUseTls() {
        return SecureCalling.fromContext(VialerApplication.get()).isEnabled();
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
    private void setSipLogging(EpConfig endpointConfig) {
        endpointConfig.getLogConfig().setLevel(SipConstants.SIP_LOG_LEVEL);
        endpointConfig.getLogConfig().setConsoleLevel(SipConstants.SIP_CONSOLE_LOG_LEVEL);
        LogConfig logConfig = endpointConfig.getLogConfig();
        mSipLogWriter = new SipLogWriter();
        logConfig.setWriter(mSipLogWriter);
        logConfig.setDecor(logConfig.getDecor() &
                ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                        pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue())
        );
    }

    private TransportConfig createTransportConfig() {
        TransportConfig config = new TransportConfig();
        return config;
    }

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

}
