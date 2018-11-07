package com.voipgrid.vialer.call;

public class DisplayCallDetail {

    private String number, callerId;

    public DisplayCallDetail(String number, String callerId) {
        this.number = number;
        this.callerId = callerId;
    }

    public String getNumber() {
        return number;
    }

    public String getCallerId() {
        return callerId;
    }
}
