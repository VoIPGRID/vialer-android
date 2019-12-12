package com.voipgrid.vialer.calling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.CallDisconnectedReason;
import com.voipgrid.vialer.sip.SipConstants;

import org.pjsip.pjsua2.pjsip_status_code;

import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_PUT_ON_HOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.CALL_RINGING_IN_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_RINGING_OUT_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_UNHOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.SERVICE_STOPPED;

public class CallStatusReceiver extends BroadcastReceiver {

    private final Listener mListener;
    private final Logger mLogger;

    public CallStatusReceiver(Listener listener) {
        mListener = listener;
        mLogger = new Logger(this);
    }

    private static final int DEFAULT_STATUS_CODE = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = intent.getStringExtra(SipConstants.CALL_STATUS_KEY) + "";
        String callId = intent.getStringExtra(SipConstants.CALL_IDENTIFIER_KEY);

        int statusCodeInt = intent.getIntExtra(SipConstants.CALL_STATUS_CODE, DEFAULT_STATUS_CODE);
        pjsip_status_code statusCode = statusCodeInt != DEFAULT_STATUS_CODE
            ? pjsip_status_code.swigToEnum(statusCodeInt)
            : null;

        mLogger.i("Dispatching call status " + status + " for call id " + callId + " to " + mListener.getClass().getSimpleName());

        switch (status) {
            case CALL_CONNECTED_MESSAGE: mListener.onCallConnected(); break;
            case CALL_DISCONNECTED_MESSAGE:
                mListener.onCallDisconnected(
                    CallDisconnectedReason.Companion.fromStatusCode(statusCode)
                );
                break;
            case CALL_PUT_ON_HOLD_ACTION: mListener.onCallHold(); break;
            case CALL_UNHOLD_ACTION: mListener.onCallUnhold(); break;
            case CALL_RINGING_OUT_MESSAGE: mListener.onCallRingingOut(); break;
            case CALL_RINGING_IN_MESSAGE: mListener.onCallRingingIn(); break;
            case SERVICE_STOPPED: mListener.onServiceStopped(); break;
        }

        mListener.onCallStatusChanged(status, callId);
    }

    public interface Listener {
        void onCallStatusChanged(String status, String callId);

        void onCallConnected();

        void onCallDisconnected(CallDisconnectedReason reason);

        void onCallHold();

        void onCallUnhold();

        void onCallRingingOut();

        void onCallRingingIn();

        void onServiceStopped();
    }
}
