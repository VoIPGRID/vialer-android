package com.voipgrid.vialer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.voipgrid.vialer.cryptography.Encrypter;
import com.voipgrid.vialer.cryptography.LegacyAsymmetricEncrypter;
import com.voipgrid.vialer.cryptography.LegacyAsymmetricToSymmetricPorter;
import com.voipgrid.vialer.logging.RemoteLogger;

/**
 * Class used in handling account information for a user. Encrypt/decrypt credentials that
 * are stored in the shared preferences.
 */
public class AccountHelper {
    public static final String EMAIL_KEY = "EMAIL_KEY";
    public static final String PASSWORD_KEY = "PASSWORD_KEY";
    public static final String API_TOKEN_KEY = "TOKEN_KEY";

    private RemoteLogger mRemoteLogger;
    private SharedPreferences mPrefs;
    private Encrypter mEncrypter;
    private LegacyAsymmetricToSymmetricPorter mEncryptionPorter;

    public AccountHelper(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mRemoteLogger = new RemoteLogger(AccountHelper.class).enableConsoleLogging();
        mEncrypter = new Encrypter(context);
        mEncryptionPorter = new LegacyAsymmetricToSymmetricPorter(
                mEncrypter,
                new LegacyAsymmetricEncrypter(mRemoteLogger, Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2),
                mPrefs,
                mRemoteLogger
        );
    }

    public void setCredentials(String email, String password) {
        setEmail(email);
        setPassword(password);
    }

    public void setCredentials(String email, String password, String token) {
        setEmail(email);
        setPassword(password);
        setApiToken(token);
    }

    /**
     * Set the email for the current user.
     *
     * @param email
     */
    private void setEmail(String email) {
        // No need to encrypt since it's also stored as info on the systemuser.
        mPrefs.edit().putString(EMAIL_KEY, email).apply();
    }

    /**
     * Set the password for the current user.
     *
     * @param password
     */
    private void setPassword(String password) {
        encryptToPrefs(PASSWORD_KEY, password);
    }

    /**
     * Set the api token for the current user.
     *
     * @param token
     */
    public void setApiToken(String token) {
        encryptToPrefs(API_TOKEN_KEY, token);
    }

    /**
     * Retrieve the current email.
     *
     * @return
     */
    public String getEmail() {
        return mPrefs.getString(EMAIL_KEY, null);
    }

    /**
     * Retrieve the current password.
     *
     * @return
     */
    public String getPassword() {
        return decryptFromPrefs(PASSWORD_KEY);
    }

    /**
     * Retrieve the current api token.
     *
     * @return
     */
    public String getApiToken() {
        return decryptFromPrefs(API_TOKEN_KEY);
    }

    /**
     * Encrypt a value and then insert it into the preferences with the given key.
     *
     * @param key The key to identify the value in the preferences.
     * @param value The string to encrypt and then add to preferences.
     */
    private void encryptToPrefs(String key, String value) {
        mPrefs.edit().putString(key, mEncrypter.encrypt(value)).apply();
    }

    /**
     * Take a value out of the preferences and decrypt it. This method will attempt to port
     * from legacy encryption to current encryption if it fails.
     *
     * @param key The preference to retrieve.
     * @return The decrypted preference.
     */
    private String decryptFromPrefs(String key) {
        String decrypted = decryptFromPrefsOrNull(key);

        if (decrypted != null) {
            return decrypted;
        }

        mEncryptionPorter.port();

        return decryptFromPrefsOrNull(key);
    }

    /**
     * Attempts to decrypt a key from the shared preferences, if this is not possible
     * for any reason, null is returned.
     *
     * @return
     */
    private String decryptFromPrefsOrNull(String key) {
        try {
            return mEncrypter.decrypt(mPrefs.getString(key, null));
        } catch (Exception e) {
            mRemoteLogger.e("Unable to decrypt " + key + " due to: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if there is an api token stored without performing unnecessary decryption. There are
     * no checks made to verify this is a valid token, only that one exists.
     *
     * @return TRUE if there is an api token, otherwise FALSE
     */
    public boolean hasApiToken() {
        return mPrefs.getString(API_TOKEN_KEY, null) != null;
    }

    /**
     * Delete all credentials, this will mean the user cannot authenticate.
     *
     */
    public void clearCredentials() {
        mPrefs.edit().remove(EMAIL_KEY).apply();
        mPrefs.edit().remove(PASSWORD_KEY).apply();
        mPrefs.edit().remove(API_TOKEN_KEY).apply();
    }
}
