package com.voipgrid.vialer.api.models;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;

import androidx.annotation.Nullable;

/**
 * Model for storing SystemUser info and parsing of JSON to model.
 */
public class SystemUser {

    @SerializedName("first_name")
    String firstName;

    @SerializedName("last_name")
    String lastName;

    String email;

    String preposition;

    @SerializedName("outgoing_cli")
    String outgoingCli;

    @SerializedName("app_account")
    String appAccountUri;

    String password;

    @SerializedName("mobile_nr")
    String mobileNumber;

    String partner;

    @SerializedName("client")
    String clientId;

    /**
     * HashSet with list of Strings describing numbers which are not the main number
     * for multi entries in contact list. They are stored in a set because that forces
     * deduplication.
     */
    private HashSet<String> secondaryNumbers;

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

    /**
     * This a LEGACY method and should not be used anymore.
     * @return
     */
    @Deprecated
    public String getPassword() {
        return password;
    }

    /**
     * This a LEGACY method and should not be used anymore.
     */
    @Deprecated
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
        if(getPreposition().equals("")) {
            return getFirstName() + " "  + getLastName();
        }

        return getFirstName() + " " + getPreposition() + " " + getLastName();
    }

    @Nullable
    public String getVoipAccountId() {
        if(appAccountUri != null) {
            Uri uri = Uri.parse(appAccountUri);
            return uri.getLastPathSegment();
        }
        return null;
    }

    public boolean hasVoipAccount() {
        return getVoipAccountId() != null;
    }

    public void setClient(String client) {
        // This method is not called by the serialization of the api so we have
        // to escape the string in the get function.
        this.clientId = client;
    }

    public String getClient() {
        // Make sure we only have the numeric part of the client url.
        if (this.clientId != null) {
            return this.clientId.replaceAll("\\D+", "");
        }
        return this.clientId;
    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public HashSet<String> getSecondaryNumbers() {
        return this.secondaryNumbers;
    }

    public void setSecondaryNumbers(HashSet<String> secondaryNumbers) {
        this.secondaryNumbers = secondaryNumbers;
    }

    public String getPreposition() {
        return preposition != null ? preposition : "";
    }
}
