package com.voipgrid.vialer.twostepcall.widget;

import android.content.Context;
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

    public TwoStepCallStepView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.two_step_call_step, this);
        mMessage = (TextView) findViewById(R.id.message);
        mNumber = (TextView) findViewById(R.id.number);
        mIcon = (TwoStepCallIconView) findViewById(R.id.icon);
    }

    public void setColor(int color) {
        mIcon.setColor(color);
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
