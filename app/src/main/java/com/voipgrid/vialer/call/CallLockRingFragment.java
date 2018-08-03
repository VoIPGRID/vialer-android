package com.voipgrid.vialer.call;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voipgrid.vialer.CallActivity;
import com.voipgrid.vialer.R;
import com.wearespindle.spindlelockring.library.LockRing;
import com.wearespindle.spindlelockring.library.OnTriggerListener;

/**
 * Fragment for when the phone is locked for the user on an incoming call.
 */
public class CallLockRingFragment extends Fragment implements OnTriggerListener {
    private LockRing mLockRing;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_lock_ring, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLockRing = (LockRing) view.findViewById(R.id.lock_ring);
        mLockRing.setOnTriggerListener(this);
        mLockRing.setShowTargetsOnIdle(false);
    }

    @Override
    public void onGrabbed(View view, int handle) {

    }

    @Override
    public void onReleased(View view, int handle) {

    }

    @Override
    public void onTrigger(View view, int target) {
        final int resourceId = mLockRing.getResourceIdForTarget(target);
        switch (resourceId) {
            case R.drawable.ic_lock_ring_answer:
                ((CallActivity) getActivity()).answer();
                break;
            case R.drawable.ic_lock_ring_decline:
//                ((CallActivity) getActivity()).decline();
                break;
        }
        mLockRing.reset(true);
    }

    @Override
    public void onGrabbedStateChange(View view, int handle) {

    }

    @Override
    public void onFinishFinalAnimation() {

    }
}
