package com.voipgrid.vialer.test;

import com.voipgrid.vialer.t9.T9Query;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class T9QueryTest {

    /**
     * Test if the correct T9 queries are created based on amount of names.
     */
    @Test
    public void generateT9NameQueriesTest() {
        ArrayList<String> expectedResult = new ArrayList<>();
        ArrayList<String> generatedNameQueries;

        // Case 1: one name.
        generatedNameQueries = T9Query.generateT9NameQueries("Henk");

        expectedResult.add("4365");

        assertEquals(expectedResult, generatedNameQueries);

        // Case 2: two names.
        expectedResult.clear();
        generatedNameQueries = T9Query.generateT9NameQueries("Henk Bakker");

        expectedResult.add("4365225537");
        expectedResult.add("225537");

        assertEquals(generatedNameQueries, expectedResult);

        // Case 3: three names.
        expectedResult.clear();
        generatedNameQueries = T9Query.generateT9NameQueries("Henk de Boer");

        expectedResult.add("4365332637");
        expectedResult.add("332637");
        expectedResult.add("2637");

        assertEquals(generatedNameQueries, expectedResult);

        // Case 4: four names.
        expectedResult.clear();
        generatedNameQueries = T9Query.generateT9NameQueries("Henk van den Berg");

        expectedResult.add("43658263362374");
        expectedResult.add("8263362374");
        expectedResult.add("3362374");
        expectedResult.add("2374");

        assertEquals(generatedNameQueries, expectedResult);

    }

    /**
     * Test if special chars are converted correctly to normal counterparts.
     */
    @Test
    public void generateT9NameQueriesSpecialCharsTest() {
        ArrayList<String> expectedResult = new ArrayList<>();
        ArrayList<String> generatedNameQueries;

        // Case 1: one name.
        // `ê`
        generatedNameQueries = T9Query.generateT9NameQueries("H\u00EAnk");

        expectedResult.add("4365");

        assertEquals(expectedResult, generatedNameQueries);

        // Case 2: two names.
        expectedResult.clear();
        // `á`
        generatedNameQueries = T9Query.generateT9NameQueries("Henk B\u00E1kker");

        expectedResult.add("4365225537");
        expectedResult.add("225537");

        assertEquals(generatedNameQueries, expectedResult);

        // Case 3: three names.
        expectedResult.clear();
        // `õ`
        generatedNameQueries = T9Query.generateT9NameQueries("Henk de B\u00F5er");

        expectedResult.add("4365332637");
        expectedResult.add("332637");
        expectedResult.add("2637");

        assertEquals(generatedNameQueries, expectedResult);

        // Case 4: four names.
        expectedResult.clear();
        // `ë` `å` `è` `é`
        generatedNameQueries = T9Query.generateT9NameQueries("H\u00EBnk v\u00E5n d\u00E8n B\u00E9rg");

        expectedResult.add("43658263362374");
        expectedResult.add("8263362374");
        expectedResult.add("3362374");
        expectedResult.add("2374");

        assertEquals(generatedNameQueries, expectedResult);

    }
}