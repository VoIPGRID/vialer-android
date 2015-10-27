package com.voipgrid.vialer.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by bwiegmans on 29/07/15.
 */
public class ClickToDialParams {
    @SerializedName("a_number")
    public final String aNumber;

    @SerializedName("b_number")
    public final String bNumber;

    @SerializedName("a_cli")
    public final String aCli;

    @SerializedName("b_cli")
    public final String bCli;
    public ClickToDialParams(String fromNumber, String toNumber) {
        aNumber = fromNumber;
        bNumber = toNumber;
        aCli = "default_number";
        bCli = "default_number";
    }
}
