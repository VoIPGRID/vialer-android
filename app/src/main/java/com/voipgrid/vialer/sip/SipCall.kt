package com.voipgrid.vialer.sip

import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.SipInvite.CallerInformationHeader
import com.voipgrid.vialer.sip.pjsip.Pjsip.SipAccount
import com.voipgrid.vialer.util.PhoneNumberUtils
import org.pjsip.pjsua2.*

/**
 * Call class used to interact with a call.
 */
class SipCall : Call {

    val thirdParty: CallerInformationHeader
    val state = State()
    private val logger = Logger(this)
    private val sipService: SipService
    private val direction: Direction
    private var invite: SipInvite? = null

    val isConnected
        get() = state.telephonyState != TelephonyState.INITIALIZING && state.telephonyState != TelephonyState.DISCONNECTED

    private val pjsipState
        get() = info.state

    val duration: Int
        get() = try { info.connectDuration.sec } catch (e: Exception) { -1 }

    val codec: String
        get() = try { getStreamInfo(0).codecName } catch (e: Exception) { "" }

    val phoneNumber: String
        get() = PhoneNumberUtils.maskAnonymousNumber(thirdParty.number)

    val callerId: String
        get() = thirdParty.name

    val isOutgoing: Boolean
        get() = direction == Direction.OUTGOING

    val isIncoming: Boolean
        get() = direction == Direction.INCOMING


    /**
     * Create an outgoing call.
     *
     */
    constructor(sipService: SipService, sipAccount: SipAccount, phoneNumber: String) : super(sipAccount) {
        this.sipService = sipService
        direction = Direction.OUTGOING
        this.thirdParty = CallerInformationHeader("", phoneNumber)
        super.makeCall(SipUri.sipAddress(get(), phoneNumber), CallOpParam().apply { statusCode = pjsip_status_code.PJSIP_SC_RINGING })
    }

    /**
     * Create an incoming call.
     *
     */
    constructor(sipService: SipService, sipAccount: SipAccount, callId: Int, invite: SipInvite) : super(sipAccount, callId) {
        this.sipService = sipService
        this.invite = invite
        direction = Direction.INCOMING
        thirdParty = when {
            invite.hasPAssertedIdentity() -> invite.pAssertedIdentity
            invite.hasRemotePartyId() -> invite.remotePartyId
            invite.hasFrom() -> invite.from
            else -> CallerInformationHeader("UNABLE TO GET CIH", "WAS UNABLE TO GET CIH")
        }
    }

    override fun onCallTsxState(prm: OnCallTsxStateParam) {
        super.onCallTsxState(prm)

        if (state.telephonyState != TelephonyState.INCOMING_RINGING) return

        val packet = prm.e.body.tsxState.src.rdata.wholeMsg

        if (packet.isEmpty()) return

        when {
            packet.contains(CallMissedReason.CALL_ORIGINATOR_CANCEL.packetLookupString) -> {}
            packet.contains(CallMissedReason.CALL_COMPLETED_ELSEWHERE.packetLookupString) -> {}
            else -> sipService.callWasMissed(this)
        }
    }

    @Throws(Exception::class)
    fun answer() {
        answerWithCode(pjsip_status_code.PJSIP_SC_ACCEPTED)
    }

    @Throws(Exception::class)
    fun decline() {
        hangupWithStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE)
    }

    @Throws(Exception::class)
    fun hangup(userHangup: Boolean) {
        state.wasUserHangup = userHangup
        hangupWithStatusCode(pjsip_status_code.PJSIP_SC_DECLINE)
    }

    @Throws(Exception::class)
    fun putOnHold() {
        val callOpParam = CallOpParam(true)
        super.setHold(callOpParam)
        state.isOnHold = true
    }

    @Throws(Exception::class)
    fun takeOffHold() {
        val callOpParam = CallOpParam(true)
        callOpParam.opt.flag = pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue().toLong()
        super.reinvite(callOpParam)
        state.isOnHold = false
    }

    /**
     * Update the call microphone volume.
     *
     */
    private fun updateMicrophoneVolume(microphoneState: MicrophoneState) {
        try {
            for (i in 0 until this.info.media.size()) {
                if (this.info.media[i.toInt()].type == pjmedia_type.PJMEDIA_TYPE_AUDIO) {
                    AudioMedia.typecastFromMedia(getMedia(i))?.adjustRxLevel(microphoneState.volume.toFloat())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Toggle the microphone mute status of this call.
     *
     */
    fun toggleMute() {
        logger.d("Changing the mute status of this call from " + state.isMuted + " to " + !state.isMuted)
        updateMicrophoneVolume(if (state.isMuted) MicrophoneState.UNMUTED else MicrophoneState.MUTED)
        state.isMuted = !state.isMuted
    }

    /**
     * Attended transfer to a second existing call.
     * @param transferTo
     * @throws Exception
     */
    @Throws(Exception::class)
    fun xFerReplaces(transferTo: SipCall) = super.xferReplaces(transferTo, CallOpParam(true))

    /**
     * Function to perform a hangup with a certain status code to be able to distinguish between
     * a hangup and a decline.
     * @param statusCode
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun hangupWithStatusCode(statusCode: pjsip_status_code) {
        val callOpParam = CallOpParam(true)
        callOpParam.statusCode = statusCode
        super.hangup(callOpParam)
    }

    /**
     * We will translate this call state into our own internal state and then broadcast it to our listener.
     *
     * @param onCallStateParam parameters containing the state of an active call.
     */
    override fun onCallState(onCallStateParam: OnCallStateParam) {
        state.telephonyState = when(pjsipState) {
            pjsip_inv_state.PJSIP_INV_STATE_NULL -> TelephonyState.INITIALIZING
            pjsip_inv_state.PJSIP_INV_STATE_CALLING -> TelephonyState.OUTGOING_RINGING
            pjsip_inv_state.PJSIP_INV_STATE_INCOMING -> TelephonyState.INCOMING_RINGING
            pjsip_inv_state.PJSIP_INV_STATE_EARLY -> TelephonyState.CONNECTED
            pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> TelephonyState.CONNECTED
            pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> TelephonyState.CONNECTED
            pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> TelephonyState.DISCONNECTED
            else -> TelephonyState.INITIALIZING
        }

        sipService.onTelephonyStateChange(this, state.telephonyState)
    }

    /**
     * Translate the callback to the interface, which is implemented by the SipService
     *
     * @param onCallMediaStateParam parameters containing the state of the an active call' media.
     */
    override fun onCallMediaState(onCallMediaStateParam: OnCallMediaStateParam) {
        sipService.beginTransmittingAudio()
    }

    /**
     * Finds the usable audio channel for this call.
     *
     */
    fun getUsableAudio(): Media {
        for (i in 0 until info.media.size()) {
            val cmi = info.media[i.toInt()]
            val isUsable = cmi.status ==
                    pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                    cmi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD
            if (cmi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO && isUsable) {
                return getMedia(i)
            }
        }

        throw Exception("Unable to find usable audio")
    }

    fun beginIncomingRinging() {
        answerWithCode(pjsip_status_code.PJSIP_SC_RINGING)
    }

    /**
     * Answer the current call with a code, throwing a runtime exception if it fails.
     *
     * @param code
     * @return
     */
    private fun answerWithCode(code: pjsip_status_code) {
        val callOpParam = CallOpParam()
        callOpParam.statusCode = code
        try {
            this.answer(callOpParam)
            logger.i("Answered call with code: $code")
        } catch (e: Exception) {
            logger.e("Failed to answer call with code $code")
            throw RuntimeException(e)
        }
    }

    fun busy() = answerWithCode(pjsip_status_code.PJSIP_SC_BUSY_HERE)

    enum class Direction {
        OUTGOING, INCOMING
    }

    enum class MicrophoneState(val volume: Int) {
        MUTED(0), UNMUTED(2)
    }

    enum class TelephonyState {
        INITIALIZING, INCOMING_RINGING, OUTGOING_RINGING, CONNECTED, DISCONNECTED
    }

    enum class CallMissedReason(val packetLookupString: String) {
        CALL_ORIGINATOR_CANCEL("ORIGINATOR_CANCEL"), CALL_COMPLETED_ELSEWHERE("Call completed elsewhere");
    }
}