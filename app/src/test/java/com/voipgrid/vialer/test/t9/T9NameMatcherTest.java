package com.voipgrid.vialer.test.t9;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.voipgrid.vialer.t9.T9NameMatcher;

import org.junit.Test;

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

    @Test
    public void highlightMatchedPartTest() {
        t9matcherTest("436", "<b>Hen</b>k van den Berg ");

        // henk.
        t9matcherTest("4365", "<b>Henk</b> van den Berg ");

        // van.
        t9matcherTest("826", "Henk <b>van</b> den Berg ");

        // vande.
        t9matcherTest("82633", "Henk <b>van de</b>n Berg ");

        // den.
        t9matcherTest("336", "Henk van <b>den</b> Berg ");

        // denb.
        t9matcherTest("3362", "Henk van <b>den B</b>erg ");

        // berg.
        t9matcherTest("2374", "Henk van den <b>Berg</b> ");
    }

    private void t9matcherTest(String query, String expected) {
        String name = "Henk van den Berg";
        assertEquals(expected, T9NameMatcher.highlightMatchedPart(query, name));
    }
}
