package com.voipgrid.vialer.call;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;

import com.voipgrid.vialer.R;

public class CallActionButton extends AppCompatImageView {

    public static final float ENABLED_ALPHA = 1.0f, DISABLED_ALPHA = 0.5f;

    public CallActionButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void enable() {
        this.setActivated(true);
        this.setAlpha(ENABLED_ALPHA);
    }

    public void disable() {
        this.setActivated(false);
        this.setAlpha(DISABLED_ALPHA);
    }

    public void enable(boolean enable) {
        if (enable) {
            this.enable();
        } else {
            this.disable();
        }
    }

    public void activate() {
        this.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.color_primary, null));
        Drawable newDrawableSpeaker = this.getBackground();
        DrawableCompat.setTint(newDrawableSpeaker, Color.WHITE);
        this.setBackground(newDrawableSpeaker);
    }

    public void deactivate() {
        this.clearColorFilter();
        Drawable newDrawableSpeaker = this.getBackground();
        DrawableCompat.setTintList(newDrawableSpeaker, null);
        this.setBackground(newDrawableSpeaker);
    }

    public void activate(boolean activate) {
        if (activate) {
            this.activate();
        } else {
            this.deactivate();
        }
    }
}
