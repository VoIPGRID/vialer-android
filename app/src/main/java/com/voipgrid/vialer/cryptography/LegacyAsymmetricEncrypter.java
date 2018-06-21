package com.voipgrid.vialer.cryptography;

import android.util.Base64;
import android.util.Log;

import com.voipgrid.vialer.logging.RemoteLogger;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * This is the remaining code required to export legacy encrypted values
 * stored in shared preferences.
 * @deprecated
 */
public class LegacyAsymmetricEncrypter {

    public static final String KEY_ALIAS = "PASS_ALIAS";
    public static final String KEYSTORE_TYPE = "AndroidKeyStore";
    private static final String ENCRYPTION_METHOD = "RSA/ECB/PKCS1Padding";

    private final RemoteLogger mRemoteLogger;
    private static KeyStore sKeyStore;

    public LegacyAsymmetricEncrypter(RemoteLogger remoteLogger, boolean useAndroidKeystore) {
        mRemoteLogger = remoteLogger;
        if (useAndroidKeystore) {
            createKeyStore();
        }
    }

    /**
     * Decrypt a base64 encoded encrypted value to the original string.
     *
     * @param value The base64 encoded encrypted value
     * @return The unencrypted value as a string
     * @deprecated
     */
    public String decrypt(String value) {
        if (value == null || !hasKeyStore()) {
            return value;
        }

        try {
            Cipher cipher = createCipher();
            cipher.init(Cipher.DECRYPT_MODE, createEntry().getPrivateKey());
            byte[] encodedBytes = cipher.doFinal(Base64.decode(value, Base64.DEFAULT));
            return new String(encodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            mRemoteLogger.e("Decryption failed!");
            mRemoteLogger.e(Log.getStackTraceString(e));
            return value;
        }
    }

    private KeyStore.PrivateKeyEntry createEntry()
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        return (KeyStore.PrivateKeyEntry) sKeyStore.getEntry(KEY_ALIAS, null);
    }

    private Cipher createCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(ENCRYPTION_METHOD);
    }

    /**
     * Create the AndroidKeyStore.
     *
     */
    private void createKeyStore() {
        if (hasKeyStore()) {
            return;
        }

        try {
            sKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            sKeyStore.load(null);
        } catch (Exception e) {
            mRemoteLogger.e("Unable to create key store: " + e.getMessage());
        }
    }

    /**
     * Check if there is currently a keystore available.
     *
     * @return
     */
    private boolean hasKeyStore() {
        return sKeyStore != null;
    }
}
