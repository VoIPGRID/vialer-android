package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.sip.CallInteraction;

import org.pjsip.pjsua2.Call;

/**
 * Calls that handles incoming GSM calls during a sip call.
 */
public class GsmCallListener extends BroadcastReceiver {

    private CallInteraction mCallInteraction;
    private Call mCurrentCall;

    private String mLastState = "";
    private boolean mIsOnHold = false;

    public GsmCallListener(Call call, CallInteraction callInteraction) {
        mCallInteraction = callInteraction;
        mCurrentCall = call;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // If, the received action is not a type of "Phone_State", ignore it.
        if (!intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            return;
        }

        String state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE, "");

        // State changes are often received twice.
        if (state.equals(mLastState)) {
            return;
        }

        // On GSM ringing put sip call on hold.
        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            mCallInteraction.putOnHold(mCurrentCall);
            mIsOnHold = true;
        }

        // When GSM goes idle check if we had to put the SIP call on hold en release the hold.
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            if (mIsOnHold) {
                mCallInteraction.putOnHold(mCurrentCall);
                mIsOnHold = false;
            }
        }

        mLastState = state;
    }
}
