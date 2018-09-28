package dialer;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.dialer.KeyPadView;
import com.voipgrid.vialer.dialer.NumberInputView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class Dialer extends LinearLayout implements KeyPadView.OnKeyPadClickListener, NumberInputView.OnInputChangedListener {

    private final boolean showRemoveButton;
    private Listener listener;

    @BindView(R.id.number_input_edit_text) NumberInputView mNumberInput;
    @BindView(R.id.key_pad_view) KeyPadView mKeypad;
    private Unbinder unbinder;
    private boolean showExitButton;

    public Dialer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.view_dialer, this);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Dialer);
        showExitButton = a.getBoolean(R.styleable.Dialer_show_exit_button, false);
        showRemoveButton = a.getBoolean(R.styleable.Dialer_show_remove_button, false);
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
        mNumberInput.add(digit);
        mNumberInput.setCorrectFontSize();
        listener.digitWasPressed(digit);
    }

    public String getNumber() {
        return mNumberInput.getNumber();
    }

    @Override
    public void onInputChanged(String number) {
        listener.numberWasChanged(number);
    }

    @Override
    public void exitButtonWasPressed() {
        listener.exitButtonWasPressed();
    }

    public interface Listener {
        void numberWasChanged(String number);
        void digitWasPressed(String digit);
        void exitButtonWasPressed();
    }
}
