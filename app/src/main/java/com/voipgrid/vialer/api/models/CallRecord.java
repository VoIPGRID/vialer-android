package com.voipgrid.vialer.api.models;

import android.text.format.DateFormat;

import com.google.gson.annotations.SerializedName;

import java.util.Calendar;

/**
 * Call record class
 */
public class CallRecord {

    public static final String DIRECTION_OUTBOUND = "outbound";
    public static final String DIRECTION_INBOUND = "inbound";

    private static final String CALL_DATE_FORMAT = "yyyy-MM-dd";

    private int amount;

    @SerializedName("atime")
    private int duration;

    @SerializedName("call_date")
    private String callDate;

    @SerializedName("dialed_number")
    private String dialedNumber;

    @SerializedName("src_number")
    private String caller;

    private String direction;

    public static String getLimitDate() {
        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.MONTH, -1);
        return DateFormat.format(CallRecord.CALL_DATE_FORMAT, currentTime).toString();
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getCallDate() {
        return callDate;
    }

    public void setCallDate(String callDate) {
        this.callDate = callDate;
    }

    public String getDialedNumber() {
        return dialedNumber;
    }

    public void setDialedNumber(String dialedNumber) {
        this.dialedNumber = dialedNumber;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
