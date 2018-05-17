package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.logging.sip.SipLogHandler;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

public class SipLogWriter extends LogWriter {
    private RemoteLogger mRemoteLogger;

    private SipLogHandler mSipLogHandler = new SipLogHandler();

    void enabledRemoteLogging(RemoteLogger logger) {
        mRemoteLogger = logger;
    }

    void disableRemoteLogging() {
        mRemoteLogger.i("Prematurely disable the remote logger");
        mRemoteLogger = null;
    }

    @Override
    public void write(LogEntry entry) {
        Integer pjsipLogLevel = entry.getLevel();
        String logString = entry.getMsg().substring(13);

        mSipLogHandler.handle(logString);

        if (mRemoteLogger == null) {
            return;
        }

        switch (pjsipLogLevel){
            case 1:
                mRemoteLogger.e(logString);
                break;
            case 2:
                mRemoteLogger.w(logString);
                break;
            case 3:
                mRemoteLogger.i(logString);
                break;
            case 4:
                mRemoteLogger.d(logString);
                break;
            default:
                mRemoteLogger.v(logString);
                break;
        }
    }
}
