package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.sip.SipCall;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls that handles incoming GSM calls during a sip call.
 */
public class GsmCallListener extends BroadcastReceiver {

    private List<SipCall> mSipCalls;
    private List<SipCall> mAlreadyHoldedCalls = new ArrayList<>();

    private String mLastState = "";

    public GsmCallListener(List<SipCall> sipCalls) {
        mSipCalls = sipCalls;
    }

    public void updateSipCallsList(List<SipCall> newSipCalls) {
        mSipCalls = newSipCalls;
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

        // Put all calls on hold that are not already on hold.
        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            for (int i = 0; i < mSipCalls.size(); i++) {
                SipCall call = mSipCalls.get(i);
                try {
                    if (call.isOnHold()) {
                        mAlreadyHoldedCalls.add(call);
                    } else {
                        call.toggleHold();
                    }
                } catch (Exception e) {

                }
            }
        }

        // Remove hold from calls that were not on hold in the first place.
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            for (int i = 0; i < mSipCalls.size(); i++) {
                SipCall call = mSipCalls.get(i);
                try {
                    if (!mAlreadyHoldedCalls.contains(call)) {
                        call.toggleHold();
                    }
                } catch (Exception e) {

                }
            }
        }
        mLastState = state;
    }
}
