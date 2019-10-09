package com.voipgrid.vialer.call

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.voip.VoipService
import kotlinx.android.synthetic.main.activity_incoming_call.*
import org.koin.android.ext.android.inject

class NewIncomingCallActivity : LoginRequiredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_incoming_call)

        button_decline.setOnClickListener {
            try {
            voip?.getCurrentCall()?.decline()

            } catch (e: Throwable) {
                Log.e("TEST123", "e", e)
            }
        }

        button_pickup.setOnClickListener {
            Log.e("TEST123", "Answering..")
            voip?.getCurrentCall()?.answer()
        }

        render()
    }

    private fun render() {
        incoming_caller_title.text = voip?.getCurrentCall()?.metaData?.callerId
        incoming_caller_subtitle.text = voip?.getCurrentCall()?.metaData?.number
    }

    override fun voipServiceIsAvailable() {
        render()
    }

    override fun voipStateWasUpdated() {
        render()
    }
}