package com.voipgrid.vialer.util;

import android.util.Log;

public class HtmlHelper {

    /**
     * Enclose a portion of a string within font tags with a color attribute.
     *
     * @param str The string to add the color to
     * @param color The hex code of the color to be applied
     * @param start The start of the substring that the color will be applied to
     * @param end The end of the substring that the color will be applied to
     * @return
     */
    public String colorSubstring(String str, String color, int start, int end) {
        color = formatColorHexCode(color);
Log.e("TEST123", "Strin:" + str + " start:"+ start + " end" + end);
        return str.substring(0, start) + wrapInColorTags(str.substring(start, end), color) + str.substring(end);
    }

    /**
     * Prefixes the hex code with a hash if it isn't already prefixed.
     *
     * @param color
     * @return
     */
    private String formatColorHexCode(String color) {
        if (color.startsWith("#")) return color;

        return "#" + color;
    }

    private String wrapInColorTags(String content, String color) {
        return "<font color=\"" + color + "\">" + content + "</font>";
    }
}
