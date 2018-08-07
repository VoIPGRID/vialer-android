package com.voipgrid.vialer.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.logging.sip.SipLogHandler;
import com.voipgrid.vialer.statistics.VialerStatistics;
import com.voipgrid.vialer.util.BroadcastReceiverManager;

/**
 * Regularly checks that a call has been setup after receiving a push notification.
 */
public class CallSetupChecker {

    /**
     * The number of seconds that we wait for a call before we determine that it has failed
     * to setup.
     */
    private static final int MILLISECONDS_ALLOWED_BEFORE_CALL_FAILED_TO_SETUP = 5000;

    /**
     * The time between each check that is performed, this should be set to a low enough value
     * that it will always catch a call that has been active for even a short amount of time.
     */
    private static final int CHECK_INTERVAL_IN_MILLISECONDS = 50;

    private final String mRequestToken;
    private final String mMessageStartTime;
    private final String mAttempt;
    private long mCheckStartTime;
    private final Logger mLogger;
    private SipService mSipService;
    private BroadcastReceiverManager mBroadcastReceiverManager;

    /**
     * Controls the call checking loop, if this is ever set to false, checking for the call will stop.
     *
     */
    private boolean continueChecking = true;

    /**
     * Broadcast receiver that will listen for a sip error code from the logs, if found will send the metrics for call
     * failed with sip error code.
     *
     */
    private BroadcastReceiver mSipErrorCodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            continueChecking = false;
            mBroadcastReceiverManager.unregisterReceiver(this);
            VialerStatistics.incomingCallFailedDueToSipError(
                    mRequestToken,
                    mMessageStartTime,
                    mAttempt,
                    intent.getIntExtra(SipLogHandler.EXTRA_SIP_ERROR_CODE, 0)
            );
        }
    };

    private CallSetupChecker (String requestToken, String messageStartTime, String attempt) {
        mRequestToken = requestToken;
        mMessageStartTime = messageStartTime;
        mAttempt = attempt;
        mBroadcastReceiverManager = BroadcastReceiverManager.fromContext(VialerApplication.get());
        mLogger = new Logger(this.getClass());
    }

    /**
     * Create a new callsetupchecker with the relevant information from the middleware push message.
     *
     * @param requestToken
     * @param messageStartTime
     * @param attempt
     * @return
     */
    public static CallSetupChecker withPushMessageInformation(String requestToken, String messageStartTime, String attempt) {
        return new CallSetupChecker(requestToken, messageStartTime, attempt);
    }

    /**
     * Starts a new thread that will regularly check that the sip service is handling the call.
     *
     */
    public void start(SipService sipService) {
        mSipService = sipService;

        mCheckStartTime = System.currentTimeMillis();

        new Thread(this::loop).start();
    }

    /**
     * Calls the check method continuously.
     *
     */
    private void loop() {
        mBroadcastReceiverManager.registerReceiverViaLocalBroadcastManager(mSipErrorCodeReceiver, SipLogHandler.INVITE_FAILED_WITH_SIP_ERROR_CODE);

        while (continueChecking) {
            check();
        }
    }

    /**
     * Looks for whether or not the call is active in the SipService or if we have reached the maximum allowed time for checking.
     *
     * @return Return a boolean depending on whether or not we should continue checking for the call.
     */
    private void check() {
        if (isSipServiceHandlingOurCall()){
            mLogger.i("Confirmed call from fcm message (" + mRequestToken + ") has been setup");
            continueChecking = false;
        }

        if (hasMaximumAllowedTimeExpired()) {
            VialerStatistics.noCallReceivedFromAsteriskAfterOkToMiddleware(mRequestToken, mMessageStartTime, mAttempt);
            mLogger.e("Unable to confirm call from fcm message (" + mRequestToken + ") was setup correctly");
            continueChecking = false;
        }

        sleep();
    }

    /**
     * Determine if we have been checking for longer than the allowed time.
     *
     * @return
     */
    private boolean hasMaximumAllowedTimeExpired() {
        return System.currentTimeMillis() >= (mCheckStartTime + MILLISECONDS_ALLOWED_BEFORE_CALL_FAILED_TO_SETUP);
    }

    /**
     * Check if the SipService is currently busy with the call we are checking for.
     */
    private boolean isSipServiceHandlingOurCall() {
        return mSipService != null && mSipService.getCurrentCall() != null && mRequestToken.equals(
                mSipService.getCurrentCall().getMiddlewareKey());
    }

    /**
     * Sleep the current thread for the specified check interval.
     *
     */
    private void sleep() {
        try {
            Thread.sleep(CHECK_INTERVAL_IN_MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
