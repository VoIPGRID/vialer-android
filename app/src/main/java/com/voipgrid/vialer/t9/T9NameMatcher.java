package com.voipgrid.vialer.t9;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Class to match a T9Query to a name.
 */
public class T9NameMatcher {

    /**
     * Function to check if a t9 query matches a display name.
     * @param query T9 query to match.
     * @param displayName Display name to match.
     * @return Whether the t9 query matched the display name.
     */
    public static boolean T9QueryMatchesName(String query, String displayName) {
        ArrayList<String> possibleQueries = T9Query.generateT9NameQueries(displayName);

        Collections.sort(possibleQueries, new T9QueryComparator());

        for (int i = 0; i < possibleQueries.size(); i++) {
            if (possibleQueries.get(i).startsWith(query)){
                return true;
            }
        }
        return false;
    }

    /**
     * Function that surrounds the matched part in the name with <b></b>
     * @param t9Query The query that is matched on.
     * @param displayName The name.
     * @return Name with <b></b> tags.
     */
    public static String highlightMatchedPart(String t9Query, String displayName) {
        ArrayList<String> possibleQueries = T9Query.generateT9NameQueries(displayName);

        String queryOfWholeName = possibleQueries.get(0);

        int start = adjustStartBasedOnSpaces(displayName, queryOfWholeName.indexOf(t9Query));
        int end = start + t9Query.length();

        // Add a empty space behind the displayname to be able to substring untill the last char
        // without causing outofboundexceptions.
        return placeBoldingTags(displayName + " ", start, end, end-start);
    }

    /**
     * Calculates the number of spaces in the display name before the start value and increases
     * start based on this value.
     *
     * @param displayName
     * @param start
     * @return The value of start adjusted based on the number of spaces.
     */
    private static int adjustStartBasedOnSpaces(String displayName, int start) {
        String substr = displayName.substring(0, start);

        int spaceCount = substr.length() - substr.replace(" ", "").length();

        return start + spaceCount;
    }

    /**
     * Recursively builds a string with the correct highlighting.
     */
    private static String placeBoldingTags(String fullString, int start, int end, int charCount) {
        // Replace special chars that impact the regexes used for substrings.
        fullString = fullString.replaceAll("[*+?^<>|$\\\\]", " ");
        String highlightString = fullString.substring(start, end);

        int charsInHighlight = highlightString.replaceAll("[^A-Za-z]","").length();
        // Increase the endIndex if our highlightString is now to small.
        if (charsInHighlight < charCount) {
            return placeBoldingTags(fullString, start, end+1, charCount);
        }

        // Clean up the front of the string.
        if (!Character.isLetter(highlightString.charAt(0))) {
            return placeBoldingTags(fullString, start+1, end, charCount);
        }

        String preHighlight = fullString.split(highlightString)[0];
        String pastHighlight = fullString.split(highlightString)[1];

        String result = preHighlight + "<b>" + highlightString + "</b>" + pastHighlight;
        return result;
    }

    /**
     * Custom Comparator to sort the list based on string length DESC.
     */
    private static class T9QueryComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            return s2.length() - s1.length();
        }
    }

}
