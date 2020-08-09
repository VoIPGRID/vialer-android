package com.voipgrid.vialer.call.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.voipgrid.vialer.R
import com.voipgrid.vialer.util.LoginRequiredActivity

class CallActivity : LoginRequiredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
    }
}