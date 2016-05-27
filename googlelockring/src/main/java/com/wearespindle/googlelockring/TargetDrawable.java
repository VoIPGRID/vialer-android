package com.wearespindle.googlelockring;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class TargetDrawable {
    private static final String TAG = "TargetDrawable";
    private static final boolean DEBUG = true;

    public static final int[] STATE_ACTIVE =
            { android.R.attr.state_enabled, android.R.attr.state_active };
    public static final int[] STATE_INACTIVE =
            { android.R.attr.state_enabled, -android.R.attr.state_active };
    public static final int[] STATE_FOCUSED =
            { android.R.attr.state_enabled, -android.R.attr.state_active,
                    android.R.attr.state_focused };

    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mPositionX = 0.0f;
    private float mPositionY = 0.0f;
    private float mAlpha = 1.0f;
    private Drawable mDrawable;
    private boolean mEnabled = true;
    private final int mResourceId;
    private int mNumDrawables = 1;

    /**
     * This is changed from the framework version to pass in the number of drawables in the
     * container. The framework version relies on private api's to get the count from
     * StateListDrawable.
     *
     * @param res The resource
     * @param resId The resource id
     * @param count The number of drawables in the resource.
     */
    public TargetDrawable(Resources res, int resId, int count) {
        mResourceId = resId;
        setDrawable(res, resId);
        mNumDrawables = count;
    }

    public void setDrawable(Resources res, int resId) {
        // Note we explicitly don't set mResourceId to resId since we allow the drawable to be
        // swapped at runtime and want to re-use the existing resource id for identification.
        Drawable drawable = resId == 0 ? null : res.getDrawable(resId);
        // Mutate the drawable so we can animate shared drawable properties.
        mDrawable = drawable != null ? drawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public void setState(int [] state) {
        if (mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) mDrawable;
            d.setState(state);
        }
    }

    /**
     * Typically an enabled target contains a valid
     * drawable in a valid state. Currently all targets with valid drawables are valid.
     *
     * @return boolean true if this target is enabled.
     */
    public boolean isEnabled() {
        return mDrawable != null && mEnabled;
    }

    /**
     * Makes drawables in a StateListDrawable all the same dimensions.
     * If not a StateListDrawable, then justs sets the bounds to the intrinsic size of the
     * drawable.
     */
    private void resizeDrawables() {
        if (mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) mDrawable;
            int maxWidth = 0;
            int maxHeight = 0;

            for (int i = 0; i < mNumDrawables; i++) {
                d.selectDrawable(i);
                Drawable childDrawable = d.getCurrent();
                maxWidth = Math.max(maxWidth, childDrawable.getIntrinsicWidth());
                maxHeight = Math.max(maxHeight, childDrawable.getIntrinsicHeight());
            }

            if (DEBUG) {
                Log.v(TAG, "union of childDrawable rects " + d + " to: " + maxWidth + "x" + maxHeight);
            }

            d.setBounds(0, 0, maxWidth, maxHeight);

            for (int i = 0; i < mNumDrawables; i++) {
                d.selectDrawable(i);
                Drawable childDrawable = d.getCurrent();

                if (DEBUG) {
                    Log.v(TAG, "sizing drawable " + childDrawable + " to: " + maxWidth + "x" + maxHeight);
                }
                childDrawable.setBounds(0, 0, maxWidth, maxHeight);
            }
        } else if (mDrawable != null) {
            mDrawable.setBounds(0, 0,
                    mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
        }
    }

    public void setX(float x) {
        mTranslationX = x;
    }

    public void setY(float y) {
        mTranslationY = y;
    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setPositionX(float x) {
        mPositionX = x;
    }

    public void setPositionY(float y) {
        mPositionY = y;
    }

    public int getWidth() {
        return mDrawable != null ? mDrawable.getIntrinsicWidth() : 0;
    }

    public int getHeight() {
        return mDrawable != null ? mDrawable.getIntrinsicHeight() : 0;
    }

    public void draw(Canvas canvas) {
        if (mDrawable == null || !mEnabled) {
            return;
        }

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.scale(scaleX, scaleY, mPositionX, mPositionY);
        canvas.translate(mTranslationX + mPositionX, mTranslationY + mPositionY);
        canvas.translate(-0.5f * getWidth(), -0.5f * getHeight());
        mDrawable.setAlpha(Math.round(mAlpha * 255f));
        mDrawable.draw(canvas);
        canvas.restore();
    }

    public void setEnabled(boolean enabled) {
        mEnabled  = enabled;
    }

    public int getResourceId() {
        return mResourceId;
    }
}