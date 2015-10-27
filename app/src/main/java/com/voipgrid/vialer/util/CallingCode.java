package com.voipgrid.vialer.util;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.R;

public class CallingCode {
    static String[] prefixes;
    static String mobileCC;

    static public String getSystemCallingCode(Context context) {
        if (mobileCC == null) {
            String systemCC = null;

            final TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String countryIso = manager.getSimCountryIso().toUpperCase();
            final String[] countryCodes = context.getResources().getStringArray(R.array.country_codes);

            for (int i = 0; i < countryCodes.length; i++) {
                String[] cc = countryCodes[i].split(",");
                if (cc[1].trim().equals(countryIso.trim())) {
                    systemCC = cc[0];
                    break;
                }
            }

            mobileCC = systemCC != null ? "+" + systemCC : "";
        }
        return mobileCC;
    }

    static public String removeCallingCodeFromPhoneNumber(Context context, String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        if (prefixes == null) {
            // Calculate calling code prefixes
            // NOTE: Only works in some countries like The Netherlands
            String mobileCC = getSystemCallingCode(context);
            if (mobileCC.length() > 0) {
                prefixes = new String[] { mobileCC, mobileCC.replace("+", ""), "00", "0" };
            }
        }

        phoneNumber = phoneNumber.replaceAll("[^a-zA-Z0-9]", "");
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (phoneNumber.startsWith(prefix)) {
                    // Remove prefix
                    phoneNumber = phoneNumber.substring(prefix.length());
                    break;
                }
            }
        }

        return phoneNumber;
    }
}
