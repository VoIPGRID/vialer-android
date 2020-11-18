package com.voipgrid.vialer.calling

import android.app.KeyguardManager
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.voipgrid.vialer.R
import com.voipgrid.vialer.VialerApplication.Companion.get
import com.voipgrid.vialer.contacts.Contacts
import com.voipgrid.vialer.sip.CallDisconnectedReason
import com.voipgrid.vialer.sip.SipService
import javax.inject.Inject

class IncomingCallActivity : AbstractCallActivity() {

    @JvmField @Inject var mKeyguardManager: KeyguardManager? = null

    @JvmField @Inject var mContacts: Contacts? = null

    @JvmField @Inject var mCallActivityHelper: CallActivityHelper? = null

    @JvmField @BindView(R.id.incoming_caller_title) var mIncomingCallerTitle: TextView? = null

    @JvmField @BindView(R.id.incoming_caller_subtitle) var mIncomingCallerSubtitle: TextView? = null

    @JvmField @BindView(R.id.button_decline) var mButtonDecline: ImageButton? = null

    @JvmField @BindView(R.id.button_pickup) var mButtonPickup: ImageButton? = null

    @JvmField @BindView(R.id.call_buttons) var mCallButtons: View? = null

    @JvmField @BindView(R.id.animation) var animation: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        ButterKnife.bind(this)
        get().component().inject(this)
        mCallActivityHelper!!.updateLabelsBasedOnPhoneNumber(mIncomingCallerTitle, mIncomingCallerSubtitle, phoneNumberFromIntent, callerIdFromIntent)
        beginAnimation()
    }

    /**
     * Begin the incoming call animation and loop it.
     *
     */
    private fun beginAnimation() {
        val d = animation!!.drawable
        (d as AnimatedVectorDrawable).start()
        d.registerAnimationCallback(
                object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable) {
                        super.onAnimationEnd(drawable)
                        Handler().postDelayed({ d.start() }, 1000)
                    }
                })
    }

    @OnClick(R.id.button_decline)
    public override fun onDeclineButtonClicked() {
        logger.d("decline")
        disableAllButtons()
        if (!softPhone.hasCall) {
            return
        }
        SipService.performActionOnSipService(this, SipService.Actions.DECLINE_INCOMING_CALL)
        finish()
    }

    @OnClick(R.id.button_pickup)
    public override fun onPickupButtonClicked() {
        if (!softPhone.hasCall) {
            finish()
            return
        }
        disableAllButtons()
        SipService.performActionOnSipService(this, SipService.Actions.ANSWER_INCOMING_CALL)
    }

    private fun disableAllButtons() {
        mButtonPickup!!.isEnabled = false
        mButtonDecline!!.isEnabled = false
    }

    override fun onCallStatusChanged(status: String, callId: String) {}

    override fun onCallConnected() {
        finish()
    }

    override fun onCallDisconnected(reason: String?) {
        finish()
    }
}