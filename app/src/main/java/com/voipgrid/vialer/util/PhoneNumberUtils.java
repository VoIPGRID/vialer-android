package com.voipgrid.vialer.util;

/**
 * Utils class for PhoneNumber
 */
public class PhoneNumberUtils {

    /**
     * Format phonenumber to allowed format for i.e. SIP uri's
     * @param phoneNumber number to format
     * @return String formatted phoneNumber
     */
    public static String format(String phoneNumber) {
        return phoneNumber.replaceAll("[^+0-9]","");
    }
}
