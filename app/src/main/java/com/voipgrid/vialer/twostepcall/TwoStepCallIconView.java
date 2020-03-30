package com.voipgrid.vialer.twostepcall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.voipgrid.vialer.R;

import androidx.core.content.ContextCompat;

/**
 * Created by eltjo on 12/10/15.
 */
public class TwoStepCallIconView extends View {

    private static final int DEFAULT_COLOR = Color.WHITE;

    public static final float BORDER_RADIUS = 30f;
    private static final float DX = 2.0f;
    private static final float DY = 2.0f;
    public static final float RADIUS = 27f;
    public static final float SHADOW_WIDTH = 3.0f;
    private static final int NOT_ENABLED_ALPHA = 127;

    private float mDensity;

    private int mBackgroundColor;
    private int mForegroundColor;
    private float mShadowRadius;
    private Bitmap mBitmap = null;
    private boolean mEnabled = true;

    public TwoStepCallIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;
        mShadowRadius = SHADOW_WIDTH * mDensity;
        mForegroundColor = DEFAULT_COLOR;
        mBackgroundColor = DEFAULT_COLOR;
        Paint paintBorder = new Paint();
        paintBorder.setColor(mBackgroundColor);
        this.setLayerType(LAYER_TYPE_SOFTWARE, paintBorder);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) ((BORDER_RADIUS * mDensity + mShadowRadius) * 2);
        int height = (int) ((BORDER_RADIUS * mDensity + mShadowRadius) * 2);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float borderRadius = BORDER_RADIUS * mDensity;
        float radius = RADIUS * mDensity;

        Paint border = new Paint();
        border.setAntiAlias(true);
        border.setColor(mBackgroundColor);
        border.setShadowLayer(mShadowRadius, DX, DY, ContextCompat.getColor(getContext(), R.color.two_step_call_step_shadow));
        canvas.drawCircle(borderRadius + mShadowRadius, borderRadius + mShadowRadius,
                borderRadius, border);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(mForegroundColor);
        if(!mEnabled) {
            paint.setAlpha(NOT_ENABLED_ALPHA);
        }
        canvas.drawCircle(borderRadius + mShadowRadius, borderRadius + mShadowRadius,
                radius, paint);

        if(mBitmap != null) {
            float width = mBitmap.getWidth();
            float left = mShadowRadius + borderRadius - width / 2;

            float height = mBitmap.getHeight();
            float top = mShadowRadius + borderRadius - height / 2;

            canvas.drawBitmap(mBitmap, left, top, null);
        }
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
    }

    public void setDrawable(int resource) {
        mBitmap = BitmapFactory.decodeResource(getResources(), resource);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        invalidate();
    }
}