package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_PUT_ON_HOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.CALL_RINGING_IN_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_RINGING_OUT_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_UNHOLD_ACTION;
import static com.voipgrid.vialer.sip.SipConstants.SERVICE_STOPPED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipConstants;

public class CallStatusReceiver extends BroadcastReceiver {

    private final Listener mListener;
    private final Logger mLogger;

    public CallStatusReceiver(Listener listener) {
        mListener = listener;
        mLogger = new Logger(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = intent.getStringExtra(SipConstants.CALL_STATUS_KEY);
        String callId = intent.getStringExtra(SipConstants.CALL_IDENTIFIER_KEY);

        mLogger.i("Dispatching call status " + status + " for call id " + callId + " to " + mListener.getClass().getSimpleName());

        switch (status) {
            case CALL_CONNECTED_MESSAGE: mListener.onCallConnected(); break;
            case CALL_DISCONNECTED_MESSAGE: mListener.onCallDisconnected(); break;
            case CALL_PUT_ON_HOLD_ACTION: mListener.onCallHold(); break;
            case CALL_UNHOLD_ACTION: mListener.onCallUnhold(); break;
            case CALL_RINGING_OUT_MESSAGE: mListener.onCallRingingOut(); break;
            case CALL_RINGING_IN_MESSAGE: mListener.onCallRingingIn(); break;
            case SERVICE_STOPPED: mListener.onServiceStopped(); break;
        }

        mListener.onCallStatusChanged(status, callId);
    }

    interface Listener {
        void onCallStatusChanged(String status, String callId);

        void onCallConnected();

        void onCallDisconnected();

        void onCallHold();

        void onCallUnhold();

        void onCallRingingOut();

        void onCallRingingIn();

        void onServiceStopped();
    }
}
