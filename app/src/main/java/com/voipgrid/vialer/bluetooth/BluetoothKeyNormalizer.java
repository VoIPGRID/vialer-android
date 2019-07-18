package com.voipgrid.vialer.bluetooth;

import androidx.annotation.IntDef;
import android.util.SparseArray;
import android.view.KeyEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public class BluetoothKeyNormalizer {

    @IntDef({KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_ENDCALL})
    @Retention(RetentionPolicy.SOURCE)
    @interface NormalizedCodes {}

    private static SparseArray<Integer[]> sAliases = new SparseArray<>();

    static {
        sAliases.put(KeyEvent.KEYCODE_CALL, new Integer[] {});
        sAliases.put(KeyEvent.KEYCODE_ENDCALL, new Integer[] {KeyEvent.KEYCODE_HEADSETHOOK});

    }

    private BluetoothKeyNormalizer(SparseArray aliases) {
        sAliases = aliases;
    }

    public static BluetoothKeyNormalizer defaultAliases() {
        return customAliases(sAliases);
    }

    public static BluetoothKeyNormalizer customAliases(SparseArray aliases) {
        return new BluetoothKeyNormalizer(aliases);
    }

    /**
     * Normalizes the various different keys from different bluetooth headsets to a set of standard
     * keys.
     *
     * @see NormalizedCodes
     * @param keyCode The key event that should be translated.
     * @return The translated key event or null if none is returned.
     */
    public @NormalizedCodes Integer normalize(int keyCode) {
        if(sAliases.get(keyCode) != null) return keyCode;

        for(int i = 0; i < sAliases.size(); i++) {
            Integer[] keyAliases = sAliases.valueAt(i);

            if(Arrays.asList(keyAliases).contains(keyCode)) return sAliases.keyAt(i);
        }

        return null;
    }
}
