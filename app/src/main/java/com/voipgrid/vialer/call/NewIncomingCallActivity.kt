package com.voipgrid.vialer.call

import android.os.Bundle
import android.util.Log
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.voip.core.call.State
import kotlinx.android.synthetic.main.activity_incoming_call.*

class NewIncomingCallActivity : NewAbstractCallActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_incoming_call)

        button_decline.setOnClickListener {
            voip?.getCurrentCall()?.decline()
            finish()
        }

        button_pickup.setOnClickListener {
            Log.e("TEST123", "Answering..")
            voip?.getCurrentCall()?.answer()
            finish()
        }

        render()
    }

    private fun render() {
       renderCallerInformation()
    }

    override fun voipServiceIsAvailable() {
        render()
    }

    override fun voipStateWasUpdated(state: State.TelephonyState) {
        Log.e("TEST123", "Activity got state! $state")
        when(state) {
            State.TelephonyState.RINGING -> render()
            else -> finish()
        }
    }
}