package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;
import com.voipgrid.vialer.util.StringUtil;
/**
 * Created by eltjo on 03/08/15.
 */
public class PhoneAccount implements Destination {

    @SerializedName("account_id")
    private String accountId;

    private String password;

    @SerializedName("internal_number")
    private String number;

    private String id;

    private String description;

    private String country;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getNumber() {
        return number;
    }

    @Override
    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return getNumber() + " / " + getDescription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhoneAccount that = (PhoneAccount) o;

        if (getAccountId() == null) return false;
        if (!getAccountId().equals(that.getAccountId())) return false;
        if (!getPassword().equals(that.getPassword())) return false;
        return getNumber().equals(that.getNumber());
    }

    /**
     * Extract the country from the country string.
     *
     * @return
     */
    public String getCountry() {
        if (country == null) {
            return null;
        }

        if (!country.contains("/")) {
            return country;
        }
        return StringUtil.extractFirstCaptureGroupFromString(country, "\\/([a-z]+)\\/$");
    }
}
