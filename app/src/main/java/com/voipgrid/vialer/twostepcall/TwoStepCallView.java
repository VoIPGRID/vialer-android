package com.voipgrid.vialer.twostepcall;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;

/**
 * Class that contains a view with information about a two step call.
 */
public class TwoStepCallView extends LinearLayout {
    private TwoStepCallConnectionView mConnectionCallA, mConnectionCallB;
    private TwoStepCallStepView mStepPlatform, mStepCallA, mStepCallB;

    /**
     * Constructor.
     * @param context
     * @param attrs
     */
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
        setState(TwoStepCallUtils.STATE_INITIAL);
    }

    private TwoStepCallStepView addStep(Context context, int color, int icon, String description) {
        TwoStepCallStepView step = new TwoStepCallStepView(context);
        step.setColor(color);
        step.setIcon(icon);
        step.setDescription(description);
        addView(step);
        return step;
    }

    private TwoStepCallConnectionView addConnection(Context context) {
        TwoStepCallConnectionView connection = new TwoStepCallConnectionView(context);
        addView(connection);
        return connection;
    }

    /**
     * Set outgoing number.
     * @param number Outgoing CLI.
     */
    public void setOutgoingNumber(String number) {
        mStepPlatform.setNumber(number);
    }

    /**
     * Set the a number.
     * @param number The users own number.
     */
    public void setNumberA(String number) {
        mStepCallA.setNumber(number);
    }

    /**
     * Set the b number.
     * @param number The number being called.
     */
    public void setNumberB(String number) {
        mStepCallB.setNumber(number);
    }

    /**
     * Set the state and act accordingly to the new state.
     * @param state Found in TwoStepCallUtils.
     */
    public void setState(String state) {
        if(state != null) {
            switch (state) {
                case TwoStepCallUtils.STATE_INITIAL:
                    initialize();
                    break;
                case TwoStepCallUtils.STATE_CALLING_A:
                    callingA();
                    break;
                case TwoStepCallUtils.STATE_CALLING_B:
                    callingB();
                    break;
                case TwoStepCallUtils.STATE_FAILED_A:
                    failedA();
                    break;
                case TwoStepCallUtils.STATE_FAILED_B:
                    failedB();
                    break;
                case TwoStepCallUtils.STATE_CONNECTED:
                    connected();
                    break;
                case TwoStepCallUtils.STATE_DISCONNECTED:
                    disconnected();
                    break;
                case TwoStepCallUtils.STATE_CANCELLING:
                    cancelling();
                    break;
                case TwoStepCallUtils.STATE_CANCELLED:
                    cancelled();
                    break;
            }
        }
    }

    /**
     * Setup all UI elements.
     */
    private void initialize() {
        mStepPlatform.setEnabled(true);
        mConnectionCallA.setEnabled(false);
        mConnectionCallA.stopProgress();
        mStepCallA.setEnabled(false);
        mConnectionCallB.setEnabled(false);
        mConnectionCallB.stopProgress();
        mStepCallB.setEnabled(false);
    }

    /**
     * Start calling a number animations.
     */
    private void callingA() {
        mConnectionCallA.setEnabled(true);
        mConnectionCallA.startProgress();
        mStepCallA.setEnabled(true);
    }

    /**
     * Start calling b number animations.
     */
    private void callingB() {
        mConnectionCallA.stopProgress();
        mConnectionCallA.setState(TwoStepCallProgressView.STATE_SUCCESS);
        mConnectionCallB.setEnabled(true);
        mConnectionCallB.startProgress();
        mStepCallB.setEnabled(true);
    }

    /**
     * Update view to reflect failed a number call.
     */
    private void failedA() {
        mConnectionCallA.stopProgress();
        mConnectionCallA.setState(TwoStepCallProgressView.STATE_FAILED);
        mConnectionCallA.setMessage(getResources().getString(R.string.two_step_call_message_step_a_failed));
    }

    /**
     * Update view to reflect failed b number call.
     */
    private void failedB() {
        mConnectionCallB.stopProgress();
        mConnectionCallB.setState(TwoStepCallProgressView.STATE_FAILED);
        mConnectionCallB.setMessage(getResources().getString(R.string.two_step_call_message_step_b_failed));
    }

    /**
     * Update the view to show in progress two way call.
     */
    private void connected() {
        mConnectionCallB.stopProgress();
        mConnectionCallB.setState(TwoStepCallProgressView.STATE_SUCCESS);
    }

    /**
     * Update the view to show a ended two way call.
     */
    private void disconnected() {
        mConnectionCallA.stopProgress();
        mConnectionCallB.stopProgress();
    }

    /**
     * Update the view to show a two way call being cancelled.
     */
    private void cancelling() {
        mConnectionCallA.stopProgress();
        mConnectionCallB.stopProgress();
    }

    /**
     * Update the view to show that a two way call is cancelled.
     */
    private void cancelled() {
        mConnectionCallA.setEnabled(true);
        mConnectionCallA.stopProgress();
        mConnectionCallA.setState(TwoStepCallProgressView.STATE_FAILED);

        mConnectionCallB.setEnabled(true);
        mConnectionCallB.stopProgress();
        mConnectionCallB.setState(TwoStepCallProgressView.STATE_FAILED);
    }
}
