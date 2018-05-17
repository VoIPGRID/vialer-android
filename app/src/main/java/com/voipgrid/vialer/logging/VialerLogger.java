package com.voipgrid.vialer.logging;

import android.content.Context;

import com.logentries.logger.AsyncLoggingWorker;

import java.util.ArrayList;
import java.util.HashMap;

public class VialerLogger {

    /**
     * All environments in this list will be logged to whenever log() method is
     * called.
     */
    private final ArrayList<AsyncLoggingWorker> environments = new ArrayList<>();

    /**
     * This HashMap holds onto environments that were logged to specifically so they can be reused
     * if needed, the token is used as the key.
     */
    private final HashMap<String, AsyncLoggingWorker> standAloneEnvironments = new HashMap<>();

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

    /**
     * Log a message to a specific environment. The environment will be kept alive and reused
     * for future requests.
     *
     * @param token
     * @param message
     */
    public void logToEnvironment(String token, String message) {
        if(!standAloneEnvironments.containsKey(token)) {
            standAloneEnvironments.put(
                    token,
                    mLogEntriesFactory.createLogger(token, mContext)
            );
        }

        standAloneEnvironments.get(token).addLineToQueue(message);
    }
}
