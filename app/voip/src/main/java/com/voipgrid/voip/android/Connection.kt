package com.voipgrid.voip.android

import android.annotation.SuppressLint
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.util.Log
import com.voipgrid.voip.SoftPhone
import nl.spindle.phonelib.PhoneLib
import nl.spindle.phonelib.model.Reason
import org.koin.core.KoinComponent
import org.koin.core.inject

class Connection: android.telecom.Connection(), KoinComponent {

    private val softPhone: SoftPhone by inject()

    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
        Log.e("TEST123", "Show clal ui..|")
    }

    @SuppressLint("MissingPermission")
    override fun onAnswer() {
        super.onAnswer()

        softPhone.call?.let {
            softPhone.actions.acceptIncoming(it.session)
        }
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
        Log.e("TEST123", "Audio state change... $state")
    }

    override fun onHold() {
        super.onHold()
        softPhone.call?.let {
            softPhone.actions.setHold(it.session, true)
        }
    }

    override fun onUnhold() {
        super.onUnhold()
        softPhone.call?.let {
            softPhone.actions.setHold(it.session, false)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onReject() {
        super.onReject()
        softPhone.call?.let {
            softPhone.actions.declineIncoming(it.session, Reason.DECLINED)
        }
    }

    override fun onDisconnect() {
        super.onDisconnect()
        softPhone.call?.let {
            softPhone.actions.end(it.session)
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        }
    }
}