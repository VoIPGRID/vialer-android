package com.voipgrid.vialer.util

import android.telephony.TelephonyManager

class Sim(private val telephonyManager: TelephonyManager) {

    /**
     * The mobile number of the currently installed sim card, if it cannot be found, null.
     *
     */
    val mobileNumber: String?
        get() = try {
            if (telephonyManager.line1Number != null && telephonyManager.line1Number.isNotEmpty()
                    && PhoneNumberUtils.isValidMobileNumber(telephonyManager.line1Number)) {
                telephonyManager.line1Number
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }
}