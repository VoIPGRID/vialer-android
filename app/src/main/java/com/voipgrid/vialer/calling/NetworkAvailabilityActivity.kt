package com.voipgrid.vialer.calling

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.logging.Logger
import com.voipgrid.vialer.sip.CallDisconnectedReason
import com.voipgrid.vialer.sip.SipService
import com.voipgrid.vialer.util.NetworkUtil
import kotlinx.android.synthetic.main.activity_call.*
import org.openvoipalliance.phonelib.PhoneLib
import javax.inject.Inject

class NetworkAvailabilityActivity : AbstractCallActivity() {
    @JvmField @Inject var mCallActivityHelper: CallActivityHelper? = null

    @JvmField @Inject var mNetworkUtil: NetworkUtil? = null
    var mLogger = Logger(this)

    private val mCheckServiceHandler = Handler()
    private val mCheckServiceRunnable: Runnable = object : Runnable {
        override fun run() {
            // Check if the Network is restored every 5 seconds.
            checkNetworkRestored()
            mCheckServiceHandler.postDelayed(this, CHECK_USER_IS_CONNECTED_TO_NETWORK.toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_availability)
        ButterKnife.bind(this)
        get().component().inject(this)
        mCheckServiceHandler.postDelayed(mCheckServiceRunnable, CHECK_USER_IS_CONNECTED_TO_NETWORK.toLong())
    }

    protected fun checkNetworkRestored() {
        if (mNetworkUtil!!.isOnline) {
            finish()
        }
    }

    override fun onPickupButtonClicked() {}
    @OnClick(R.id.button_hangup)
    public override fun onDeclineButtonClicked() {
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall) {
            finish()
            return
        }
        try {
            PhoneLib.getInstance(this).end(sipServiceConnection!!.get().currentCall)
            mLogger.i("The user hang up from Network Availability Activity")
        } catch (e: Exception) {
            mLogger.i("Failed to decline call " + e.javaClass.simpleName + e.message)
            finish()
        }
    }

    override fun sipServiceHasConnected(sipService: SipService) {
        super.sipServiceHasConnected(sipService)
        if (!sipServiceConnection!!.isAvailableAndHasActiveCall) {
            finish()
            return
        }
        val sipCall = sipServiceConnection!!.get().currentCall
        mCallActivityHelper!!.updateLabelsBasedOnPhoneNumber(incoming_caller_title, incoming_caller_subtitle, sipCall.phoneNumber, sipCall.displayName)
    }

    override fun onCallStatusChanged(status: String, callId: String) {}
    override fun onCallConnected() {}
    override fun onCallDisconnected(reason: CallDisconnectedReason) {}

    companion object {
        private const val CHECK_USER_IS_CONNECTED_TO_NETWORK = 500
        @JvmStatic
        fun start() {
            val intent = Intent(get(), NetworkAvailabilityActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val logger = Logger(NetworkAvailabilityActivity::class.java)
            get().startActivity(intent)
            logger.d("No connectivity available, the Network Availability Activity is being shown")
        }
    }
}