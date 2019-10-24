package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R
import com.voipgrid.vialer.contacts.Contacts
import nl.voipgrid.vialer_voip.core.call.Call
import nl.voipgrid.vialer_voip.core.call.State
import kotlinx.android.synthetic.main.fragment_call_active_header.*
import org.koin.android.ext.android.inject

class ActiveCallHeader : VoipAwareFragment() {

    private val contacts: Contacts by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_active_header, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        render()
    }

    override fun voipUpdate() {
        render()
    }

    override fun render() {
        val voip = voip ?: return
        val call = voip.calls.active ?: return

        if (voip.isTransferring()) {
            val callerInformation = getAppropriateCallerTitles(voip.calls.original ?: return)
            original_call_container.visibility = View.VISIBLE
            if (callerInformation.subtitle.isNotBlank()) {
                original_call_information.text = getString(R.string.call_transfer_in_progress_details, "${callerInformation.title} (${callerInformation.subtitle})")
            } else {
                original_call_information.text = getString(R.string.call_transfer_in_progress_details, callerInformation.title)
            }
        } else {
            original_call_container.visibility = View.GONE
        }

        val callerInformation = getAppropriateCallerTitles(call)

        third_party_title.text = callerInformation.title

        if (callerInformation.subtitle.isNotBlank()) {
            third_party_subtitle.text = callerInformation.subtitle
            third_party_subtitle.visibility = View.VISIBLE
        } else {
            third_party_subtitle.visibility = View.GONE
        }

        renderCallDurationBox(call)
    }

    private fun getAppropriateCallerTitles(call: Call): CallerInformation {
        if (call.metaData.callerId.isNotBlank()) {
            return CallerInformation(call.metaData.callerId, call.metaData.number)
        }

        contacts.getContactNameByPhoneNumber(call.metaData.number)?.let {
            return CallerInformation(it, call.metaData.number)
        }

        return CallerInformation(call.metaData.number, "")
    }

    private fun renderCallDurationBox(call: Call) {
        val duration = DateUtils.formatElapsedTime(call.getDuration(Call.DurationUnit.SECONDS).toLong())
        val state = findStateString(call)

        if (state.isBlank()) {
            call_duration.text = duration
            return
        }

        if (call.state.telephonyState == State.TelephonyState.CONNECTED) {
            call_duration.text = activity?.getString(R.string.call_duration_with_state, state, duration)
        } else {
            call_duration.text = state
        }
    }

    private fun findStateString(call: Call) = when {
        call.state.isOnHold -> getString(R.string.call_state_on_hold)
        call.state.isMuted -> getString(R.string.call_state_microphone_muted)
        call.state.telephonyState == State.TelephonyState.OUTGOING_CALLING -> getString(R.string.call_state_calling)
        else -> ""
    }

    data class CallerInformation(val title: String, val subtitle: String)
}