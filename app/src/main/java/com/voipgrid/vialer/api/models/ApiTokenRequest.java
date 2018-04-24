package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

public class ApiTokenRequest {

    String email;

    String password;

    @SerializedName("two_factor_token")
    String twoFactorCode;

    /**
     * Generate an Api Token Request with all necessary information.
     *
     * @param email The user's email that is used to authenticate with the Api
     * @param password The user's password that is used to authenticate with the Api
     * @param twoFactorCode The user's current 2fa code
     */
    public ApiTokenRequest(String email, String password, String twoFactorCode) {
        this.email = email;
        this.password = password;
        this.twoFactorCode = twoFactorCode;
    }

    /**
     * Generate an Api Token Request with just an email address and password, this request will fail
     * if 2fa is enabled on the account.
     *
     * @param email The user's email that is used to authenticate with the Api
     * @param password The user's password that is used to authenticate with the Api
     */
    public ApiTokenRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
