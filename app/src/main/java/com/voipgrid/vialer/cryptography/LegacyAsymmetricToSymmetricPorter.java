package com.voipgrid.vialer.cryptography;

import static com.voipgrid.vialer.util.AccountHelper.API_TOKEN_KEY;
import static com.voipgrid.vialer.util.AccountHelper.PASSWORD_KEY;

import android.content.SharedPreferences;

import com.voipgrid.vialer.logging.RemoteLogger;

/**
 * This class exists to port values in shared preferences that that have been encrypted
 * with the legacy encryption method to use the new encryption method.
 */
public class LegacyAsymmetricToSymmetricPorter {

    /**
     * The keys in shared preferences that will be ported to the new encryption format.
     */
    private static final String[] valuesToPort = { PASSWORD_KEY, API_TOKEN_KEY };

    private final Encrypter mEncrypter;
    private final LegacyAsymmetricEncrypter mLegacyAsymmetricEncryptor;
    private final SharedPreferences mSharedPreferences;
    private final RemoteLogger mRemoteLogger;

    public LegacyAsymmetricToSymmetricPorter(Encrypter encrypter, LegacyAsymmetricEncrypter legacyAsymmetricEncryptor, SharedPreferences sharedPreferences, RemoteLogger remoteLogger) {
        mEncrypter = encrypter;
        mLegacyAsymmetricEncryptor = legacyAsymmetricEncryptor;
        mSharedPreferences = sharedPreferences;
        mRemoteLogger = remoteLogger;
    }

    /**
     * Exports data stored in the old preferences to the new preferences.
     */
    public void port() {
        mRemoteLogger.i("Beginning porting of encrypted keys from legacy method to current method");

        for (String key : valuesToPort) {
            String unencrypted = mLegacyAsymmetricEncryptor.decrypt(mSharedPreferences.getString(key, null));

            if (unencrypted == null) {
                continue;
            }

            mRemoteLogger.i("Porting " + key);

            mSharedPreferences.edit().putString(key, mEncrypter.encrypt(unencrypted)).apply();
        }

        mRemoteLogger.i("Finished porting, removing legacy keystore alias.");
    }
}
