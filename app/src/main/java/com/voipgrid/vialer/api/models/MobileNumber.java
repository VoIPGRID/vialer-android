package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by eltjo on 06/08/15.
 */
public class MobileNumber {

    @SerializedName("mobile_nr")
    private String mobileNumber;

    public MobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}
