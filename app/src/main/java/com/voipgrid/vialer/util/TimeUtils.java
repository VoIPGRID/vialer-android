package com.voipgrid.vialer.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

public class TimeUtils {

    /**
     * Convert milliseconds to system time, assuming the time has been provided in europe time (UTC+1).
     *
     * @param datetime The timestamp to convert
     */
    public static long convertToSystemTime(long datetime) {
        return convertToSystemTime(datetime, "Europe/Amsterdam");
    }

    /**
     * Convert the provided milliseconds to the system timezone.
     *
     * @param datetime The timestamp to convert
     * @param initialTimezone The timezone that the timestamp has been provided in
     */
    public static long convertToSystemTime(long datetime, String initialTimezone) {
        return new DateTime(datetime, DateTimeZone.forID(initialTimezone))
                .withZone(DateTimeZone.forID(TimeZone.getDefault().getID()))
                .getMillis();
    }
}
