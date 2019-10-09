package com.voipgrid.vialer.sip;

import android.util.Log;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.logging.sip.SipLogHandler;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

public class SipLogWriter extends LogWriter {
    private Logger mLogger;

    private SipLogHandler mSipLogHandler = new SipLogHandler();

    void enabledRemoteLogging(Logger logger) {
        mLogger = logger;
    }

    void disableRemoteLogging() {
        mLogger.i("Prematurely disable the remote logger");
        mLogger = null;
    }

    @Override
    public void write(LogEntry entry) {
        Integer pjsipLogLevel = entry.getLevel();
        String logString = entry.getMsg().substring(13);
Log.e("TEST1SIP", "PjSip:" +entry.getMsg());
        mSipLogHandler.handle(logString);

        if (mLogger == null) {
            return;
        }

        switch (pjsipLogLevel){
            case 1:
                mLogger.e(logString);
                break;
            case 2:
                mLogger.w(logString);
                break;
            case 3:
                mLogger.i(logString);
                break;
            case 4:
                mLogger.d(logString);
                break;
            default:
                mLogger.v(logString);
                break;
        }
    }
}
