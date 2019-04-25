package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class ContactsProgressBar extends ProgressBar {
    public ContactsProgressBar(Context context) {
        super(context);
    }

    public ContactsProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactsProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ContactsProgressBar(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(VialerApplication.get(), R.color.color_primary), PorterDuff.Mode.SRC_ATOP);
    }
}
