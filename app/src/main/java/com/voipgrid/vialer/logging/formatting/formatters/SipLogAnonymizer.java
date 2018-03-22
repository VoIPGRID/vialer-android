package com.voipgrid.vialer.logging.formatting.formatters;

import com.voipgrid.vialer.logging.formatting.Formatter;
import com.voipgrid.vialer.sip.SipService;

import java.util.regex.Pattern;

public class SipLogAnonymizer implements Formatter {
    @Override
    public String format(String tag, String message) {
        message = Pattern.compile("sip:\\+?\\d+").matcher(message).replaceAll("sip:SIP_USER_ID");
        message = Pattern.compile("\"caller_id\" = (.+?);").matcher(message).replaceAll("<CALLER_ID>");
        message = Pattern.compile("To:(.+?)>").matcher(message).replaceAll("To: <SIP_ANONYMIZED>");
        message = Pattern.compile("From:(.+?)>").matcher(message).replaceAll("From: <SIP_ANONYMIZED>");
        message = Pattern.compile("Contact:(.+?)>").matcher(message).replaceAll("Contact: <SIP_ANONYMIZED>");
        message = Pattern.compile("Digest username=\"(.+?)\"").matcher(message).replaceAll("Digest username=\"<SIP_USERNAME>\"");
        message = Pattern.compile("nonce=\"(.+?)\"").matcher(message).replaceAll("nonce=\"<NONCE>\"");
        message = Pattern.compile("username=(.+?)&").matcher(message).replaceAll("username=<USERNAME>");

        return message;
    }

    @Override
    public boolean shouldFormat(String tag, String message) {
        return tag.contains(SipService.class.getSimpleName()) || tag.contains("Pjsip");
    }
}
