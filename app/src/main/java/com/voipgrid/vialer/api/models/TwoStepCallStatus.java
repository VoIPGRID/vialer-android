package com.voipgrid.vialer.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by eltjo on 27/08/15.
 */
public class TwoStepCallStatus {

    public static final String STATE_CONNECTED = "connected";
    public static final String STATE_DISCONNECTED = "disconnected";
    public static final String STATE_FAILED_A = "failed_a";
    public static final String STATE_FAILED_B = "failed_b";
    public static final String STATE_CALLING_A = "dialing_a";
    public static final String STATE_CALLING_B = "dialing_b";

    @SerializedName("a_cli")
    private String aCli;

    @SerializedName("a_number")
    private String aNumber;

    @SerializedName("auto_answer")
    private Boolean autoAnswer;

    @SerializedName("b_cli")
    private String bCli;

    @SerializedName("b_number")
    private String bNumber;

    @SerializedName("callid")
    private String callId;

    @SerializedName("created")
    private String created;

    @SerializedName("originating_ip")
    private String originatingIp;

    @SerializedName("resource_uri")
    private String resourceUri;

    @SerializedName("status")
    private String status;

    public String getaCli() {
        return aCli;
    }

    public void setaCli(String aCli) {
        this.aCli = aCli;
    }

    public String getaNumber() {
        return aNumber;
    }

    public void setaNumber(String aNumber) {
        this.aNumber = aNumber;
    }

    public Boolean getAutoAnswer() {
        return autoAnswer;
    }

    public void setAutoAnswer(Boolean autoAnswer) {
        this.autoAnswer = autoAnswer;
    }

    public String getbCli() {
        return bCli;
    }

    public void setbCli(String bCli) {
        this.bCli = bCli;
    }

    public String getbNumber() {
        return bNumber;
    }

    public void setbNumber(String bNumber) {
        this.bNumber = bNumber;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getOriginatingIp() {
        return originatingIp;
    }

    public void setOriginatingIp(String originatingIp) {
        this.originatingIp = originatingIp;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
