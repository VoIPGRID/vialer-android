package com.voipgrid.vialer.sip;

import android.util.Log;

import com.voipgrid.vialer.util.RemoteLogger;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

import java.util.regex.Pattern;

public class SIPLogWriter extends LogWriter {
    private final static String TAG = SIPLogWriter.class.getSimpleName(); // TAG used for debug Logs

    RemoteLogger remoteLogger = null;
    private static final Pattern sipRegex = Pattern.compile("sip:\\d+");

    public void enabledRemoteLogging(RemoteLogger logger) {
        remoteLogger = logger;
    }

    public void disableRemoteLogging() {
        remoteLogger = null;
    }


    @Override
    public void write(LogEntry entry) {
        Integer pjsipLogLevel = entry.getLevel();
        String logString = entry.getMsg().substring(13);
        boolean remoteLogging = remoteLogger != null;
        // Strip sip messages.
        if (remoteLogging) {
            logString =  sipRegex.matcher(logString).replaceAll("sip:SIP_USER_ID");
            if (logString.contains("\n")) {
                Integer newLinePosition = logString.indexOf("\n");
                logString = logString.substring(0, newLinePosition);
            }
        }

        switch (pjsipLogLevel){
            case 1:
                if (remoteLogging){
                    remoteLogger.e(logString);
                } else {
                    Log.e(TAG, logString);
                }
                break;
            case 2:
                if (remoteLogging){
                    remoteLogger.w(logString);
                } else {
                    Log.w(TAG, logString);
                }
                break;
            case 3:
                if (remoteLogging){
                    remoteLogger.i(logString);
                } else {
                    Log.i(TAG, logString);
                }
                break;
            case 4:
                if (remoteLogging){
                    remoteLogger.d(logString);
                } else {
                    Log.d(TAG, logString);
                }
                break;
            default:
                if (remoteLogging){
                    remoteLogger.v(logString);
                } else {
                    Log.v(TAG, logString);
                }
                break;
        }
    }
}
