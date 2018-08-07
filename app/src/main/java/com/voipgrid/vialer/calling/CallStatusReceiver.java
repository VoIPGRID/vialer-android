package com.voipgrid.vialer.calling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.sip.SipConstants;

public class CallStatusReceiver extends BroadcastReceiver {

    private final Listener mListener;

    public CallStatusReceiver(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mListener.onCallStatusReceived(
                intent.getStringExtra(SipConstants.CALL_STATUS_KEY),
                intent.getStringExtra(SipConstants.CALL_IDENTIFIER_KEY)
        );
    }

    interface Listener {
        void onCallStatusReceived(String status, String callId);
    }
}
