package com.voipgrid.vialer.calling;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.CallButton;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;

import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class Dialer extends LinearLayout implements KeyPadView.OnKeyPadClickListener, NumberInputView.OnInputChangedListener {

    private Listener listener;

    @BindView(R.id.bottom) ViewGroup holder;
    @BindView(R.id.number_input_edit_text) NumberInputView mNumberInput;
    @BindView(R.id.key_pad_view) KeyPadView mKeypad;
    @BindView(R.id.button_call) CallButton mCallButton;

    private Unbinder unbinder;
    private final boolean showExitButton;
    private final boolean showCallButton;
    private final boolean showRemoveButton;
    private final float keypadHeightPercentage;

    public Dialer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.view_dialer, this);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Dialer);
        showExitButton = a.getBoolean(R.styleable.Dialer_show_exit_button, false);
        showRemoveButton = a.getBoolean(R.styleable.Dialer_show_remove_button, false);
        showCallButton = a.getBoolean(R.styleable.Dialer_show_call_button, true);
        keypadHeightPercentage = a.getFloat(R.styleable.Dialer_keypad_height_percentage, 0.6f);
    }

    public Dialer(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        this(context, attrs);
    }

    public Dialer(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        this(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        unbinder = ButterKnife.bind(this);
        mNumberInput.setOnInputChangedListener(this);
        mKeypad.setOnKeyPadClickListener(this);
        if (showExitButton) {
            mNumberInput.enableExitButton();
        }
        if (showRemoveButton) {
            mNumberInput.enableRemoveButton();
        }
        if (!showCallButton) {
            mCallButton.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbinder.unbind();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setNumber(String number) {
        mNumberInput.setNumber(number);
    }

    @Override
    public void onKeyPadButtonClick(String digit, String chars) {
        if (isFadedOut()) return;

        if (digit.equals("+")) {
            mNumberInput.replaceLast(digit);
        } else {
            mNumberInput.add(digit);
        }

        mNumberInput.setCorrectFontSize();

        if (listener != null) {
            listener.digitWasPressed(digit);
        }
    }

    public String getNumber() {
        return mNumberInput.getNumber();
    }

    /**
     * Determine if the keypad is faded out or not based on
     * its alpha, if it is faded out it should probably not
     * accept input.
     *
     * @return TRUE if faded out, FALSE otherwise.
     */
    private boolean isFadedOut() {
        return mKeypad.getAlpha() < 1;
    }

    @Override
    public void onInputChanged(String number) {
        if (listener == null) return;

        listener.numberWasChanged(number);
    }

    @Override
    public void exitButtonWasPressed() {
        listener.exitButtonWasPressed();
    }

    /**
     * Reverse the fade out method..
     *
     */
    public void fadeIn() {
        setKeypadAlpha(1);
        mCallButton.enable();
    }

    /**
     * Fade the entire dialer out, making it transparent
     * and stopping user interaction.
     *
     */
    public void fadeOut() {
        setKeypadAlpha(0.3f);
        mCallButton.disable();
    }

    /**
     * Set the alpha of the keypad while avoiding NPE.
     *
     * @param alpha
     */
    private void setKeypadAlpha(float alpha) {
        if (mKeypad == null) return;

        mKeypad.setAlpha(alpha);
    }

    public interface Listener {
        void numberWasChanged(String number);
        void digitWasPressed(String digit);
        void exitButtonWasPressed();
    }
}
