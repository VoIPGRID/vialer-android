package com.voipgrid.vialer.sip;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Helper class for sending broadcasts from background SIP classes to listening activities.
 */
class SipBroadcaster {
    private LocalBroadcastManager mBroadcastManager;

    private SipService mSipService;

    SipBroadcaster(SipService sipService) {
        mSipService = sipService;
        setUp();
    }

    private void setUp() {
        mBroadcastManager = LocalBroadcastManager.getInstance(mSipService);
    }

    void broadcastServiceInfo(String info) {
        Intent intent = new Intent(SipConstants.ACTION_BROADCAST_SERVICE_INFO);
        intent.putExtra(SipConstants.SERVICE_INFO_KEY, info);
        mBroadcastManager.sendBroadcast(intent);
    }

    void broadcastCallStatus(String identifier, String status) {
        Intent intent = new Intent(SipConstants.ACTION_BROADCAST_CALL_STATUS);
        intent.putExtra(SipConstants.CALL_IDENTIFIER_KEY, identifier);
        intent.putExtra(SipConstants.CALL_STATUS_KEY, status);
        mBroadcastManager.sendBroadcast(intent);
    }

    void broadcastMissedCalls(SipConstants.CallMissedReason reason) {
        Intent intent = new Intent(SipConstants.ACTION_BROADCAST_CALL_MISSED);
        intent.putExtra(SipConstants.CALL_MISSED_KEY, reason);
        mBroadcastManager.sendBroadcast(intent);
    }
}
