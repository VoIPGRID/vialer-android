package com.voipgrid.vialer.sip;

import android.util.Log;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

import static java.lang.String.format;

public class SIPLogWriter extends LogWriter {
    private final static String TAG = SIPLogWriter.class.getSimpleName(); // TAG used for debug Logs

    @Override
    public void write(LogEntry entry) {
        String logMessage = entry.getMsg().substring(13);
        String logString = format("Level %d: %s", entry.getLevel(), logMessage);
        Log.d(TAG, logString);
    }
}
