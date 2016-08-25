package com.voipgrid.vialer.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;
import android.util.Log;

import com.voipgrid.vialer.logging.RemoteLogger;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Calendar;

import javax.crypto.Cipher;

import javax.security.auth.x500.X500Principal;

/**
 * Class used in handling account information for a user. Encrypt/decrypt credentials that
 * are stored in the shared preferences.
 */
public class AccountHelper {
    private static final String EMAIL_KEY = "EMAIL_KEY";
    private static final String PASSWORD_KEY = "PASSWORD_KEY";
    private static final String KEY_ALIAS = "PASS_ALIAS";

    private Context mContext;
    private KeyStore mKeyStore;
    private RemoteLogger mRemoteLogger;
    private SharedPreferences mPrefs;

    public AccountHelper(Context context) {
        mContext = context;
        init();
    }

    /**
     * Init the class by getting the KeyStore that contains encryption keys or make
     * new keys when they do not exists yet.
     */
    private void init() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mRemoteLogger = new RemoteLogger(mContext);

        // Api level 18 only supports the easiest way to use the KeyStore without using
        // several workarounds.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                mKeyStore = KeyStore.getInstance("AndroidKeyStore");
                mKeyStore.load(null);

                if (!mKeyStore.containsAlias(KEY_ALIAS)) {
                    generateKeyPair();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function to generate a KeyPair used for encrypting/decrypting
     * @throws Exception
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void generateKeyPair() throws Exception {
        // Make sure this key is valid for a lifetime.
        Calendar notBefore = Calendar.getInstance();
        Calendar notAfter = Calendar.getInstance();
        notAfter.add(Calendar.YEAR, 99);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                .setAlias(KEY_ALIAS)
                .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(notBefore.getTime())
                .setEndDate(notAfter.getTime())
                .build();

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        generator.initialize(spec);
        // This will auto add them to the keystore.
        generator.generateKeyPair();
    }

    public void setCredentials(String email, String password) {
        setEmail(email);
        setPassword(password);
    }

    private void setPassword(String password) {
        String encryptedPassword = encrypt(password);
        mPrefs.edit().putString(PASSWORD_KEY, encryptedPassword).apply();
    }

    private void setEmail(String email) {
        // No need to encrypt since it's also stored as info on the systemuser.
        mPrefs.edit().putString(EMAIL_KEY, email).apply();
    }

    private String encrypt(String value) {
        // No keystore so just return the value because we can not encrypt it.
        if (mKeyStore == null) {
            return value;
        }

        byte[] encodedBytes;
        try {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) mKeyStore.getEntry(KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, entry.getCertificate().getPublicKey());
            encodedBytes = cipher.doFinal(value.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            mRemoteLogger.e("Encryption failed!");
            mRemoteLogger.e(Log.getStackTraceString(e));
            return value;
        }

        return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    }

    private String decrypt(String value) {
        if (value == null || mKeyStore == null) {
            return value;
        }

        byte[] encodedBytes;
        try {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) mKeyStore.getEntry(KEY_ALIAS, null);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, entry.getPrivateKey());
            encodedBytes = cipher.doFinal(Base64.decode(value, Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
            mRemoteLogger.e("Decryption failed!");
            mRemoteLogger.e(Log.getStackTraceString(e));
            return value;
        }

        return new String(encodedBytes);
    }

    public String getEmail() {
        return mPrefs.getString(EMAIL_KEY, null);
    }

    public String getPassword() {
        String encryptedPassword = mPrefs.getString(PASSWORD_KEY, null);
        return decrypt(encryptedPassword);
    }

    public void clearCredentials() {
        mPrefs.edit().remove(EMAIL_KEY).apply();
        mPrefs.edit().remove(PASSWORD_KEY).apply();
    }
}
