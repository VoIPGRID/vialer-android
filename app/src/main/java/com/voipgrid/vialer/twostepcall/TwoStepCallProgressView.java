package com.voipgrid.vialer.twostepcall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.voipgrid.vialer.R;

import androidx.core.content.ContextCompat;

/**
 * Created by eltjo on 12/10/15.
 */
public class TwoStepCallProgressView extends View {

    public static final int STATE_SUCCESS = 1;

    private static final long ANIMATION_DELAY = 150;
    private static final float DOT_RADIUS = 4.5f;
    private static final float DOT_ACTIVE_RADIUS = 6f;
    private static final float DOT_STATE_RADIUS = 10f;
    private static final float HEIGHT = 90f;
    private static final int NOT_ENABLED_ALPHA = 127;

    private float mDensity;

    private float mRadius;
    private float mActiveRadius;
    private float mStateRadius;
    private float mHeight;
    private boolean mInProgress;

    private Handler mHandler;
    private Runnable mRunnable;

    private int mProgressDotIndex = 0;
    private int mState = 0;

    private Bitmap mBitmapSuccess, mBitmapFailed;
    private boolean mEnabled = true;

    private Paint mActivePaint;
    private Paint mPaint;
    private float mPosition;
    private float mStep;

    public TwoStepCallProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;
        mRadius = DOT_RADIUS * mDensity;
        mActiveRadius = DOT_ACTIVE_RADIUS * mDensity;
        mStateRadius = DOT_STATE_RADIUS * mDensity;
        mHeight = HEIGHT * mDensity;

        mHandler = new Handler();
        mRunnable = () -> {
            mProgressDotIndex ++;
            if(mProgressDotIndex == 4) {
                mProgressDotIndex = 0;
            }
            invalidate();
            if(mInProgress) {
                mHandler.postDelayed(mRunnable, ANIMATION_DELAY);
            }
        };

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.two_step_call_dots));

        mActivePaint = new Paint();
        mActivePaint.setAntiAlias(true);
        mActivePaint.setColor(ContextCompat.getColor(getContext(), R.color.two_step_call_dot_selected));

        mPosition = (TwoStepCallIconView.BORDER_RADIUS * mDensity) +
                (TwoStepCallIconView.SHADOW_WIDTH * mDensity);

        mStep = mHeight / 4;

        mBitmapSuccess = BitmapFactory.decodeResource(
                getResources(), R.drawable.ic_twostepcall_check);
        mBitmapFailed = BitmapFactory.decodeResource(
                getResources(), R.drawable.ic_twostepcall_failed);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) ((TwoStepCallIconView.BORDER_RADIUS * mDensity +
                TwoStepCallIconView.SHADOW_WIDTH * mDensity) * 2);
        setMeasuredDimension(width, (int) mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!mEnabled) {
            mPaint.setAlpha(NOT_ENABLED_ALPHA);
        }

        for(int i=1; i<4; i++) {
            if(i == 2 && mState > 0) {
                canvas.drawCircle(mPosition,
                        (mRadius + mStep * i) - mRadius,
                        mStateRadius,
                        mPaint);
                Bitmap bitmap = mState == STATE_SUCCESS ? mBitmapSuccess : mBitmapFailed;
                int height = bitmap.getHeight();
                int width = bitmap.getWidth();
                canvas.drawBitmap(bitmap, mPosition - width / 2,
                        (mRadius + mStep * i) - mRadius - height / 2, null);
            } else {
                canvas.drawCircle(mPosition,
                        (mRadius + mStep * i) - mRadius,
                        mProgressDotIndex == i ? mActiveRadius : mRadius,
                        mProgressDotIndex == i ? mActivePaint : mPaint);
            }
        }
    }

    public void startProgress() {
        if(!mInProgress) {
            mHandler.postDelayed(mRunnable, ANIMATION_DELAY);
        }
        mInProgress = true;
    }

    public void stopProgress() {
        mProgressDotIndex = -1;
        mInProgress = false;
    }

    public void setState(int state) {
        mState = state;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        invalidate();
    }
}
