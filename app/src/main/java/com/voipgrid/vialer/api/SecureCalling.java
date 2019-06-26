package com.voipgrid.vialer.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.api.models.UpdateVoIPAccountParameters;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.util.JsonStorage;

import retrofit2.Call;
import retrofit2.Response;

public class SecureCalling {

    /**
     * This is a flag that has the current VoIP account id appended to the end (e.g.
     * PREF_VOIP_ACCOUNT_HAS_SECURE_CALLING_ENABLED_123456) to track whether secure calling has been
     * successfully enabled for this account. This allows Vialer to maintain greater consistency
     * with the VoIP account secure calling setting.
     */
    private static final String PREF_VOIP_ACCOUNT_HAS_SECURE_CALLING_ENABLED =
            "PREF_VOIP_ACCOUNT_HAS_SECURE_CALLING_ENABLED_";

    /**
     * An event that is broadcast when the secure calling API issues a response.
     */
    public static final String ACTION_SECURE_CALLING_API_CALL_RESPONSE = "ACTION_SECURE_CALLING_API_CALL_RESPONSE";

    public static final String EXTRA_API_CALL_SUCCEEDED = "EXTRA_API_CALL_SUCCEEDED";

    public static final String EXTRA_API_CALL_WAS_ATTEMPTING_TO_ENABLE = "EXTRA_API_WAS_ATTEMPTING_TO_ENABLE";

    private SharedPreferences mSharedPreferences;
    private VoipgridApi mVoipgridApi;
    private Logger mLogger;
    private Preferences mPreferences;
    private String mIdentifier;
    private LocalBroadcastManager mLocalBroadcastManager;

    /**
     * @param identifier This is the identifier that will be appended to the end of the shared
     *                   preference, this should uniquely identify the current VoIP account and
     *                   be updated when the VoIP account is switched.
     */
    public SecureCalling(SharedPreferences sharedPreferences, VoipgridApi voipgridApi, Preferences preferences,
            String identifier, LocalBroadcastManager localBroadcastManager, Logger logger) {
        mSharedPreferences = sharedPreferences;
        mVoipgridApi = voipgridApi;
        mIdentifier = identifier;
        mPreferences = preferences;
        mLocalBroadcastManager = localBroadcastManager;
        mLogger = logger;
    }

    /**
     * A named constructor to easily create an instance of SecureCalling from only a context.
     *
     * @return A new instance of SecureCalling.
     */
    public static SecureCalling fromContext(Context context) {
        SystemUser systemUser = null;
        JsonStorage jsonStorage = new JsonStorage(context);
        Logger logger = new Logger(SecureCalling.class);

        if (jsonStorage.has(SystemUser.class)) {
            systemUser = (SystemUser) jsonStorage.get(SystemUser.class);
        } else {
            logger.e("Attempted to use SecureCalling with no SystemUser available");
        }

        return new SecureCalling(
                PreferenceManager.getDefaultSharedPreferences(context),
                ServiceGenerator.createApiService(context),
                new Preferences(context),
                systemUser != null ? systemUser.getPhoneAccountId() : "",
                LocalBroadcastManager.getInstance(context),
                logger
        );
    }

    /**
     * Enable secure calling asynchronously.
     *
     * @param callback The callback that will be triggered when a response has been received from
     *                the api.
     */
    public void enable(Callback callback) {
        createCall(true).enqueue(new HttpCallback(true, callback));
    }

    /**
     * Disable secure calling asynchronously.
     *
     * @param callback The callback that will be triggered when a response has been received from
     *                the api.
     */
    public void disable(Callback callback) {
        createCall(false).enqueue(new HttpCallback(false, callback));
    }


    /**
     * Sends an API request using whatever the tls switch in advanced settings is currently
     * set to.
     */
    public void updateApiBasedOnCurrentPreferenceSetting(Callback callback) {
        if (mPreferences.hasTlsEnabled()) {
            enable(callback);
        } else {
            disable(callback);
        }
    }

    public void updateApiBasedOnCurrentPreferenceSetting() {
        if (mPreferences.hasTlsEnabled()) {
            enable(new EmptyCallback());
        } else {
            disable(new EmptyCallback());
        }
    }

    private Call<UpdateVoIPAccountParameters> createCall(boolean enable) {
        mLogger.i("Sending an API request to set secure calling to: " + enable);

        return mVoipgridApi.updateVoipAccount(new UpdateVoIPAccountParameters(enable));
    }

    /**
     * Check if, to the best of our knowledge, the current setting against the VoIP account
     * matches the TLS switch in advanced settings.
     */
    public boolean isSetCorrectly() {
        return isEnabled() == mPreferences.hasTlsEnabled();
    }

    /**
     * Check if secure calling has already been enabled via the API.
     *
     * @return TRUE if a successful call has been made to the API to enable secure calling.
     */
    public boolean isEnabled() {
        return mSharedPreferences.getBoolean(getSharedPreferencesKey(), false);
    }

    /**
     * Check if secure calling has been specifically disabled, unlike the isEnabled() method
     * this will not return TRUE unless we know it has been disabled rather than just not
     * having information yet.
     *
     * @return TRUE if it has been disabled, otherwise FALSE
     */
    public boolean hasBeenDisabled() {
        return mSharedPreferences.contains(getSharedPreferencesKey()) && !isEnabled();
    }

    /**
     * Perform various actions when a successful call has been made to the API.
     */
    private void handleSuccessfulApiCall(boolean enabled) {
        mSharedPreferences.edit().putBoolean(getSharedPreferencesKey(), enabled).apply();
        mLocalBroadcastManager.sendBroadcast(createIntent(true, enabled));
    }

    private void handleFailedApiCall(boolean enabled) {
        mLocalBroadcastManager.sendBroadcast(createIntent(false, enabled));
    }

    private Intent createIntent(boolean success, boolean enabled) {
        Intent intent = new Intent(ACTION_SECURE_CALLING_API_CALL_RESPONSE);
        intent.putExtra(EXTRA_API_CALL_SUCCEEDED, success);
        intent.putExtra(EXTRA_API_CALL_WAS_ATTEMPTING_TO_ENABLE, enabled);
        return intent;
    }

    /**
     * Generates the key for the shared preferences flag this should be unique per
     * VoIP account.
     *
     * @return The key for use in shared preferences
     */
    private String getSharedPreferencesKey() {
        return PREF_VOIP_ACCOUNT_HAS_SECURE_CALLING_ENABLED + mIdentifier;
    }

    /**
     * A simple callback interface so the caller can receive an update when the secure calling
     * API call has been completed.
     */
    public interface Callback {

        /**
         * The secure calling API call was successful.
         */
        void onSuccess();

        /**
         * The secure calling API call failed.
         */
        void onFail();
    }

    /**
     * The OkHTTP callback that SecureCalling uses internally to handle the result from the API
     * call.
     */
    private class HttpCallback implements retrofit2.Callback<UpdateVoIPAccountParameters> {
        private boolean mEnable;
        private Callback mCallback;

        HttpCallback(boolean enable, Callback callback) {
            mEnable = enable;
            mCallback = callback;
        }

        @Override
        public void onResponse(Call<UpdateVoIPAccountParameters> call, Response<UpdateVoIPAccountParameters> response) {
            if (response.isSuccessful()) {
                handleSuccessfulApiCall(mEnable);

                mLogger.i("Secure calling API call successfully set to: " + mEnable);

                if (mCallback != null) {
                    mCallback.onSuccess();
                }

                return;
            }

            mLogger.e("Secure calling API call failed with code: " + response.code());

            handleFailedApiCall(mEnable);

            if (mCallback != null) {
                mCallback.onFail();
            }
        }

        @Override
        public void onFailure(Call<UpdateVoIPAccountParameters> call, Throwable t) {
            mLogger.e("Secure calling API call failed with error: " + t.getMessage());

            handleFailedApiCall(mEnable);

            if (mCallback != null) {
                mCallback.onFail();
            }
        }
    }

     public static class EmptyCallback implements Callback {
        @Override
        public void onSuccess() {

        }

        @Override
        public void onFail() {

        }
    }
}
