package com.voipgrid.vialer.calling;

import android.view.View;

import com.voipgrid.vialer.R;
import com.wearespindle.spindlelockring.library.LockRing;
import com.wearespindle.spindlelockring.library.OnTriggerListener;

public class LockRingListener implements OnTriggerListener {

    private final LockRing mView;
    private final IncomingCallActivity mIncomingCallActivity;

    public LockRingListener(LockRing view, IncomingCallActivity incomingCallActivity) {
        mView = view;
        view.setShowTargetsOnIdle(false);
        mIncomingCallActivity = incomingCallActivity;
    }

    @Override
    public void onGrabbed(View view, int i) {

    }

    @Override
    public void onReleased(View view, int i) {

    }

    @Override
    public void onTrigger(View view, int target) {
        final int resourceId = mView.getResourceIdForTarget(target);
        switch (resourceId) {
            case R.drawable.ic_lock_ring_answer:
                mIncomingCallActivity.onPickupButtonClicked();
                break;
            case R.drawable.ic_lock_ring_decline:
                mIncomingCallActivity.onDeclineButtonClicked();
                break;
        }
        mView.reset(true);
    }

    @Override
    public void onGrabbedStateChange(View view, int i) {

    }

    @Override
    public void onFinishFinalAnimation() {

    }
}
