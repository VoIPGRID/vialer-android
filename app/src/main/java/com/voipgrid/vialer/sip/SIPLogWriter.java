package com.voipgrid.vialer.sip;

import android.util.Log;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

import static java.lang.String.format;

public class SIPLogWriter extends LogWriter {
    private final static String TAG = SIPLogWriter.class.getSimpleName(); // TAG used for debug Logs

    @Override
    public void write(LogEntry entry) {
        Integer pjsipLogLevel = entry.getLevel();
        String logMessage = entry.getMsg().substring(13);
        String logString = format("Level %d: %s", pjsipLogLevel, logMessage);
        switch (pjsipLogLevel){
            case 1:
                Log.e(TAG, logString);
                break;
            case 2:
                Log.w(TAG, logString);
                break;
            case 3:
                Log.i(TAG, logString);
                break;
            case 4:
                Log.d(TAG, logString);
                break;
            default:
                Log.v(TAG, logString);
                break;
        }
    }
}
