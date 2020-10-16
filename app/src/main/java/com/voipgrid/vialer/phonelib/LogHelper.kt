package com.voipgrid.vialer.phonelib

import com.google.gson.GsonBuilder
import com.voipgrid.vialer.logging.Logger
import org.linphone.core.StreamType
import org.openvoipalliance.phonelib.model.Session

object LogHelper {

    private val logger = Logger(this)
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

    fun logCall(call: Session) {
        val linphoneCall = call.linphoneCall
        val data = mapOf(
                "to-address" to mapOf(
                        "transport" to linphoneCall.toAddress.transport.name,
                        "domain" to linphoneCall.toAddress.domain
                ),
                "remote-params" to mapOf(
                        "encryption" to linphoneCall.remoteParams.mediaEncryption.name,
                        "sessionName" to linphoneCall.remoteParams.sessionName
                ),
                "params" to mapOf(
                        "encryption" to linphoneCall.params.mediaEncryption.name,
                        "sessionName" to linphoneCall.params.sessionName
                ),
                "call" to mapOf(
                        "callId" to linphoneCall.callLog.callId,
                        "ref-key" to linphoneCall.callLog.refKey,
                        "status" to linphoneCall.callLog.status,
                        "direction" to linphoneCall.callLog.dir.name,
                        "quality" to linphoneCall.callLog.quality,
                        "startDate" to linphoneCall.callLog.startDate,
                        "reason" to linphoneCall.reason.name,
                        "duration" to linphoneCall.duration
                ),
                "error" to mapOf(
                        "phrase" to linphoneCall.errorInfo.phrase,
                        "protocol" to linphoneCall.errorInfo.protocol,
                        "reason" to linphoneCall.errorInfo.reason,
                        "protocolCode" to linphoneCall.errorInfo.protocolCode
                ),
                "audio" to mapOf(
                        "downloadBandwidth" to linphoneCall.getStats(StreamType.Audio).downloadBandwidth,
                        "estimatedDownloadBandwidth" to linphoneCall.getStats(StreamType.Audio).estimatedDownloadBandwidth,
                        "jitterBufferSizeMs" to linphoneCall.getStats(StreamType.Audio).jitterBufferSizeMs,
                        "localLateRate" to linphoneCall.getStats(StreamType.Audio).localLateRate,
                        "localLossRate" to linphoneCall.getStats(StreamType.Audio).localLossRate,
                        "receiverInterarrivalJitter" to linphoneCall.getStats(StreamType.Audio).receiverInterarrivalJitter,
                        "receiverLossRate" to linphoneCall.getStats(StreamType.Audio).receiverLossRate,
                        "roundTripDelay" to linphoneCall.getStats(StreamType.Audio).roundTripDelay,
                        "rtcpDownloadBandwidth" to linphoneCall.getStats(StreamType.Audio).rtcpDownloadBandwidth,
                        "rtcpUploadBandwidth" to linphoneCall.getStats(StreamType.Audio).rtcpUploadBandwidth,
                        "senderInterarrivalJitter" to linphoneCall.getStats(StreamType.Audio).senderInterarrivalJitter,
                        "senderLossRate" to linphoneCall.getStats(StreamType.Audio).senderLossRate,
                        "iceState" to linphoneCall.getStats(StreamType.Audio).iceState.name,
                        "uploadBandwidth" to linphoneCall.getStats(StreamType.Audio).uploadBandwidth
                )
        )

        logger.i(gson.toJson(data))
    }
}