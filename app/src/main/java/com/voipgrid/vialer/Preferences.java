package com.voipgrid.vialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by eltjo on 15/09/15.
 */
public class Preferences {

    public static final String PREF_HAS_SIP_ENABLED = "PREF_HAS_SIP_ENABLED";
    public static final String PREF_HAS_SIP_PERMISSION = "PREF_HAS_SIP_PERMISSION";

    public static final boolean DEFAULT_VALUE_HAS_SIP_ENABLED = true;
    public static final boolean DEFAULT_VALUE_HAS_SIP_PERMISSION = false;

    private SharedPreferences mPreferences;

    public Preferences(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setSipPermission(boolean sipPermission) {
        mPreferences.edit().putBoolean(PREF_HAS_SIP_PERMISSION, sipPermission).apply();
    }

    public boolean hasSipPermission() {
        return  mPreferences.getBoolean(PREF_HAS_SIP_PERMISSION, DEFAULT_VALUE_HAS_SIP_PERMISSION);
    }

    public void setSipEnabled(boolean sipEnabled) {
        mPreferences.edit().putBoolean(PREF_HAS_SIP_ENABLED, sipEnabled).apply();
    }

    public boolean hasSipEnabled() {
        return  mPreferences.getBoolean(PREF_HAS_SIP_ENABLED, DEFAULT_VALUE_HAS_SIP_ENABLED);
    }

    public boolean canUseSip() {
        return hasSipPermission() && hasSipEnabled();
    }
}
