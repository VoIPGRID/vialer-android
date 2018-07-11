package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.statistics.VialerStatistics;

import java.lang.ref.WeakReference;

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
    private final RemoteLogger mRemoteLogger;
    private SipService mSipService;

    private CallSetupChecker (String requestToken, String messageStartTime, String attempt) {
        mRequestToken = requestToken;
        mMessageStartTime = messageStartTime;
        mAttempt = attempt;
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
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
        boolean continueChecking = true;

        while (continueChecking) {
            continueChecking = check();
        }
    }

    /**
     * Looks for whether or not the call is active in the SipService or if we have reached the maximum allowed time for checking.
     *
     * @return Return a boolean depending on whether or not we should continue checking for the call.
     */
    private boolean check() {
        if (isSipServiceHandlingOurCall()){
            mRemoteLogger.i("Confirmed call from fcm message (" + mRequestToken + ") has been setup");
            return false;
        }

        if (hasMaximumAllowedTimeExpired()) {
            VialerStatistics.noCallReceivedFromAsteriskAfterOkToMiddleware(mRequestToken, mMessageStartTime, mAttempt);
            mRemoteLogger.e("Unable to confirm call from fcm message (" + mRequestToken + ") was setup correctly");
            return false;
        }

        sleep();

        return true;
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
