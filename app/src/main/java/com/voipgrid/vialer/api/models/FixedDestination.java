package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by eltjo on 16/09/15.
 */
public class FixedDestination implements Destination {

    private String id;

    @SerializedName("phonenumber")
    private String number;

    private String description;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        if(number == null) {
            return description;
        }
        return number + " / " + description;
    }
}
