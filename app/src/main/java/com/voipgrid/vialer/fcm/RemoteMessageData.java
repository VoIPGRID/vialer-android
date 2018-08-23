package com.voipgrid.vialer.fcm;

import java.util.Map;

public class RemoteMessageData {

    private final static String MESSAGE_TYPE = "type";

    private final static String CALL_REQUEST_TYPE = "call";
    private final static String MESSAGE_REQUEST_TYPE = "message";

    private final static String RESPONSE_URL = "response_api";
    private final static String REQUEST_TOKEN = "unique_key";
    private final static String PHONE_NUMBER = "phonenumber";
    private final static String CALLER_ID = "caller_id";
    private static final String SUPPRESSED = "supressed";
    private static final String ATTEMPT = "attempt";
    static final String MESSAGE_START_TIME = "message_start_time";

    private Map<String, String> mData;

    public RemoteMessageData(Map<String, String> data) {
        mData = data;
    }

    public String getRequestType() {
        return this.mData.get(MESSAGE_TYPE);
    }

    public boolean hasRequestType() {
        return getRequestType() != null;
    }

    public boolean isCallRequest() {
        return getRequestType().equals(CALL_REQUEST_TYPE);
    }

    public boolean isMessageRequest() {
        return getRequestType().equals(MESSAGE_REQUEST_TYPE);
    }

    public String getCallerId() {
        return propertyOrEmptyString(CALLER_ID);
    }

    public String getResponseUrl() {
        return propertyOrEmptyString(RESPONSE_URL);
    }

    public String getRequestToken() {
        return propertyOrEmptyString(REQUEST_TOKEN);
    }

    public String getMessageStartTime() {
        return propertyOrEmptyString(MESSAGE_START_TIME);
    }

    public String getPhoneNumber() {
        return propertyOrEmptyString(PHONE_NUMBER);
    }

    public String getSupressed() {
        return propertyOrEmptyString(SUPPRESSED);
    }

    public int getAttemptNumber() {
        return Integer.parseInt(propertyOrEmptyString(ATTEMPT));
    }

    public Map<String, String> getRawData() {
        return mData;
    }

    private String propertyOrEmptyString(String property) {
        return mData.get(property) != null ? mData.get(property) : "";
    }

    public boolean isNumberSuppressed() {
        return mData.get(PHONE_NUMBER) != null && (getPhoneNumber().equalsIgnoreCase(SUPPRESSED) || getPhoneNumber().toLowerCase().contains("xxxx"));
    }
}
