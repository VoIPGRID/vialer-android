package com.voipgrid.vialer.call.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.voipgrid.vialer.CallActivity
import com.voipgrid.vialer.call.NewAbstractCallActivity
import com.voipgrid.vialer.util.BroadcastReceiverManager
import com.voipgrid.vialer.util.LoginRequiredActivity
import com.voipgrid.vialer.voip.VoipService
import com.voipgrid.vialer.voip.core.call.State
import org.koin.android.ext.android.inject

abstract class VoipAwareFragment : Fragment() {

    private lateinit var receiver: BroadcastReceiver
    protected val voip
        get() = activity?.let {
            (activity as LoginRequiredActivity).voip
        }

    private val broadcastReceiverManager: BroadcastReceiverManager by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        retainInstance = true
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        receiver = broadcastReceiverManager.registerReceiverViaLocalBroadcastManager(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                when (intent.action) {
                    VoipService.ACTION_CALL_STATE_WAS_UPDATED -> voipStateWasUpdated(intent.getSerializableExtra(VoipService.CALL_STATE_EXTRA) as State.TelephonyState)
                    VoipService.ACTION_VOIP_UPDATE -> voipUpdate()
                }
            }

        }, VoipService.ACTION_CALL_STATE_WAS_UPDATED, VoipService.ACTION_VOIP_UPDATE)
    }

    override fun onPause() {
        super.onPause()
    }

    open fun voipStateWasUpdated(state: State.TelephonyState) {
    }

    open fun voipUpdate() {
    }

    open fun render() {

    }

}