package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;


/**
 * Model for the selected user destination response.
 */
public class SelectedUserDestination {

    @SerializedName("fixeddestination")
    private String fixedDestinationId;

    @SerializedName("phoneaccount")
    private String phoneAccountId;

    @SerializedName("id")
    private String id;

    public String getId() {
        return id;
    }

    public String getFixedDestinationId() {
        return fixedDestinationId;
    }

    public String getPhoneAccountId() {
        return phoneAccountId;
    }
}
