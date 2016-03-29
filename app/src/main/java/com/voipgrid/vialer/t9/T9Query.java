package com.voipgrid.vialer.t9;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for generating a T9 query from a name.
 */
public class T9Query {

    /**
     * Generate T9 Queries for the given number.
     * @param number Number to create queries for.
     * @return List of all queries.
     */
    public static ArrayList<String> generateT9NumberQueries(String number) {
        ArrayList<String> numberQueries = new ArrayList<>();
        numberQueries.add(number);

        // Best effort to convert +XX12345678 numbers to 0612345678
        if (number.startsWith("+")) {
            numberQueries.add("0" + number.substring(3));
        }

        return numberQueries;
    }

    /**
     * Generate a T9 Query from a name like 'Henk' -> '4365'
     * @param displayName The name to convert to a T9 Query
     * @return The generated T9 Query.
     */
    public static ArrayList<String> generateT9NameQueries(String displayName) {
        // Get mapping for chars to digits.
        HashMap<Character, Character> t9Mapping = getT9Mapping();
        ArrayList<String> nameQueries = new ArrayList<>();

        // Normalize the name to remove special chars and replace them with their normal
        // counterparts Eg: ë -> e. And lowercase everything.
        String normalized_name = Normalizer.normalize(displayName, Normalizer.Form.NFD);
        normalized_name = normalized_name.replaceAll("\\p{M}", "");
        normalized_name = normalized_name.toLowerCase();

        // Split the names so we can generate a T9 Query for all parts of the name to make
        // search with LIKE `query` % possible instead of a LIKE % `query` %
        String[] split_name = normalized_name.split(" ");

        StringBuilder nameQuery = new StringBuilder();

        // For the name `Henk de Boer` the loop does the following:
        // Append: henkdeboer
        // Append: deboer
        // Append: boer
        // During appending the name is converted to a T9 Query.
        for (int i = 0; i < split_name.length; i++) {
            // Clear builder.
            nameQuery.setLength(0);
            for (int j = i; j < split_name.length; j++){
                nameQuery.append(split_name[j]);
            }
            // Convert the name to a T9 Query.
            nameQueries.add(convertNameToT9Query(nameQuery.toString(), t9Mapping));
        }

        return nameQueries;
    }

    /**
     * Function to convert a name to a T9 Query.
     * @param name Name to be converted.
     * @param t9Mapping Mapping for letter -> digit.
     * @return String with the name converted to digits.
     */
    private static String convertNameToT9Query(String name, HashMap<Character, Character> t9Mapping) {
        StringBuilder t9Query = new StringBuilder();

        // Loop all chars of the name.
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            // If digit no need to convert.
            if (Character.isDigit(c)) {
                t9Query.append(c);
            } else if (t9Mapping.keySet().contains(c)) {
                // Mapping contained the letter so get the corresponding digit.
                t9Query.append(t9Mapping.get(c));
            } else {
                // No digit and no letter related to mapping. Can't determine what to do so continue.
                continue;
            }
        }
        return t9Query.toString();
    }

    /**
     * Function to create a letter -> digit mapping.
     * @return The letter -> digit T9 mapping.
     */
    private static final HashMap<Character, Character> getT9Mapping() {
        HashMap<Character, Character> t9Mapping = new HashMap<>();

        t9Mapping.put('a', '2');
        t9Mapping.put('b', '2');
        t9Mapping.put('c', '2');

        t9Mapping.put('d', '3');
        t9Mapping.put('e', '3');
        t9Mapping.put('f', '3');

        t9Mapping.put('g', '4');
        t9Mapping.put('h', '4');
        t9Mapping.put('i', '4');

        t9Mapping.put('j', '5');
        t9Mapping.put('k', '5');
        t9Mapping.put('l', '5');

        t9Mapping.put('m', '6');
        t9Mapping.put('n', '6');
        t9Mapping.put('o', '6');

        t9Mapping.put('p', '7');
        t9Mapping.put('q', '7');
        t9Mapping.put('r', '7');
        t9Mapping.put('s', '7');

        t9Mapping.put('t', '8');
        t9Mapping.put('u', '8');
        t9Mapping.put('v', '8');

        t9Mapping.put('w', '9');
        t9Mapping.put('x', '9');
        t9Mapping.put('y', '9');
        t9Mapping.put('z', '9');

        return t9Mapping;
    }
}
