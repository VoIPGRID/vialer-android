package com.voipgrid.vialer.sip;


import android.content.Intent;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.Middleware;
import com.voipgrid.vialer.api.SecureCalling;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.fcm.RemoteMessageData;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.phonelib.SessionCallback;
import com.voipgrid.vialer.util.UserAgent;

import org.jetbrains.annotations.NotNull;
import org.openvoipalliance.phonelib.PhoneLib;
import org.openvoipalliance.phonelib.model.RegistrationState;
import org.openvoipalliance.phonelib.repository.registration.RegistrationCallback;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;

public class SipConfig {

    private PhoneAccount mPhoneAccount;
    private Logger mLogger = new Logger(this);
    private SipService mSipService;

    private boolean shouldResponseToMiddlewareOnRegistration = false;
    private boolean mHasRespondedToMiddleware = false;

    /**
     * Initialise the sip service with the relevant details.
     *
     * @param sipService
     * @param phoneAccount
     * @param shouldResponseToMiddlewareOnRegistration Set to TRUE if when the account has been registered, that a reply should be sent to the middleware.
     * @return
     */
    public SipConfig init(SipService sipService, PhoneAccount phoneAccount, boolean shouldResponseToMiddlewareOnRegistration) {
        mSipService = sipService;
        mPhoneAccount = phoneAccount;
        this.shouldResponseToMiddlewareOnRegistration = shouldResponseToMiddlewareOnRegistration;

        return this;
    }

    void initLibrary() {
        PhoneLib.getInstance(mSipService).setUserAgent(new UserAgent(mSipService).generate());
        PhoneLib.getInstance(mSipService).setSessionCallback(new SessionCallback(
                LocalBroadcastManager.getInstance(mSipService))
        );

        PhoneLib.getInstance(mSipService).register(
                mPhoneAccount.getAccountId(),
                mPhoneAccount.getPassword(),
                getSipHost(),
                null,
                User.voip.getHasStunEnabled() ? mSipService.getResources().getStringArray(R.array.stun_hosts)[0] : null,
                shouldUseTls(),
                new RegistrationCallback() {
                    @Override
                    public void stateChanged(@NotNull final RegistrationState registrationState) {
                        super.stateChanged(registrationState);

                        if (registrationState == RegistrationState.REGISTERED) {
                            if (shouldResponseToMiddlewareOnRegistration && !mHasRespondedToMiddleware) {
                                respondToMiddleware();
                            }
                        }
                    }
                });
    }

    /**
     * Response to the middleware on a incoming call to notify asterisk we are ready to accept
     * calls.
     */
    private void respondToMiddleware() {
        Intent incomingCallDetails = mSipService.getIncomingCallDetails();

        if (incomingCallDetails == null) {
            mLogger.w("Trying to respond to middleware with no details");
            return;
        }

        String url = incomingCallDetails.getStringExtra(SipConstants.EXTRA_RESPONSE_URL);
        String messageStartTime = incomingCallDetails.getStringExtra(RemoteMessageData.MESSAGE_START_TIME);
        String token = incomingCallDetails.getStringExtra(SipConstants.EXTRA_REQUEST_TOKEN);
        String attempt = incomingCallDetails.getStringExtra(RemoteMessageData.ATTEMPT);

        // Set responded as soon as possible to avoid duplicate requests due to multiple
        // onAccountRegistered calls in a row.
        mHasRespondedToMiddleware = true;

        Middleware middlewareApi = ServiceGenerator.createRegistrationService(mSipService);

        String sipUserId = "";

        if (User.getVoipAccount() != null && User.getVoipAccount().getAccountId() != null) {
            sipUserId = User.getVoipAccount().getAccountId();
        }

        retrofit2.Call<ResponseBody> call = middlewareApi.reply(
                token,
                true,
                messageStartTime,
                sipUserId
        );
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    mLogger.w(
                            "Unsuccessful response to middleware: " + Integer.toString(response.code()));
                    mSipService.stopSelf();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ResponseBody> call, @NonNull Throwable t) {
                mLogger.w("Failed sending response to middleware");
                mSipService.stopSelf();
            }
        });

        CallSetupChecker.withPushMessageInformation(token, messageStartTime, attempt).start(mSipService);
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
