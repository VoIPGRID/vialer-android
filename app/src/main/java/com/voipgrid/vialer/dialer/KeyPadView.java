package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;


/**
 * Custom KeyPadClass to extend the LinearLayout to show dial pad buttons.
 */
public class KeyPadView extends LinearLayout
        implements View.OnClickListener, View.OnLongClickListener {

    private static final int DTMF_TONE_DURATION = 200;

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
            AudioManager audioManager = (AudioManager) getContext().getSystemService(
                    Context.AUDIO_SERVICE
            );
            mToneGenerator = new ToneGenerator(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            );
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        );
        inflater.inflate(R.layout.view_key_pad, this);
        ViewGroup view = (ViewGroup) findViewById(R.id.grid_layout);

        for(int i = 0, size = view.getChildCount(); i < size; i++) {
            View child = view.getChildAt(i);
            if(child instanceof DialpadButton) {
                String digit = ((DialpadButton) child).getDigit();
                if (digit.equals("0")) {
                    child.setOnLongClickListener(this);
                }
                child.setOnClickListener(this);
            }
        }
    }

    public void setOnKeyPadClickListener(OnKeyPadClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View view) {
        if(view instanceof DialpadButton) {
            DialpadButton button = (DialpadButton) view;
            mToneGenerator.startTone(button.getDtmfTone(), DTMF_TONE_DURATION);
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
                }
            }
        }
        return true;
    }

    public interface OnKeyPadClickListener {
        void onKeyPadButtonClick(String digit, String chars);
    }
}
