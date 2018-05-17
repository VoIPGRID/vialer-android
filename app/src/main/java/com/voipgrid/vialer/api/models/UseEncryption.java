package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

public class UseEncryption {

    @SerializedName("appaccount_use_encryption")
    private Boolean useEncryption;

    public UseEncryption(Boolean useEncryption) {
        this.useEncryption = useEncryption;
    }

    public Boolean getUseEncryption() {
        return useEncryption;
    }

    public void setUseEncryption(Boolean useEncryption) {
        this.useEncryption = useEncryption;
    }
}
