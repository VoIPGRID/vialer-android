package com.voipgrid.vialer.call;

import static com.voipgrid.vialer.call.ui.buttons.CallActionButton.DISABLED_ALPHA;
import static com.voipgrid.vialer.call.ui.buttons.CallActionButton.ENABLED_ALPHA;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;

public class HangupButton extends AppCompatImageButton {
    public HangupButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void enable() {
        this.setActivated(true);
        this.setAlpha(ENABLED_ALPHA);
        this.setClickable(true);
    }

    public void disable() {
        this.setActivated(false);
        this.setAlpha(DISABLED_ALPHA);
        this.setClickable(false);
    }
}
