package com.voipgrid.vialer.logging.formatting;


public interface Formatter {

    String format(String tag, String message);

    boolean shouldFormat(String tag, String message);
}
