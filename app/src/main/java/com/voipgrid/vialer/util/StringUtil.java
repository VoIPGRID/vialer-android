package com.voipgrid.vialer.util;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    /**
     * Perform a regex match to extract the first matching capturing group from a string.
     *
     * @param subject The string to search in
     * @param pattern The pattern to search for, this must contain a single capturing group
     * @return The captured group or null if not found
     */
    public static @Nullable String extractFirstCaptureGroupFromString(String subject, String pattern) {
        return extractCaptureGroups(subject, pattern).get(0);
    }

    public static ArrayList<String> extractCaptureGroups(String subject, String pattern) {

        if (subject == null) return null;

        Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
        Matcher m = p.matcher(subject);

        if (!m.find()) {
            return new ArrayList<>();
        }

        ArrayList<String> matches = new ArrayList<>();

        int i = 1;

        while(true) {
            try {
                String match = m.group(i);

                if (match == null) {
                    return matches;
                }

                matches.add(match);

                i++;
            } catch (Exception e) {
                return matches;
            }
        }
    }
}
