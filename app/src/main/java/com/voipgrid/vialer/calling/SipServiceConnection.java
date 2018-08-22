package com.voipgrid.vialer.calling;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.sip.SipService;

public class SipServiceConnection implements ServiceConnection {

    /**
     * The class that this ServiceConnection will be binding to.
     *
     */
    private static final Class SERVICE = SipService.class;

    /**
     * Set to TRUE when the sip service is successfully bound.
     *
     */
    private boolean mSipServiceBound = false;

    /**
     * Set to TRUE when an attempt to bind the service has been initiated
     * successfully. This does not mean that the service binding has been
     * completed, this may set to be true even when attempts to query
     * the service fail.
     *
     */
    private boolean mShouldUnbind = false;

    /**
     * Holds a reference to the service that we are binding to, in this case
     * the SipService.
     *
     */
    private SipService mSipService;

    /**
     * The activity that initiated the binding.
     *
     */
    private Activity mActivity;

    /**
     * An object that will receive events when the SipService binds
     * or when it fails to bind.
     *
     */
    private SipServiceConnectionListener mListener;

    /**
     * If the sip service has not successfully bound in this amount of time, the call
     * activity will attempt to finish.
     */
    private static final int MAX_ALLOWED_MS_TO_BIND_SIP_SERVICE = 3000;

    private Logger mLogger;

    /**
     * Create a SipServiceConnection with an activity, if the activity implements
     * the listener interface, it will be automatically assigned as the listener.
     *
     * @param activity
     */
    SipServiceConnection(Activity activity) {
        mLogger = new Logger(this.getClass());
        mActivity = activity;

        if (activity instanceof SipServiceConnectionListener) {
            mListener = (SipServiceConnectionListener) activity;
        }
    }

    /**
     * Create a SipServiceConnection manually specifying both the activity and the listener,
     * if there is a reason to have them be separate.
     *
     * @param activity
     * @param listener
     */
    SipServiceConnection(Activity activity, SipServiceConnectionListener listener) {
        this(activity);
        mListener = listener;
    }

    /**
     * Attempt to connect to the SipService, the actual binding will occur asynchronously and the listener
     * callback will be triggered.
     *
     */
    public void connect() {
        if (mSipServiceBound || mSipService != null) return;
Log.e("TEST123", "mShouldUnbind:" + mShouldUnbind);
        mLogger.i(mActivity.getClass().getSimpleName() + " is attempting to bind to SipService");

        if (mActivity.bindService(new Intent(mActivity, SERVICE), this, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        }

        // Make sure service is bound before updating status.
        new Handler().postDelayed(() -> {
            if (!mSipServiceBound) {
                mLogger.i(mActivity.getClass().getSimpleName() + " failed to bind to " + SERVICE.getSimpleName() + " after delay");
                mListener.sipServiceBindingFailed();
            }
        }, MAX_ALLOWED_MS_TO_BIND_SIP_SERVICE);
    }

    /**
     * Disconnect from the SipService, this disconnection will only happen if the SipService has been
     * properly shutdown and therefore has no active call.
     *
     * @param force If TRUE is passed, this will attempt to disconnect the service even if an active call exists.
     */
    public void disconnect(boolean force) {
        if ((hasActiveCall() && !force)|| !mShouldUnbind) return;

        mLogger.i(mActivity.getClass().getSimpleName() + " is attempting to unbind from " + SERVICE.getSimpleName());
        mActivity.unbindService(this);
        mShouldUnbind = false;
        mSipServiceBound = false;
        mListener.sipServiceHasBeenDisconnected();
    }

    public void disconnect() {
        disconnect(false);
    }

    /**
     * Check to see if the SipService is active.
     *
     * @return TRUE if the sip service is available and has an active call, otherwise FALSE.
     */
    public boolean hasActiveCall() {
        return mSipService != null && mSipService.getCurrentCall() != null;
    }

    public boolean isAvailableAndHasActiveCall() {
        return isAvailable() && hasActiveCall();
    }

    /**
     * Get the connected SipService, this should not be called until the connection has
     * been bound properly.
     *
     * @return
     */
    public SipService get() {
        return mSipService;
    }

    /**
     * Returns TRUE if the sip service is bound and available.
     *
     * @return
     */
    public boolean isAvailable() {
        return mSipServiceBound;
    }

    public boolean isAvailableOrIsAvailableSoon() {
        return mSipServiceBound || mShouldUnbind;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        mLogger.i(mActivity.getClass().getSimpleName() + " connected to " + SERVICE.getSimpleName());
        SipService.SipServiceBinder binder = (SipService.SipServiceBinder) service;
        mSipService = binder.getService();
        mSipServiceBound = true;
        mListener.sipServiceHasConnected(mSipService);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        mLogger.i(mActivity.getClass().getSimpleName() + " disconnected from " + SERVICE.getSimpleName());
    }

    /**
     * An interface to listen to receive callbacks regarding the
     * state of the SipService.
     *
     */
    public interface SipServiceConnectionListener {

        /**
         * Called when the SipService has successfully been bound.
         *
         * @param sipService
         */
        void sipServiceHasConnected(SipService sipService);

        /**
         * Called when the SipService has attempted to bind but has not
         * been bound successfully in {@value #MAX_ALLOWED_MS_TO_BIND_SIP_SERVICE}
         * milliseconds.
         *
         */
        void sipServiceBindingFailed();

        /**
         * Called when the SipService has been disconnected successfully.
         *
         */
        void sipServiceHasBeenDisconnected();
    }
}
