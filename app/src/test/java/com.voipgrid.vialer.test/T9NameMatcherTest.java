package com.voipgrid.vialer.test;

import com.voipgrid.vialer.t9.T9NameMatcher;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Class for testing the T9NameMatcher class.
 */
public class T9NameMatcherTest {

    @Test
    public void T9QueryMatchesNameTest() {
        String query;
        String name = "Henk van den Berg";

        // Assert true.
        // berg.
        query = "2374";
        assertTrue(T9NameMatcher.T9QueryMatchesName(query, name));

        // van den.
        query = "826336";
        assertTrue(T9NameMatcher.T9QueryMatchesName(query, name));

        // van.
        query = "826";
        assertTrue(T9NameMatcher.T9QueryMatchesName(query, name));

        // den.
        query = "336";
        assertTrue(T9NameMatcher.T9QueryMatchesName(query, name));

        // henk.
        query = "4365";
        assertTrue(T9NameMatcher.T9QueryMatchesName(query, name));

        // henkvandenberg.
        query = "43658263362374";
        assertTrue(T9NameMatcher.T9QueryMatchesName(query, name));

        // Assert false.
        // enkvandenberg.
        query = "3658263362374";
        assertFalse(T9NameMatcher.T9QueryMatchesName(query, name));
    }

}
