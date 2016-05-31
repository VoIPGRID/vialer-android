package com.wearespindle.googlelockring;

import android.view.View;

public interface OnTriggerListener {
    int NO_HANDLE = 0;
    int CENTER_HANDLE = 1;

    void onGrabbed(View view, int handle);

    void onReleased(View view, int handle);

    void onTrigger(View view, int target);

    void onGrabbedStateChange(View view, int handle);

    void onFinishFinalAnimation();
}