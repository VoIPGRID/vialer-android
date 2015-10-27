package com.voipgrid.vialer.api.models;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eltjo on 16/09/15.
 */
public class UserDestination {

    @SerializedName("fixeddestinations")
    private List<FixedDestination> fixedDestinations;

    @SerializedName("phoneaccounts")
    private List<PhoneAccount> phoneAccounts;

    @SerializedName("selecteduserdestination")
    private SelectedUserDestination selectedUserDestination;

    @SerializedName("id")
    private String id;

    public List<FixedDestination> getFixedDestinations() {
        return fixedDestinations;
    }

    public void setFixedDestinations(List<FixedDestination> fixedDestinations) {
        this.fixedDestinations = fixedDestinations;
    }

    public List<PhoneAccount> getPhoneAccounts() {
        return phoneAccounts;
    }

    public void setPhoneAccounts(List<PhoneAccount> phoneAccounts) {
        this.phoneAccounts = phoneAccounts;
    }

    public SelectedUserDestination getSelectedUserDestination() {
        return selectedUserDestination;
    }

    public void setSelectedUserDestination(SelectedUserDestination selectedUserDestination) {
        this.selectedUserDestination = selectedUserDestination;
    }

    public Destination getActiveDestination() {
        // Je moet door de phone accounts en fixed accounts heen loopen om de lijst van mogelijke
        // nummers waar je bereikbaar op bent.
        List<Destination> destinations = getDestinations();

        // In de selecteduserdestination zitten twee keys welke het id hebben van of een phone
        // account of een fixed destination en van die keys heeft er altijd eentje een waarde
        if(selectedUserDestination != null) {
            String selectedDestinationId = null;
            if(!TextUtils.isEmpty(selectedUserDestination.fixedDestinationId)) {
                selectedDestinationId = selectedUserDestination.fixedDestinationId;
            } else if(!TextUtils.isEmpty(selectedUserDestination.phoneAccountId)) {
                selectedDestinationId = selectedUserDestination.phoneAccountId;
            }

            if(!TextUtils.isEmpty(selectedDestinationId)) {
                // tijdens de loop door de phone accounts en fixed destinations moet je dan een de
                // check hebben of de waarde van de key in de selecteduserdestination gelijk zijn
                for(int i=0, size=destinations.size(); i<size; i++) {
                    Destination destination = destinations.get(i);
                    if(selectedDestinationId.equals(destination.getId())) {
                        return destination;
                    }
                }
            }
        }
        return null;
    }

    public List<Destination> getDestinations() {
        List<Destination> destinations = new ArrayList();
        if(fixedDestinations != null) {
            destinations.addAll(fixedDestinations);
        }
        if(phoneAccounts != null) {
            destinations.addAll(phoneAccounts);
        }
        return destinations;
    }

    private class SelectedUserDestination {

        @SerializedName("fixeddestination")
        private String fixedDestinationId;

        @SerializedName("phoneaccount")
        private String phoneAccountId;

        private int id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
