package com.voipgrid.vialer.logging.formatting.formatters;

import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.logging.formatting.Formatter;

import java.util.regex.Pattern;

public class PayloadAnonymizer implements Formatter {

    @Override
    public String format(String tag, String message) {

        message = Pattern.compile("caller_id=(.+?),").matcher(message).replaceAll("callerid=<CALLER_ID>,");
        message = Pattern.compile("phonenumber=(.+?),").matcher(message).replaceAll("phonenumber=<PHONENUMBER>,");

        return message;
    }

    @Override
    public boolean shouldFormat(String tag, String message) {
        return tag.contains(FcmMessagingService.class.getSimpleName());
    }
}
