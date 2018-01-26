package com.voipgrid.vialer.call;

import android.telephony.TelephonyManager;

public class NativeCallManager {

    private TelephonyManager mTelephonyManager;

    public NativeCallManager(TelephonyManager telephonyManager) {
        mTelephonyManager = telephonyManager;
    }

    /**
     * Checks whether the phone is currently busy handling a native call at all, either in off-hook or ringing state.
     *
     * @return boolean true if a native call is either in progress or ringing
     */
    public boolean isBusyWithNativeCall() {
        return nativeCallIsInProgress() || nativeCallIsRinging();
    }

    /**
     * Check if there is a native call answered.
     *
     * @return boolean true if there is a native call answered.
     */
    public boolean nativeCallIsInProgress() {
        return mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK;
    }

    /**
     * See if there is a native GSM call Ringing
     *
     * @return boolean true when there is a native GSM call ringing
     */
    public boolean nativeCallIsRinging() {
        return mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING;
    }
}
