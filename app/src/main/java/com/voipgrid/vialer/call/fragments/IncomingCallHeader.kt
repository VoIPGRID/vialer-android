package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.voip.core.call.Call
import kotlinx.android.synthetic.main.fragment_call_active_header.*
import org.koin.android.ext.android.inject

class IncomingCallHeader : VoipAwareFragment() {

    private val contacts: Contacts by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_incoming_header, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        render()
    }

    override fun voipUpdate() {
        render()
    }

    private fun render() {
        val call = voip?.getCurrentCall() ?: return

        third_party_title.text = if (call.metaData.callerId.isNotBlank()) call.metaData.callerId else call.metaData.number
        third_party_subtitle.text = if (call.metaData.callerId.isNotBlank()) call.metaData.number else ""

        contacts.getContactNameByPhoneNumber(call.metaData.number)?.let {
            third_party_title.text = it
            third_party_subtitle.text = call.metaData.number
        }
    }
}