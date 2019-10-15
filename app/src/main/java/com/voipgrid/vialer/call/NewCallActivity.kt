package com.voipgrid.vialer.call

import android.os.Bundle
import android.util.Log
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.voip.core.call.State

class NewCallActivity : LoginRequiredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_call)

//        render()
    }

    override fun voipServiceIsAvailable() {
        super.voipServiceIsAvailable()
    }

    override fun voipStateWasUpdated(state: State.TelephonyState) {
        super.voipStateWasUpdated(state)
        Log.e("TEST123", "Voip tate update")
    }

    override fun voipUpdate() {
        super.voipUpdate()
        Log.e("TEST123", "Voip update")
    }
}