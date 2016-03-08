package com.voipgrid.vialer.test.t9;

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

    @Test
    public void highlightMatchedPartTest() {
        String name = "Henk van den Berg";
        String query;
        String expectedResult;

        // hen.
        query = "436";
        expectedResult = "<b>Hen</b>k van den Berg";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));

        // henk.
        query = "4365";
        expectedResult = "<b>Henk</b> van den Berg";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));

        // van.
        query = "826";
        expectedResult = "Henk <b>van</b> den Berg";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));

        // vande.
        query = "82633";
        expectedResult = "Henk <b>van de</b>n Berg";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));

        // den.
        query = "336";
        expectedResult = "Henk van <b>den</b> Berg";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));

        // denb.
        query = "3362";
        expectedResult = "Henk van <b>den B</b>erg";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));

        // berg.
        query = "2374";
        expectedResult = "Henk van den <b>Berg</b>";
        assertTrue(expectedResult.equals(T9NameMatcher.highlightMatchedPart(query, name)));
    }

}
