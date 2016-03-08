package com.voipgrid.vialer.twostepcall;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.voipgrid.vialer.R;

/**
 * Created by eltjo on 12/10/15.
 */
public class TwoStepCallConnectionView extends RelativeLayout {

    private TextView mMessage;

    private TwoStepCallProgressView mProgress;

    public TwoStepCallConnectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TwoStepCallConnectionView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.two_step_call_connection, this);
        mMessage = (TextView) findViewById(R.id.message);
        mProgress = (TwoStepCallProgressView) findViewById(R.id.progress);
    }

    public void setMessage(String text) {
        mMessage.setText(text);
    }

    public void startProgress() {
        mProgress.startProgress();
    }

    public void stopProgress() {
        mProgress.stopProgress();
    }

    public void setState(int state) {
        mProgress.setState(state);
    }

    public void setEnabled(boolean enabled) {
        mProgress.setEnabled(enabled);
    }
}
