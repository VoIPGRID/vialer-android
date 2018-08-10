package com.voipgrid.vialer.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.voipgrid.vialer.logging.Logger;

import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.IpChangeParam;

public class IpSwitchMonitor extends BroadcastReceiver {

    /**
     * The time we should wait between receiving a network change until we actually try
     * and use the new IP, this must be long enough for the phone to fully connect
     * to the new network.
     *
     */
    private static final int NETWORK_SWITCH_DELAY_MS = 500;

    /**
     * Flag so we know when the network change is in progress so multiple are not queued
     * up at the same time.
     *
     */
    private boolean isChangingNetwork = false;

    private final Logger mLogger;
    private SipService mSipService;
    private Endpoint mEndpoint;
    private Handler mHandler;

    public IpSwitchMonitor() {
        mLogger = new Logger(this.getClass());
        mHandler = new Handler();
    }

    /**
     * Initialise this class with the required pjsip objects.
     *  @param sipService
     * @param endpoint
     */
    public IpSwitchMonitor init(SipService sipService, Endpoint endpoint) {
        mSipService = sipService;
        mEndpoint = endpoint;

        return this;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isChangingNetwork) return;

        mLogger.d("Received a network change: " + intent.getAction());

        if(isInitialStickyBroadcast()) {
            mLogger.i("Ignoring network change as broadcast is old (sticky).");
            return;
        }

        isChangingNetwork = true;

        mHandler.postDelayed(this::actionAfterDelay, NETWORK_SWITCH_DELAY_MS);
    }

    /**
     * The action to be performed after the delay has occurred.
     *
     */
    private void actionAfterDelay() {
        mLogger.d("Wait " + NETWORK_SWITCH_DELAY_MS + "ms before doing the network switch");
        doIpSwitch();
        isChangingNetwork = false;
    }

    /**
     * When there is a change in the network make use of the PJSIP handleIpChange
     * functionality to handle the change in the network.
     */
    private void doIpSwitch() {
        mLogger.v("doIpSwitch()");
        IpChangeParam ipChangeParam = new IpChangeParam();
        ipChangeParam.setRestartListener(false);

        SipCall sipCall = null;
        if (mSipService != null && mSipService.getCurrentCall() != null) {
            sipCall = mSipService.getCurrentCall();
            sipCall.setIsIPChangeInProgress(true);
        }

        if (sipCall == null) {
            return;
        }

        mLogger.i("Make PJSIP handle the ip address change.");
        try {
            mEndpoint.handleIpChange(ipChangeParam);
        } catch (Exception e) {
            mLogger.w("PJSIP failed to change the ip address");
            e.printStackTrace();
        }
    }
}
