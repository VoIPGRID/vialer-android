package com.voipgrid.vialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.logging.LogUuidGenerator;
import com.voipgrid.vialer.util.ConnectivityHelper;
import com.voipgrid.vialer.util.JsonStorage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.StringDef;

/**
 * Class used for storing preferences related to SIP.
 */
public class Preferences {

    public static final String PREF_HAS_SIP_ENABLED = "PREF_HAS_SIP_ENABLED";
    public static final String PREF_HAS_SIP_PERMISSION = "PREF_HAS_SIP_PERMISSION";
    public static final String PREF_REMOTE_LOGGING = "PREF_REMOTE_LOGGING";
    public static final String PREF_REMOTE_LOGGING_ID = "PREF_REMOTE_LOGGING_ID";
    public static final String PREF_FINISHED_ONBOARDING = "PREF_FINISHED_ONBOARDING";
    public static final String PREF_HAS_3G_ENABLED = "PREF_HAS_3G_ENABLED";
    public static final String PREF_HAS_TLS_ENABLED = "PREF_HAS_TLS_ENABLED";
    public static final String PREF_HAS_STUN_ENABLED = "PREF_HAS_STUN_ENABLED";
    public static final String PREF_AUDIO_CODEC = "PREF_AUDIO_CODEC";

    public static final String CONNECTION_PREFERENCE = "CONNECTION_PREFERENCE";
    public static final long CONNECTION_PREFERENCE_NONE = -10;
    public static final long CONNECTION_PREFERENCE_WIFI = ConnectivityHelper.Connection.WIFI.toInt();
    public static final long CONNECTION_PREFERENCE_LTE = ConnectivityHelper.Connection.LTE.toInt();

    public static final int AUDIO_CODEC_ILBC = 1;
    public static final int AUDIO_CODEC_OPUS = 2;

    public static final boolean DEFAULT_VALUE_HAS_SIP_ENABLED = true;
    public static final boolean DEFAULT_VALUE_HAS_SIP_PERMISSION = false;
    public static final boolean DEFAULT_VALUE_HAS_3G_ENABLED = true;
    public static final boolean DEFAULT_VALUE_HAS_TLS_ENABLED = true;
    public static final boolean DEFAULT_VALUE_HAS_STUN_ENABLED = false;
    public static final int DEFAULT_VALUE_AUDIO_CODEC = AUDIO_CODEC_ILBC;

    @IntDef({AUDIO_CODEC_ILBC, AUDIO_CODEC_OPUS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioCodec {}

    private Context mContext;
    private SharedPreferences mPreferences;

    public Preferences(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public String getLoggerIdentifier() {
        String identifier = mPreferences.getString(PREF_REMOTE_LOGGING_ID, null);
        if (identifier == null) {
            identifier = LogUuidGenerator.generate();
            setLoggerIdentifier(identifier);
        }
        return identifier;
    }

    private void setLoggerIdentifier(String identifier) {
        mPreferences.edit().putString(PREF_REMOTE_LOGGING_ID, identifier).apply();
    }

    /**
     * Whether remote logging is active or not.
     * @return
     */
    public boolean remoteLoggingIsActive() {
        return  mPreferences.getBoolean(PREF_REMOTE_LOGGING, false);
    }

    /**
     * Set the state of the remote logging.
     * @param active
     */
    public void setRemoteLogging(boolean active) {
        mPreferences.edit().putBoolean(PREF_REMOTE_LOGGING, active).apply();
    }

    /**
     * Function to check if a user is logged in.
     * @return If a system user is present thus logged in.
     */
    public boolean isLoggedIn() {
        JsonStorage storage = new JsonStorage(mContext);
        return storage.has(SystemUser.class);
    }

    /**
     * Function to check if a user passed onboarding.
     * @return If a system user finished onboarding.
     */
    public boolean finishedOnboarding() {
        return mPreferences.getBoolean(PREF_FINISHED_ONBOARDING, false);
    }

    /**
     * Function to set if a user passed onboarding.
     */
    public void setFinishedOnboarding(boolean passed) {
        mPreferences.edit().putBoolean(PREF_FINISHED_ONBOARDING, passed).apply();
    }

    /**
     * Function to check if a phone account is present.
     * @return
     */
    public boolean hasPhoneAccount() {
        return new JsonStorage(mContext).has(PhoneAccount.class);
    }

    /**
     * Function to set the sip permission.
     * @param sipPermission
     */
    public void setSipPermission(boolean sipPermission) {
        mPreferences.edit().putBoolean(PREF_HAS_SIP_PERMISSION, sipPermission).apply();
    }

    /**
     * Function to check for the sip permission.
     * @return
     */
    public boolean hasSipPermission() {
         return  mPreferences.getBoolean(PREF_HAS_SIP_PERMISSION, DEFAULT_VALUE_HAS_SIP_PERMISSION);
    }

    /**
     * Function to set the enabled state of sip.
     * @param sipEnabled
     */
    public void setSipEnabled(boolean sipEnabled) {
        mPreferences.edit().putBoolean(PREF_HAS_SIP_ENABLED, sipEnabled).apply();
    }

    /**
     * Function to check if sip is enabled.
     * @return
     */
    public boolean hasSipEnabled() {
        return  mPreferences.getBoolean(PREF_HAS_SIP_ENABLED, DEFAULT_VALUE_HAS_SIP_ENABLED);
    }

    public boolean has3GEnabled() {
        return mPreferences.getBoolean(PREF_HAS_3G_ENABLED, DEFAULT_VALUE_HAS_3G_ENABLED);
    }

    public void set3GEnabled(boolean use3G) {
        mPreferences.edit().putBoolean(PREF_HAS_3G_ENABLED, use3G).apply();
    }

    /**
     * Function that checks if all requirements are met for using sip. (Sip enabled, permission
     * and phone account).
     * @return
     */
    public boolean canUseSip() {
        return hasSipPermission() && hasSipEnabled() && hasPhoneAccount();
    }

    public void setConnectionPreference(long preference) {
        mPreferences.edit().putLong(CONNECTION_PREFERENCE, preference).apply();
    }
    public boolean hasConnectionPreference(long preference) {
        return preference == getConnectionPreference();
    }

    public long getConnectionPreference() {
        return mPreferences.getLong(CONNECTION_PREFERENCE, CONNECTION_PREFERENCE_WIFI);
    }

    public boolean hasTlsEnabled() {
        return mPreferences.getBoolean(PREF_HAS_TLS_ENABLED, DEFAULT_VALUE_HAS_TLS_ENABLED);
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        mPreferences.edit().putBoolean(PREF_HAS_TLS_ENABLED, tlsEnabled).apply();
    }

    public boolean hasStunEnabled() {
        return mPreferences.getBoolean(PREF_HAS_STUN_ENABLED, DEFAULT_VALUE_HAS_STUN_ENABLED);
    }

    public void setStunEnabled(boolean stunEnabled) {
        mPreferences.edit().putBoolean(PREF_HAS_STUN_ENABLED, stunEnabled).apply();
    }

    public void setAudioCodec(@AudioCodec int audioCodec) {
        mPreferences.edit().putInt(PREF_AUDIO_CODEC, audioCodec).apply();
    }

    public @AudioCodec int getAudioCodec() {
        return mPreferences.getInt(PREF_AUDIO_CODEC, DEFAULT_VALUE_AUDIO_CODEC);
    }
}
