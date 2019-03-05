package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.util.StringUtil;

import java.util.ArrayList;

/**
 * This object represents the original invite that initiated the call.
 */
public class SipInvite {

    /**
     * The names of headers that will be extracted from the invite packet.
     */
    private static final String
            P_ASSERTED_IDENTITY_HEADER_NAME = "P-Asserted-Identity",
            REMOTE_PARTY_ID_HEADER_NAME = "Remote-Party-ID";

    private final String packet;
    private final String callId;
    private final String time;

    private CallerInformationHeader pAssertedIdentity;
    private CallerInformationHeader remotePartyId;
    private CallerInformationHeader from;

    /**
     * Initialise the SipInvite.
     *
     * @param packet The whole INVITE that was received with the incoming call.
     */
    public SipInvite(String packet) {
        this.packet = packet;
        this.pAssertedIdentity = extractFromLikeHeader(P_ASSERTED_IDENTITY_HEADER_NAME);
        this.remotePartyId = extractFromLikeHeader(REMOTE_PARTY_ID_HEADER_NAME);
        this.callId = extractHeader("Call-ID");
        this.time = extractHeader("Date");
        this.from = extractFromLikeHeader("From");
    }

    private CallerInformationHeader extractFromLikeHeader(String header) {
        ArrayList<String> extracted = StringUtil.extractCaptureGroups(packet, header + ": ([^;]+)");

        if (extracted.isEmpty()) {
            return null;
        }

        ArrayList<String> data = StringUtil.extractCaptureGroups(extracted.get(0), "^\"(.+)\" <?sip:(.+)@");

        if (data.size() < 2) {
            return null;
        }

        return new CallerInformationHeader(data.get(0), data.get(1));
    }

    private String extractHeader(String header){
        ArrayList<String> extracted = StringUtil.extractCaptureGroups(packet, header + ": (.+)$");

        if (extracted.isEmpty()) return null;

        return extracted.get(0);
    }

    public CallerInformationHeader getFrom() {
        return from;
    }

    public boolean hasPAssertedIdentity() {
        return pAssertedIdentity != null;
    }

    public boolean hasRemotePartyId() {
        return remotePartyId != null;
    }

    public CallerInformationHeader getPAssertedIdentity() {
        return pAssertedIdentity;
    }

    public CallerInformationHeader getRemotePartyId() {
        return remotePartyId;
    }

    public String getCallId() {
        return this.callId;
    }

    public String getTime() {
        return time;
    }

    /**
     * Represents the information in any header that contains caller information.
     */
    public static class CallerInformationHeader {
        public String name;
        public String number;

        public CallerInformationHeader(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }
}
