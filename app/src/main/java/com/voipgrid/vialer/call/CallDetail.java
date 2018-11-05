package com.voipgrid.vialer.call;

import com.voipgrid.vialer.sip.SipCall;

public class CallDetail {

    private String identifier, phoneNumber, callerId;

    private CallDetail(String identifier, String phoneNumber, String callerId) {
        this.identifier = identifier;
        this.phoneNumber = phoneNumber;
        this.callerId = callerId;
    }

    public static CallDetail fromSipCall(SipCall sipCall) {
        return new CallDetail(sipCall.getIdentifier(), sipCall.getPhoneNumber(), sipCall.getCallerId());
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCallerId() {
        return callerId;
    }
}
