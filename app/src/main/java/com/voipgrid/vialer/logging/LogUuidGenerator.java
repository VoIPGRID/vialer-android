package com.voipgrid.vialer.logging;

import java.util.UUID;

public class LogUuidGenerator {

    /**
     * Generate a random unique id for logging purposes.
     *
     * @return String The random id.
     */
    public static String generate() {
        String uuid = UUID.randomUUID().toString();
        int stripIndex = uuid.indexOf("-");
        uuid = uuid.substring(0, stripIndex);
        return uuid;
    }
}
