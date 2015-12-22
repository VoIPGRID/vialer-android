package com.voipgrid.vialer.api.models;

/**
 * Created by eltjo on 17/09/15.
 */
public interface Destination {

    String getId();

    void setId(String id);

    String getNumber();

    void setNumber(String number);

    String getDescription();

    void setDescription(String description);

    String toString();
}
