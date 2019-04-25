package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;

public class CallButton extends AppCompatImageButton {

    public CallButton(Context context) {
        super(context);
    }

    public CallButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CallButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Fade the call button out, making it transparent
     * and stopping user interaction.
     *
     */
    public void fadeOut() {
        this.setAlpha(0.3f);
        this.getBackground().setAlpha(0);
        this.setEnabled(false);
    }

    /**
     * Reverse the fadeOut() method, making it fully visible and
     * allowing user interaction.
     */
    public void fadeIn() {
        this.setAlpha(1f);
        this.getBackground().setAlpha(255);
        this.setEnabled(true);
    }
}
