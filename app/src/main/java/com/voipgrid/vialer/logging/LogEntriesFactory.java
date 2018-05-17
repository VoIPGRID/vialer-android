package com.voipgrid.vialer.logging;

import android.content.Context;

import com.logentries.logger.AsyncLoggingWorker;

public class LogEntriesFactory {

    /**
     * Creates a new LogEntries logger with default settings configured.
     *
     * @param token
     * @param context
     * @return The LogEntries logger.
     */
    public AsyncLoggingWorker createLogger(String token, Context context) {
        try {
            return new AsyncLoggingWorker(context, false, false, false, token, null, 0, false);
        } catch (Exception e) {
            return null;
        }
    }
}
