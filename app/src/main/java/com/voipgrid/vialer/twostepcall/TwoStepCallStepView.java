package com.voipgrid.vialer.twostepcall;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.voipgrid.vialer.R;

/**
 * Created by eltjo on 12/10/15.
 */
public class TwoStepCallStepView extends RelativeLayout {

    private TextView mMessage;

    private TextView mNumber;

    private TwoStepCallIconView mIcon;

    private ImageView mIndicator;

    public TwoStepCallStepView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.two_step_call_step, this);
        mMessage = findViewById(R.id.message);
        mNumber = findViewById(R.id.number);
        mIcon = findViewById(R.id.icon);
        mIndicator = findViewById(R.id.connection_indicator);
    }

    public void setMessage(String message, int color) {
        mMessage.setText(message);
        mMessage.setTextColor(color);
    }

    public void setBackgroundColor(int color) {
        mIcon.setBackgroundColor(color);
        mIcon.invalidate();
    }

    public void setIndicator(Drawable background, Drawable icon) {
        mIndicator.setBackground(background);
        mIndicator.setImageDrawable(icon);
    }

    public void setIcon(int resource) {
        mIcon.setDrawable(resource);
    }

    public void setDescription(String text) {
        mMessage.setText(text);
    }

    public void setNumber(String number) {
        mNumber.setText(number);
    }

    public void setEnabled(boolean enabled) {
        mIcon.setEnabled(enabled);
        mMessage.setEnabled(enabled);
        mNumber.setEnabled(enabled);
    }
}
