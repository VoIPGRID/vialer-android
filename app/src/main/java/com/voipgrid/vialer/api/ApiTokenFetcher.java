package com.voipgrid.vialer.api;

import android.content.Context;

import com.voipgrid.vialer.api.models.ApiTokenRequest;
import com.voipgrid.vialer.api.models.ApiTokenResponse;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.AccountHelper;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiTokenFetcher {

    private final String mUsername;
    private final String mPassword;
    private final Api mApi;
    private final AccountHelper mAccountHelper;
    private final RemoteLogger mRemoteLogger;

    private ApiTokenListener mListener;

    private ApiTokenFetcher(String username, String password, Api api, AccountHelper accountHelper, RemoteLogger remoteLogger) {
        mUsername = username;
        mPassword = password;
        mApi = api;
        mAccountHelper = accountHelper;
        mRemoteLogger = remoteLogger;
    }

    /**
     * Set a listener to handle results from fetching the api token.
     *
     * @param listener
     * @return
     */
    public ApiTokenFetcher setListener(ApiTokenListener listener) {
        mListener = listener;

        return this;
    }

    /**
     * Fetch the api token with the given credentials.
     *
     * @param username The username to authenticate with
     * @param password The password to authenticate with
     */
    public static ApiTokenFetcher forCredentials(Context context, String username, String password) {
        return new ApiTokenFetcher(
                username,
                password,
                ServiceGenerator.createApiService(context, null, null, null),
                new AccountHelper(context),
                new RemoteLogger(ApiTokenFetcher.class)
        );
    }

    /**
     * Fetch the api token using the credentials saved in AccountHelper.
     *
     */
    public static ApiTokenFetcher usingSavedCredentials(Context context) {
        AccountHelper accountHelper = new AccountHelper(context);

        return new ApiTokenFetcher(
                accountHelper.getEmail(),
                accountHelper.getPassword(),
                ServiceGenerator.createApiService(context, null, null, null),
                new AccountHelper(context),
                new RemoteLogger(ApiTokenFetcher.class)
        );
    }

    /**
     * Perform the request to get the api token.
     *
     */
    public void fetch() {
        enqueue(
                new ApiTokenRequest(mUsername, mPassword),
                new ApiTokenHttpResponseHandler(false)
        );
    }


    /**
     * Perform the second fetch with a two factor authentication code attached.
     *
     * @param twoFactorCode
     */
    public void fetch(String twoFactorCode) {
        enqueue(
                new ApiTokenRequest(mUsername, mPassword, twoFactorCode),
                new ApiTokenHttpResponseHandler(true)
        );
    }

    /**
     * Enqueue a request to be handled by the response handler.
     *
     * @param request
     * @param responseHandler
     */
    private void enqueue(ApiTokenRequest request, ApiTokenHttpResponseHandler responseHandler) {
        mApi
                .apiToken(request)
                .enqueue(responseHandler);
    }

    /**
     * Listen to the possible responses from requesting an Api token.
     *
     */
    public interface ApiTokenListener {

        /**
         * An api token request has been made and it has failed due to
         * no two-factor code being provided.
         *
         */
        void twoFactorCodeRequired();

        /**
         * The request was successful and an api token was retrieved.
         *
         * @param apiToken The api token to be used for future authentication requests
         */
        void onSuccess(String apiToken);

        /**
         * If the api token request has failed, this can be due to any of the credentials
         * provided being incorrect.
         *
         */
        void onFailure();
    }

    /**
     * Handles the api response from requesting an api token and calls listener methods based
     * on it.
     *
     */
    private class ApiTokenHttpResponseHandler implements Callback<ApiTokenResponse> {

        private final boolean mDidSupplyTwoFactorCode;

        ApiTokenHttpResponseHandler(boolean didSupplyTwoFactorCode) {
            mDidSupplyTwoFactorCode = didSupplyTwoFactorCode;
        }

        @Override
        public void onResponse(Call<ApiTokenResponse> call, Response<ApiTokenResponse> response) {
            if (response.isSuccessful()) {
                mRemoteLogger.i("Successfully retrieved an api-key");
                ApiTokenResponse apiTokenResponse = response.body();
                mAccountHelper.setApiToken(apiTokenResponse.getApiToken());
                mListener.onSuccess(apiTokenResponse.getApiToken());
                return;
            }

            String errorString = errorString(response);

            if (!mDidSupplyTwoFactorCode && responseIndicatesThat2faIsRequired(errorString)) {
                mRemoteLogger.i("Attempt to retrieve an api-key failed because a two-factor code is required");
                mListener.twoFactorCodeRequired();
                return;
            }

            mRemoteLogger.e("Failed to retrieve an api-key with code: " + response.code() + ". Request included a two-factor code: " + mDidSupplyTwoFactorCode);

            mListener.onFailure();
        }

        @Override
        public void onFailure(Call<ApiTokenResponse> call, Throwable t) {
            mListener.onFailure();
        }

        /**
         * Attempts to get the error string from the response, on failure will return an empty string.
         *
         * @param response
         * @return
         */
        private String errorString(Response<ApiTokenResponse> response) {
            try {
                return response.errorBody().string();
            } catch (IOException e) {
                return "";
            }
        }

        /**
         * Check the response to see if the reason for failure is due to no
         * two-factor code being provided.
         *
         * @param errorString The response body as a string
         * @return TRUE if the responses is stating the the two-factor code is missing
         */
        private boolean responseIndicatesThat2faIsRequired(String errorString) {
            return errorString.contains("two_factor_token");
        }
    }
}
