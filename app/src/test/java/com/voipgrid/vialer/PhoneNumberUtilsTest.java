package com.voipgrid.vialer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.voipgrid.vialer.util.PhoneNumberUtils;

import org.junit.Test;

/**
 * PhoneNumberUtilsTest class for testing PhoneNumberUtils functions.
 */
public class PhoneNumberUtilsTest {

    /**
     * Test the format function.
     */
    @Test
    public void formatTest() {
        String phoneNumber;

        // Test normal number.
        phoneNumber = "0508009000";
        assertEquals("0508009000", PhoneNumberUtils.format(phoneNumber));

        // Test country code number.
        phoneNumber = "+31508009000";
        assertEquals("+31508009000", PhoneNumberUtils.format(phoneNumber));

        // Test whitespaces.
        phoneNumber = "+31 5080 0900 0";
        assertEquals("+31508009000", PhoneNumberUtils.format(phoneNumber));

        // Test letters.
        phoneNumber = "+315A08009c000B";
        assertEquals("+31508009000", PhoneNumberUtils.format(phoneNumber));

        // Test combination.
        phoneNumber = "+31508 0090a00  G";
        assertEquals("+31508009000", PhoneNumberUtils.format(phoneNumber));

        // Test * for anonymouse callings.
        phoneNumber = "*31*0508009000";
        assertEquals("*31*0508009000", PhoneNumberUtils.format(phoneNumber));
    }

    /**
     * Test the formatMobileNumber function.
     */
    @Test
    public void formatMobileNumberTest() {
        String mobileNumber;

        // Test normal number.
        mobileNumber = "0508009000";
        assertEquals("0508009000", PhoneNumberUtils.formatMobileNumber(mobileNumber));

        // Test 00 number.
        mobileNumber = "0031508009000";
        assertEquals("+31508009000", PhoneNumberUtils.formatMobileNumber(mobileNumber));
    }

    /**
     * Test the isValidMobileNumber function.
     */
    @Test
    public void isValidMobileNumberTest() {
        String mobileNumber;

        // Test normal number.
        mobileNumber = "0508009000";
        assertFalse(PhoneNumberUtils.isValidMobileNumber(mobileNumber));

        // Test country prefixed number.
        mobileNumber = "+31508009000";
        assertTrue(PhoneNumberUtils.isValidMobileNumber(mobileNumber));
    }

}
