package com.voipgrid.vialer.util;

import androidx.core.content.ContextCompat;

import com.voipgrid.vialer.VialerApplication;

public class ColorHelper {

    /**
     * Returns a color resource as a hex code.
     *
     * @param id The id of the color resource to find.
     * @return The hex code, as a string with a prefixed hash.
     */
    public String getColorResourceAsHexCode(int id) {
        return  "#" + Integer.toHexString(ContextCompat.getColor(VialerApplication.get(), id) & 0x00ffffff).toUpperCase();
    }
}
