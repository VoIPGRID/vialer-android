package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.sip.SipService;


/**
 * Custom KeyPadClass to extend the LinearLayout to show dial pad buttons.
 */
public class KeyPadView extends LinearLayout
        implements View.OnLongClickListener, DialpadButton.OnTouchListener {

    private OnKeyPadClickListener mListener;
    private ToneGenerator mToneGenerator;

    public KeyPadView(Context context) {
        super(context);
        init();
    }

    public KeyPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KeyPadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_RING, 100);
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        inflater.inflate(R.layout.view_key_pad, this);

        setUpButtonListeners(findViewById(R.id.dialpad_container));
    }

    /**
     * Recursively set up button listeners for all child DialpadButtons.
     *
     * @param container
     */
    private void setUpButtonListeners(ViewGroup container) {
        for (int i = 0; i < container.getChildCount(); i++) {

            View child = container.getChildAt(i);

            if (!(child instanceof ViewGroup)) {
                continue;
            }

            if (child instanceof DialpadButton) {
                DialpadButton dialpadButton = (DialpadButton) child;
                if (dialpadButton.getDigit().equals("0")) {
                    dialpadButton.setOnLongClickListener(this);
                }
                dialpadButton.setOnTouchEndListener(this);
                continue;
            }

            setUpButtonListeners((ViewGroup) child);
        }

    }

    /**
     * Whether the button should produce a tone when pressed.
     *
     * Will return true if in a call or when the 'Dial pad tones' setting is enabled or not found,
     * false otherwise.
     *
     * @return True if a tone should be produced, false otherwise.
     */
    private boolean shouldUseTone() {
        return SipService.sipServiceActive || Settings.System.getInt(
                getContext().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
    }

    public void setOnKeyPadClickListener(OnKeyPadClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onStartTouch(View view) {
        if(view instanceof DialpadButton) {
            DialpadButton button = (DialpadButton) view;

            if (shouldUseTone()) {
                mToneGenerator.startTone(button.getDtmfTone());
            }

            String digit = button.getDigit();

            if (mListener != null) {
                mListener.onKeyPadButtonClick(digit, button.getChars());
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if(view instanceof DialpadButton) {
            DialpadButton button = (DialpadButton) view;
            String digit = button.getDigit();
            if (mListener != null) {
                if (digit.equals("0")) {
                    mListener.onKeyPadButtonClick(button.getChars(), button.getChars());
                    mToneGenerator.stopTone();
                }
            }
        }

        return true;
    }



    @Override
    public void onEndTouch(View view) {
        if (view instanceof DialpadButton) {
            mToneGenerator.stopTone();
        }
    }

    public interface OnKeyPadClickListener {
        void onKeyPadButtonClick(String digit, String chars);
    }
}