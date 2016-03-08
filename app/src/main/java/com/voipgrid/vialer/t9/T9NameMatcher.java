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

        String strippedDisplayName = displayName.replaceAll(" ", "");

        int start = queryOfWholeName.indexOf(t9Query);
        int end = start + t9Query.length();

        // Create a string with the highlighted part without whitespace.
        StringBuilder builder = new StringBuilder();
        builder.append(strippedDisplayName.substring(0, start));
        builder.append("<b>" + strippedDisplayName.substring(start, end) + "</b>");
        builder.append(strippedDisplayName.substring(end, strippedDisplayName.length()));

        String matchedWithoutSpaces = builder.toString();

        // Clear builder.
        builder.setLength(0);

        // Loop over indexes of existing spaces.
        int index = displayName.indexOf(" ");
        int previousIndex = 0;
        int count = 0;
        while (index >= 0) {
            // Start of the substring.
            int subStart = previousIndex;
            int subEnd;

            if (index > (start + count) && index < (end + count)) {
                // The whitespace is between the <b> tags so add 3 for the first <b>.
                subEnd = index + 3;
            } else if (index >= end){
                // The whitespace is after the <b> tags so add 7 for the <b> and </b>.
                subEnd = index + 7;
            } else {
                // The whitespace is before the <b> tags.
                subEnd = index;
            }

            // Subtract the amount of whitespace added.
            subEnd -= count;

            // Create a substring with trailing whitespace.
            builder.append(matchedWithoutSpaces.substring(subStart, subEnd) + " ");

            previousIndex = subEnd;
            count++;
            index = displayName.indexOf(" ", index + 1);
        }

        builder.append(matchedWithoutSpaces.substring(previousIndex, matchedWithoutSpaces.length()));

        return builder.toString();
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
