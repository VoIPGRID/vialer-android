package com.voipgrid.vialer.twostepcall.widget;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;

/**
 * Created by eltjo on 15/10/15.
 */
public class TwoStepCallView extends LinearLayout {

    public static final String STATE_INITIAL = "initial";
    public static final String STATE_CALLING_A = "calling-a";
    public static final String STATE_CALLING_B = "calling-b";
    public static final String STATE_FAILED_A = "failed-a";
    public static final String STATE_FAILED_B = "failed-b";
    public static final String STATE_CONNECTED = "connected";
    public static final String STATE_DISCONNECTED = "disconnected";

    private TwoStepCallStepView mStepPlatform, mStepCallA, mStepCallB;

    private TwoStepCallConnectionView mConnectionCallA, mConnectionCallB;

    public TwoStepCallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mStepPlatform = addStep(
                context,
                ContextCompat.getColor(context, R.color.two_step_call_step_platform),
                R.drawable.ic_twostepcall_platform,
                getResources().getString(R.string.two_step_call_description_platform));

        mConnectionCallA = addConnection(context);

        mStepCallA = addStep(
                context,
                ContextCompat.getColor(context, R.color.two_step_call_step_a),
                R.drawable.ic_twostepcall_calling,
                getResources().getString(R.string.two_step_call_description_step_a));

        mConnectionCallB = addConnection(context);

        mStepCallB = addStep(
                context,
                ContextCompat.getColor(context, R.color.two_step_call_step_b),
                R.drawable.ic_twostepcall_calling,
                getResources().getString(R.string.two_step_call_description_step_b));

        setOrientation(VERTICAL);
        setState(STATE_INITIAL);
    }

    private TwoStepCallStepView addStep(Context context, int color, int icon, String desciption) {
        TwoStepCallStepView step = new TwoStepCallStepView(context);
        step.setColor(color);
        step.setIcon(icon);
        step.setDescription(desciption);
        addView(step);
        return step;
    }

    private TwoStepCallConnectionView addConnection(Context context) {
        TwoStepCallConnectionView connection = new TwoStepCallConnectionView(context);
        addView(connection);
        return connection;
    }

    public void setOutgoingNumber(String number) {
        mStepPlatform.setNumber(number);
    }

    public void setNumberA(String number) {
        mStepCallA.setNumber(number);
    }

    public void setNumberB(String number) {
        mStepCallB.setNumber(number);
    }

    public void setState(String state) {
        if(state != null) {
            switch (state) {
                case STATE_INITIAL:
                    initialize();
                    break;
                case STATE_CALLING_A:
                    callingA();
                    break;
                case STATE_CALLING_B:
                    callingB();
                    break;
                case STATE_FAILED_A:
                    failedA();
                    break;
                case STATE_FAILED_B:
                    failedB();
                    break;
                case STATE_CONNECTED:
                    connected();
                    break;
                case STATE_DISCONNECTED:
                    disconnected();
                    break;
            }
        }
    }



    private void initialize() {
        mStepPlatform.setEnabled(true);
        mConnectionCallA.setEnabled(false);
        mConnectionCallA.stopProgress();
        mStepCallA.setEnabled(false);
        mConnectionCallB.setEnabled(false);
        mConnectionCallB.stopProgress();
        mStepCallB.setEnabled(false);
    }

    private void callingA() {
        mConnectionCallA.setEnabled(true);
        mConnectionCallA.startProgress();
        mStepCallA.setEnabled(true);
    }

    private void callingB() {
        mConnectionCallA.stopProgress();
        mConnectionCallA.setState(TwoStepCallProgressView.STATE_SUCCESS);
        mConnectionCallB.setEnabled(true);
        mConnectionCallB.startProgress();
        mStepCallB.setEnabled(true);
    }

    private void failedA() {
        mConnectionCallA.stopProgress();
        mConnectionCallA.setState(TwoStepCallProgressView.STATE_FAILED);
        mConnectionCallA.setMessage(getResources().getString(R.string.two_step_call_message_step_a_failed));
    }

    private void failedB() {
        mConnectionCallB.stopProgress();
        mConnectionCallB.setState(TwoStepCallProgressView.STATE_FAILED);
        mConnectionCallB.setMessage(getResources().getString(R.string.two_step_call_message_step_b_failed));
    }

    private void connected() {
        mConnectionCallB.stopProgress();
        mConnectionCallB.setState(TwoStepCallProgressView.STATE_SUCCESS);
    }

    private void disconnected() {
        mConnectionCallA.stopProgress();
        mConnectionCallB.stopProgress();
    }
}
