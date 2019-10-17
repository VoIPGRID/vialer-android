package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.R
import com.voipgrid.vialer.call.NewCallActivity
import com.voipgrid.vialer.calling.Dialer
import kotlinx.android.synthetic.main.fragment_call_dialer.*

class Dialer : VoipAwareFragment(), Dialer.Listener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_dialer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialer.setListener(this)
    }

    override fun numberWasChanged(number: String?) {
    }

    override fun digitWasPressed(digit: String) {
        voip?.getCurrentCall()?.sendDtmf(digit)
    }

    override fun exitButtonWasPressed() {
        (activity as NewCallActivity).closeDialer()
    }
}