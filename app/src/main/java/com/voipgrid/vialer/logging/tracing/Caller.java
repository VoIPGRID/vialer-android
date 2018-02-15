package com.voipgrid.vialer.logging.tracing;

import java.lang.reflect.Method;

public class Caller {

    private String callingClass;

    private String callingMethod;

    private int lineNumber;

    public Caller(String callingClass, String callingMethod, int lineNumber) {
        this.callingClass = callingClass;
        this.callingMethod = callingMethod;
        this.lineNumber = lineNumber;
    }

    public String getCallingClass() {
        return callingClass;
    }

    public String getCallingMethod() {
        return callingMethod;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Formats the caller details to be readable for adding to something like a log file.
     *
     * @return String readable caller details
     */
    public String format() {

        String locationInClass = "";

        if(getCallingMethod() != null) {
            locationInClass = "(" + getLineNumber() + ")" + "::" + getCallingMethod() + "()";
        }

        return getShortCallingClass() + locationInClass;
    }

    /**
     * Takes only the class name from the fully qualified class name.
     *
     * @return String The name of the calling class.
     */
    private String getShortCallingClass() {
        String[] parts = getCallingClass().split("\\.");
        return parts[parts.length - 1];
    }
}
