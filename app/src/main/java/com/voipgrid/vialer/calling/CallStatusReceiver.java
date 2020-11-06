package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.sip.SipConstants.CALL_CONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_DISCONNECTED_MESSAGE;
import static com.voipgrid.vialer.sip.SipConstants.CALL_STATUS_CODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.CallDisconnectedReason;
import com.voipgrid.vialer.sip.SipConstants;

import org.openvoipalliance.phonelib.model.Reason;
import org.openvoipalliance.phonelib.model.Call;

import androidx.annotation.Nullable;

public class CallStatusReceiver extends BroadcastReceiver {

    private final Listener mListener;
    private final Logger mLogger;

    public CallStatusReceiver(Listener listener) {
        mListener = listener;
        mLogger = new Logger(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = intent.getStringExtra(SipConstants.CALL_STATUS_KEY) + "";
        String callId = intent.getStringExtra(SipConstants.CALL_IDENTIFIER_KEY);


        mLogger.i("Dispatching call status " + status + " for call id " + callId + " to " + mListener.getClass().getSimpleName());

        switch (status) {
            case CALL_CONNECTED_MESSAGE: mListener.onCallConnected(); break;
            case CALL_DISCONNECTED_MESSAGE:
                mListener.onCallDisconnected();
                break;
        }

        mListener.onCallStatusChanged(status, callId);
    }

    public interface Listener {
        void onCallStatusChanged(String status, String callId);

        void onCallConnected();

        void onCallDisconnected();
    }
}
