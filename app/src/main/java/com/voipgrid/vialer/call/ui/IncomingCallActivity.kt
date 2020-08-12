package com.voipgrid.vialer.call.ui

import android.os.Bundle
import android.telecom.CallAudioState
import android.widget.Toast
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.voip.SoftPhone
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class IncomingCallActivity : LoginRequiredActivity(), KoinComponent {

    private val softPhone: SoftPhone by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        initialiseButtonListeners()
    }

    private fun initialiseButtonListeners() {
        val connection = softPhone.call?.connection ?: return
        val call = softPhone.call ?: return

        button_decline.setOnClickListener {
            connection.onReject()
        }

        button_pickup.setOnClickListener {
            connection.onAnswer()
        }
    }
}