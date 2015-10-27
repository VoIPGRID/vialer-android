package com.voipgrid.vialer.api.models;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

/**
 * Created by eltjo on 31/07/15.
 */
public class SystemUser {

    @SerializedName("first_name")
    String firstName;

    @SerializedName("last_name")
    String lastName;

    String email;

    @SerializedName("outgoing_cli")
    String outgoingCli;

    @SerializedName("app_account")
    String appAccountUri;

    String password;

    @SerializedName("mobile_nr")
    String mobileNumber;

    String partner;

    @SerializedName("allow_app_account")
    boolean hasSipPermission;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOutgoingCli() {
        return outgoingCli;
    }

    public void setOutgoingCli(String outgoingCli) {
        this.outgoingCli = outgoingCli;
    }

    public String getAppAccountUri() {
        return appAccountUri;
    }

    public void setAppAccountUri(String appAccountUri) {
        this.appAccountUri = appAccountUri;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public String getPhoneAccountId() {
        if(appAccountUri != null) {
            Uri uri = Uri.parse(appAccountUri);
            return uri.getLastPathSegment();
        }
        return null;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public boolean hasSipPermission() {
        return hasSipPermission;
    }

    public void setSipPermission(boolean hasSipPermission) {
        this.hasSipPermission = hasSipPermission;
    }
}
