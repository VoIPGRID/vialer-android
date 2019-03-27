package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

public class UpdateVoIPAccountParameters {

    @SerializedName("appaccount_use_encryption")
    private Boolean useEncryption;

    @SerializedName("appaccount_use_opus")
    private Boolean useOpus = true;

    public UpdateVoIPAccountParameters(Boolean useEncryption) {
        this.useEncryption = useEncryption;
    }

    public Boolean getUseEncryption() {
        return useEncryption;
    }

    public void setUseEncryption(Boolean useEncryption) {
        this.useEncryption = useEncryption;
    }
}
