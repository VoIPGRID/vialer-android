package com.voipgrid.vialer.models;

import com.google.gson.annotations.SerializedName;

public class PasswordResetParams {

    @SerializedName("email")
    public final String mEmail;

    public PasswordResetParams(String email) {
        mEmail = email;
    }
}
