package com.voipgrid.vialer.call.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.voipgrid.vialer.R

class TransferComplete : VoipAwareFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
            = inflater.inflate(R.layout.fragment_call_transfer_complete, container, false)

}