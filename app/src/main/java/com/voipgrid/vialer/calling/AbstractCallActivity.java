package com.voipgrid.vialer.calling;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import com.voipgrid.vialer.sip.SipService;
import com.voipgrid.vialer.util.LoginRequiredActivity;

public abstract class AbstractCallActivity extends LoginRequiredActivity implements
        SipServiceConnection.SipServiceConnectionListener, CallDurationTracker.Listener {

    protected SipServiceConnection mSipServiceConnection;
    protected String mCurrentCallId;
    protected CallDurationTracker mCallDurationTracker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSipServiceConnection = new SipServiceConnection(this);
        mCallDurationTracker = new CallDurationTracker(mSipServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSipServiceConnection.connect();
        mCallDurationTracker.start(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSipServiceConnection.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    @CallSuper
    public void sipServiceHasConnected(SipService sipService) {
        if (sipService.getFirstCall() != null) {
            mCurrentCallId = sipService.getFirstCall().getIdentifier();
        }
    }

    @Override
    @CallSuper
    public void sipServiceBindingFailed() {}

    @Override
    @CallSuper
    public void sipServiceHasBeenDisconnected() {}

    @Override
    public void onCallDurationUpdate(long duration) {}
}
