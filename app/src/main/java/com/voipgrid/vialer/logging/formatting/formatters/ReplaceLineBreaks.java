package com.voipgrid.vialer.logging.formatting.formatters;

import com.voipgrid.vialer.logging.formatting.Formatter;

public class ReplaceLineBreaks implements Formatter {
    @Override
    public String format(String tag, String message) {
        return message.replaceAll("[\r\n]+", " ");
    }

    @Override
    public boolean shouldFormat(String tag, String message) {
        return message.contains("\n");
    }
}
