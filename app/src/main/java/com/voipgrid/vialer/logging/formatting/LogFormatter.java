package com.voipgrid.vialer.logging.formatting;

import com.voipgrid.vialer.logging.formatting.formatters.PayloadAnonymizer;

public class LogFormatter {

    private static final Formatter[] formatters = new Formatter[] {
            new PayloadAnonymizer(),
    };

    /**
     * Applies all registered formatters to the log message.
     *
     * @param tag
     * @param message
     * @return
     */
    public String applyAllFormatters(String tag, String message) {
        for(Formatter formatter : formatters) {

            if(!formatter.shouldFormat(tag, message)) continue;

            message = formatter.format(tag, message);
        }

        return message;
    }
}
