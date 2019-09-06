package com.voipgrid.vialer;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * This is purely to display a toast message when a user clicks on a disabled setting.
 *
 */
public class InterceptingLinearLayout extends LinearLayout {

    public InterceptingLinearLayout(Context context) {
        super(context);
    }

    public InterceptingLinearLayout(Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptingLinearLayout(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public InterceptingLinearLayout(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        super.onInterceptTouchEvent(ev);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Toast.makeText(VialerApplication.get(), R.string.call_connection_setting_not_supported, Toast.LENGTH_LONG).show();
            return true;
        } else {
            return false;
        }
    }
}