package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by eltjo on 17/09/15.
 */
public class SelectedUserDestinationParams {

    @SerializedName("fixeddestination")
    public String fixedDestination;

    @SerializedName("phoneaccount")
    public String phoneAccount;

    public String toString() {
        return "fixeDdestination: " + fixedDestination + " voipAccount: " + phoneAccount;
    }
}
