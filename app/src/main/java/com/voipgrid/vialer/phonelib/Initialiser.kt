package com.voipgrid.vialer.phonelib

import android.content.Context
import android.util.ArraySet
import android.util.Log
import com.voipgrid.vialer.R
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.api.SecureCalling
import com.voipgrid.vialer.api.ServiceGenerator
import com.voipgrid.vialer.api.models.PhoneAccount
import com.voipgrid.vialer.fcm.RemoteMessageData
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.persistence.VoipSettings
import com.voipgrid.vialer.sip.CallSetupChecker
import com.voipgrid.vialer.sip.SipConstants
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.UserAgent
import okhttp3.ResponseBody
import org.openvoipalliance.phonelib.PhoneLib
import org.openvoipalliance.phonelib.model.Codec
import org.openvoipalliance.phonelib.model.RegistrationState
import org.openvoipalliance.phonelib.repository.initialise.SessionCallback
import org.openvoipalliance.phonelib.repository.registration.RegistrationCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Initialiser(private val context: Context, private val softPhone: SoftPhone) {

    private val mLogger = Logger(this)
    private val logListener = PhoneLibLogger()
    
    private val port
        get() = if (shouldUseTls()) "5061" else "5060"
    
    private val stun
        get() = if (User.voip.hasStunEnabled) context.resources.getStringArray(R.array.stun_hosts)[0] else null

    private var onRegister: (() -> Unit)? = null
    private var onFailure: (() -> Unit)? = null

    fun initLibrary(callback: SessionCallback?, onRegister: (() -> Unit), onFailure: (() -> Unit)) {
        this.onRegister = onRegister
        this.onFailure = onFailure

        val account = User.voipAccount ?: return

        softPhone.phone = PhoneLib.getInstance(context)
        softPhone.phone?.setAudioCodecs(setOf(if (User.voip.audioCodec != VoipSettings.AudioCodec.OPUS) Codec.ILBC else Codec.OPUS))

        softPhone.phone?.apply {
            initialise()
            setUserAgent(UserAgent(context).generate())
            setSessionCallback(callback)
            setLogListener(logListener)
            register(account.accountId, account.password, sipHost, port, stun, shouldUseTls(), object : RegistrationCallback() {
                override fun stateChanged(registrationState: RegistrationState) {
                    if (registrationState == RegistrationState.REGISTERED) {
                        this@Initialiser.onRegister?.invoke()
                        this@Initialiser.onRegister = null
                        this@Initialiser.onFailure = null
                    }

                    if (registrationState == RegistrationState.FAILED) {
                        this@Initialiser.onFailure?.invoke()
                        this@Initialiser.onRegister = null
                        this@Initialiser.onFailure = null
                    }
                }
            })
        }
    }

    fun destroy() {
        onRegister = null
        onFailure = null
        softPhone.phone?.destroy()
        softPhone.phone = null
    }

    /**
     * Response to the middleware on a incoming call to notify asterisk we are ready to accept
     * calls.
     */
    fun respondToMiddleware(sipService: SipService) {
        val incomingCallDetails = sipService.incomingCallDetails

        if (incomingCallDetails == null) {
            mLogger.w("Trying to respond to middleware with no details")
            return
        }

        val url = incomingCallDetails.getStringExtra(SipConstants.EXTRA_RESPONSE_URL)
        val messageStartTime = incomingCallDetails.getStringExtra(RemoteMessageData.MESSAGE_START_TIME)
        val token = incomingCallDetails.getStringExtra(SipConstants.EXTRA_REQUEST_TOKEN)
        val attempt = incomingCallDetails.getStringExtra(RemoteMessageData.ATTEMPT)

        val middlewareApi = ServiceGenerator.createRegistrationService(sipService)

        val call = middlewareApi.reply(
                token,
                true,
                messageStartTime,
                User.voipAccount?.accountId ?: ""
        )

        call.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (!response.isSuccessful) {
                    mLogger.w(
                            "Unsuccessful response to middleware: " + Integer.toString(response.code()))
                    sipService.stop()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                mLogger.w("Failed sending response to middleware")
                sipService.stop()
            }
        })

        CallSetupChecker.withPushMessageInformation(token, messageStartTime, attempt).start(sipService)
    }

    companion object {
        /**
         * Find the current SIP domain that should be used for all calls.
         *
         * @return The domain as a string
         */
        @JvmStatic
        val sipHost: String
            get() = get().getString(if (shouldUseTls()) R.string.sip_host_secure else R.string.sip_host)

        /**
         * Determine if TLS should be used for all calls.
         *
         * @return TRUE if TLS should be used
         */
        fun shouldUseTls(): Boolean {
            return SecureCalling.fromContext(get()).isEnabled
        }
    }
}