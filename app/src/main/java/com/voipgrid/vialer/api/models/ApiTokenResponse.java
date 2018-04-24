package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

public class ApiTokenResponse {

    @SerializedName("api_token")
    String apiToken;

    /**
     * Return the api token from the response, this token should be used to authenticate
     * all future request.
     *
     * @return The api token provided after authentication
     */
    public String getApiToken() {
        return apiToken;
    }
}
