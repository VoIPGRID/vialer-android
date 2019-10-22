package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R
import kotlinx.android.synthetic.main.activity_incoming_call.*

class IncomingCallButtons : VoipAwareFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_incoming_buttons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_decline.setOnClickListener {
            voip?.calls?.active?.decline()
        }

        button_pickup.setOnClickListener {
            voip?.calls?.active?.answer()
        }
    }
}