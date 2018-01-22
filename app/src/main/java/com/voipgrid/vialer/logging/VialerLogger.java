package com.voipgrid.vialer.logging;

import android.content.Context;

import com.logentries.logger.AsyncLoggingWorker;

import java.util.ArrayList;

public class VialerLogger {

    private final ArrayList<AsyncLoggingWorker> environments = new ArrayList<>();

    private Context mContext;

    private LogEntriesFactory mLogEntriesFactory;

    public VialerLogger(Context context, LogEntriesFactory logEntriesFactory) {
        mContext = context;
        mLogEntriesFactory = logEntriesFactory;
    }

    /**
     * Add logging environments for all supplied tokens.
     *
     * @param logTokens
     */
    public VialerLogger initialize(String[] logTokens) {
        for (String logToken : logTokens) {
            addLoggingEnvironment(logToken);
        }

        return this;
    }

    /**
     * Send a log message to all configured logging environments.
     *
     * @param message
     */
    public void log(String message) {
        for (AsyncLoggingWorker logger : environments) {
            logger.addLineToQueue(message);
        }
    }

    /**
     * Add a logging environment by providing the required token.
     *
     * @param token
     */
    public void addLoggingEnvironment(String token) {
        if (token == null || token.isEmpty()) return;

        AsyncLoggingWorker logger = mLogEntriesFactory.createLogger(token, mContext);

        if (logger == null) return;

        environments.add(logger);
    }
}
