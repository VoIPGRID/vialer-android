package com.voipgrid.vialer.logging.tracing;

import java.util.HashMap;
import java.util.Map;

public class CallerLocator {

    /**
     * A list of translations, if the key is found in the class name of the Caller, it will
     * automatically be translated into the value before being returned. This is to make it easier
     * to understand when a useful caller cannot be found, for example all the output from PJSIP
     * will show as coming from the SipLogWriter.
     *
     */
    private static final HashMap<String, Caller> translations = new HashMap<>();

    static {
        translations.put("SipLogWriter", new Caller("Pjsip", null, 0));
    }

    /**
     * Finds information about where in the code the log statement came from. Performing this task frequently can
     * impact performance.
     *
     * @return The Caller that started this logging event.
     */
    public Caller locate() {
        StackTraceElement trace = findLastRelevantTrace(createStackTrace());

        Caller caller = new Caller(trace.getClassName(), trace.getMethodName(), trace.getLineNumber());

        return applyTranslations(caller);
    }

    /**
     * Attempts to look through the stack trace to determine which element originally initiated this log.
     *
     * @param trace
     * @return The most relevant StackTraceElement.
     */
    private StackTraceElement findLastRelevantTrace(StackTraceElement[] trace) {
        for(StackTraceElement e : trace) {
            if(e.getClassName().contains(".logging.")) continue;

            return e;
        }

        return trace[0];
    }

    /**
     * Attempts to find the translation based on whether or not the calling class can be found in the translations
     * list, if it is found the translation will be returned instead.
     *
     * @param caller
     * @return Caller The translated caller if found, otherwise the original caller.
     */
    private Caller applyTranslations(Caller caller) {
        for(Map.Entry<String, Caller> entry : translations.entrySet()) {
            if(caller.getCallingClass().contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return caller;
    }

    /**
     * Generates a stack trace that we will use to find the caller.
     */
    private StackTraceElement[] createStackTrace() {
        return new Throwable().getStackTrace();
    }
}
