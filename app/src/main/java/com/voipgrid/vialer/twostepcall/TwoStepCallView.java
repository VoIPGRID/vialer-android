package com.voipgrid.vialer.twostepcall;

import android.content.Context;
import androidx.core.content.ContextCompat;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;

/**
 * Class that contains a view with information about a two step call.
 */
public class TwoStepCallView extends LinearLayout {
    private int colorConnected;
    private int colorFailed;
    private Drawable backgroundConnected;
    private Drawable backgroundFailed;
    private Drawable iconConnected;
    private Drawable iconFailed;

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
        colorConnected = ContextCompat.getColor(getContext(), R.color.dialpad_fab_call_color);
        colorFailed = ContextCompat.getColor(getContext(), R.color.error_foreground_color);
        backgroundConnected = ContextCompat.getDrawable(getContext(), R.drawable.twostepcall_connected_rounded_edges);
        backgroundFailed = ContextCompat.getDrawable(getContext(), R.drawable.twostepcall_failed_rounded_edges);
        iconConnected = ContextCompat.getDrawable(getContext(), R.drawable.ic_twostepcall_check);
        iconFailed = ContextCompat.getDrawable(getContext(), R.drawable.ic_twostepcall_failed);

        mStepPlatform = addStep(
                context,
                R.drawable.ic_twostepcall_platform,
                getResources().getString(R.string.two_step_call_description_platform));

        mConnectionCallA = addConnection(context);

        mStepCallA = addStep(
                context,
                R.drawable.ic_call,
                getResources().getString(R.string.two_step_call_description_step_a));

        mConnectionCallB = addConnection(context);

        mStepCallB = addStep(
                context,
                R.drawable.ic_call,
                getResources().getString(R.string.two_step_call_description_step_b));

        setOrientation(VERTICAL);
        setState(TwoStepCallUtils.STATE_INITIAL);
    }

    private TwoStepCallStepView addStep(Context context, int icon, String description) {
        TwoStepCallStepView step = new TwoStepCallStepView(context);
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
                case TwoStepCallUtils.STATE_FAILED:
                    failed();
                    break;
                case TwoStepCallUtils.STATE_INVALID_NUMBER:
                    invalidNumber();
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
        mStepPlatform.setBackgroundColor(colorConnected);
        mStepPlatform.setIndicator(backgroundConnected, iconConnected);
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
        mStepCallA.setBackgroundColor(colorConnected);
        mStepCallA.setIndicator(backgroundConnected, iconConnected);
    }

    /**
     * Update view to reflect failed a number call.
     */
    private void failedA() {
        mConnectionCallA.stopProgress();
        mStepCallA.setMessage(getResources().getString(R.string.two_step_call_message_step_a_failed), colorFailed);
        mStepCallA.setBackgroundColor(colorFailed);
        mStepCallA.setIndicator(backgroundFailed, iconFailed);
    }

    /**
     * Update view to reflect failed b number call.
     */
    private void failedB() {
        mConnectionCallB.stopProgress();
        mStepCallB.setMessage(getResources().getString(R.string.two_step_call_message_step_b_failed), colorFailed);
        mStepCallB.setBackgroundColor(colorFailed);
        mStepCallB.setIndicator(backgroundFailed, iconFailed);
    }

    /**
     * Update the view to show in progress two way call.
     */
    private void connected() {
        mConnectionCallB.stopProgress();
        mStepCallB.setBackgroundColor(colorConnected);
        mStepCallB.setIndicator(backgroundConnected, iconConnected);
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
        mStepCallA.setEnabled(true);
        mStepCallA.setBackgroundColor(colorFailed);
        mStepCallA.setIndicator(backgroundFailed, iconFailed);

        mConnectionCallB.setEnabled(true);
        mConnectionCallB.stopProgress();
        mStepCallB.setEnabled(true);
        mStepCallB.setBackgroundColor(colorFailed);
        mStepCallB.setIndicator(backgroundFailed, iconFailed);
    }

    /**
     * Update the view to show a failed attempt on the api call.
     */
    private void failed() {
        cancelled();
    }

    /**
     * Update the view to show a failed attempt because of a invalid number.
     */
    private void invalidNumber() {
        cancelled();
    }
}
