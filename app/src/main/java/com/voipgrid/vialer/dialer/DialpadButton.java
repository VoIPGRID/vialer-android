package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.voipgrid.vialer.R;

/**
 * Custom View widget used in the apps dialpad
 */
public class DialpadButton extends LinearLayout
        implements View.OnTouchListener, View.OnClickListener {

    private String mChars;
    private String mDigit;
    private int mTone;

    private OnTouchListener mOnTouchListener;

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public DialpadButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public DialpadButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Initialize the view widget width TextViews to show a digit and characters.
     * TextViews are wrapped in several parent views to display them in a GridLayout and add a
     * decent ripple effect.
     *
     * @param context
     * @param attrs
     */
    private void init(Context context, AttributeSet attrs) {
        // Populate the parameter values supplied in the layout.
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.DialpadButton, 0, 0);
        try {
            mDigit = a.getString(R.styleable.DialpadButton_digit);
            mChars = a.getString(R.styleable.DialpadButton_chars);
        } finally {
            a.recycle();
        }

        setSoundEffectsEnabled(false);

        setDtmfTone(mDigit);

        setOnTouchListener(this);

        // Container layout to fix the button is centered in the GridLayout element.
        LinearLayout container = new LinearLayout(context);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setGravity(Gravity.CENTER);
        addView(container);

        // Square layout to fix a round background on touch events for pre Lollipop devices.
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(VERTICAL);
        linearLayout.setBackgroundResource(R.drawable.bg_dial_button);
        linearLayout.setGravity(Gravity.CENTER);
        container.addView(linearLayout);

        // LayoutParams used to center the text within the TextViews.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Setup and add the digit TextView.
        TextView digit = new TextView(context);
        digit.setText(mDigit);
        digit.setLayoutParams(params);
        digit.setGravity(Gravity.CENTER);
        if(mDigit.equals("*") || mDigit.equals("#")) {
            digit.setTextColor(getResources().getColor(R.color.dial_button_chars_color));
            digit.setTextSize(getResources().getDimension(R.dimen.dialpad_button_star_text_size));
        } else {
            digit.setTextColor(getResources().getColor(R.color.dial_button_digit_color));
            digit.setTextSize(getResources().getDimension(R.dimen.dialpad_button_digit_text_size));
        }

        linearLayout.addView(digit);

        // Setup and add the chars TextView.
        TextView chars = new TextView(context);
        chars.setText(mChars);
        chars.setLayoutParams(params);
        chars.setGravity(Gravity.CENTER);
        chars.setTextColor(getResources().getColor(R.color.dial_button_chars_color));
        chars.setTextSize(getResources().getDimension(R.dimen.dialpad_button_chars_text_size));
        chars.setAllCaps(getResources().getBoolean(R.bool.dialpad_button_chars_all_caps));
        linearLayout.addView(chars);
    }

    /**
     * Get the OnTouchListener, called when the press
     * has started or ended (no matter how short or long).
     * @return The OnTouchListener.
     */
    public OnTouchListener getOnTouchEndListener() {
        return mOnTouchListener;
    }

    /**
     * Set the OnTouchEndListener, called when the press (no matter how short or long)
     * has ended.
     */
    public void setOnTouchEndListener(@Nullable OnTouchListener listener) {
        mOnTouchListener = listener;
    }

    /**
     * Returns the buttons digit value
     *
     * @return String value of the buttons digit
     */
    public String getDigit() {
        return mDigit;
    }

    /**
     * Returns the buttons characters
     *
     * @return String value of the buttons characters
     */
    public String getChars() {
        return mChars;
    }

    public int getDtmfTone() {
        return mTone;
    }

    private void setDtmfTone(String digit) {
        switch (digit) {
            case "0" : mTone = ToneGenerator.Constants.TONE_DTMF_0; break;
            case "1" : mTone = ToneGenerator.Constants.TONE_DTMF_1; break;
            case "2" : mTone = ToneGenerator.Constants.TONE_DTMF_2; break;
            case "3" : mTone = ToneGenerator.Constants.TONE_DTMF_3; break;
            case "4" : mTone = ToneGenerator.Constants.TONE_DTMF_4; break;
            case "5" : mTone = ToneGenerator.Constants.TONE_DTMF_5; break;
            case "6" : mTone = ToneGenerator.Constants.TONE_DTMF_6; break;
            case "7" : mTone = ToneGenerator.Constants.TONE_DTMF_7; break;
            case "8" : mTone = ToneGenerator.Constants.TONE_DTMF_8; break;
            case "9" : mTone = ToneGenerator.Constants.TONE_DTMF_9; break;
            case "#" : mTone = ToneGenerator.Constants.TONE_DTMF_P; break;
            case "*" : mTone = ToneGenerator.Constants.TONE_DTMF_S; break;
            default : mTone = -1;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mOnTouchListener.onStartTouch(view);
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mOnTouchListener.onEndTouch(view);
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        mOnTouchListener.onEndTouch(view);
    }

    interface OnTouchListener {
        /**
         * Called when the touch has start.
         * @param view The view which touch has ended.
         */
        void onStartTouch(View view);

        /**
         * Called when the touch has ended.
         * @param view The view which touch has ended.
         */
        void onEndTouch(View view);
    }
}
