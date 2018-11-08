package com.voipgrid.vialer.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class HtmlHelperTest {

    private HtmlHelper mHtmlHelper = new HtmlHelper();

    @Test
    public void it_wraps_the_start_of_a_string_in_font_tags() {
        String result = mHtmlHelper.colorSubstring("This is a test string", "#FFFFFF", 0, 4);

        assertEquals("<font color=\"#FFFFFF\">This</font> is a test string", result);
    }

    @Test
    public void it_wraps_the_middle_of_a_string_in_font_tags() {
        String result = mHtmlHelper.colorSubstring("This is a test string", "#FFFFFF", 5, 9);

        assertEquals("This <font color=\"#FFFFFF\">is a</font> test string", result);
    }

    @Test
    public void it_wraps_the_end_of_a_string_in_font_tags() {
        String result = mHtmlHelper.colorSubstring("This is a test string", "#FFFFFF", 15, 21);

        assertEquals("This is a test <font color=\"#FFFFFF\">string</font>", result);
    }

    @Test
    public void it_formats_the_color_code_correctly_with_or_without_hash_prefix() {
        String result = mHtmlHelper.colorSubstring("This is a test string", "FFFFFF", 0, 4);
        assertEquals("<font color=\"#FFFFFF\">This</font> is a test string", result);

        result = mHtmlHelper.colorSubstring("This is a test string", "#FFFFFF", 0, 4);
        assertEquals("<font color=\"#FFFFFF\">This</font> is a test string", result);
    }
}