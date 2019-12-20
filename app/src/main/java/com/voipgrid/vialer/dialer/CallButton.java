package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.util.AttributeSet;

import com.voipgrid.vialer.R;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

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
     * Make the call button grey and stopping user interaction.
     *
     */
    public void disable() {
        this.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.call_pickup_button_disabled));
        this.setEnabled(false);
    }

    /**
     * Make the call button green and allowing user interaction.
     */
    public void enable() {
        this.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.call_pickup_button));
        this.setEnabled(true);
    }
}
