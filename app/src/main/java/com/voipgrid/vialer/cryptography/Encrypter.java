package com.voipgrid.vialer.cryptography;

import android.content.Context;
import android.util.Log;

import com.voipgrid.vialer.logging.Logger;
import com.yakivmospan.scytale.Crypto;
import com.yakivmospan.scytale.ErrorListener;
import com.yakivmospan.scytale.Options;
import com.yakivmospan.scytale.Store;

import javax.crypto.SecretKey;

public class Encrypter implements ErrorListener {

    private static final String KEY_ALIAS = "PASS_ALIAS_SYMMETRIC";

    private final Logger mLogger;
    private final Crypto mCrypto;
    private final Store mStore;

    public Encrypter(Context context) {
        mCrypto = new Crypto(Options.TRANSFORMATION_SYMMETRIC);
        mStore = new Store(context);
        mLogger = new Logger(this.getClass());
        mCrypto.setErrorListener(this);
        mStore.setErrorListener(this);
    }

    /**
     * Encrypt a value using the pre-set algorithms and keystore.
     *
     * @param value The String to encrypt
     * @return The encrypted value as a base64 encoded string
     */
    public String encrypt(String value) {
        return mCrypto.encrypt(value, getKey());
    }

    /**
     * Decrypt a base64 encoded encrypted value to the original string.
     *
     * @param value The base64 encoded encrypted value
     * @return The unencrypted value as a string
     */
    public String decrypt(String value) {
        return mCrypto.decrypt(value, getKey());
    }

    /**
     * Find the key in the store if it exists, otherwise generate it and store it.
     *
     * @return
     */
    private SecretKey getKey() {
        if (!mStore.hasKey(KEY_ALIAS)) {
            mStore.generateSymmetricKey(KEY_ALIAS, null);
        }
        return mStore.getSymmetricKey(KEY_ALIAS, null);
    }

    @Override
    public void onError(Exception e) {
        mLogger.e("Crypto failed with exception: " + Log.getStackTraceString(e));
    }
}
