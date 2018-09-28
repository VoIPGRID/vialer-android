package dialer;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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

    private Listener listener;

    @BindView(R.id.number_input_edit_text) NumberInputView mNumberInput;
    @BindView(R.id.key_pad_view) KeyPadView mKeypad;
    private Unbinder unbinder;

    public Dialer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.view_dialer, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        unbinder = ButterKnife.bind(this);
        mNumberInput.setOnInputChangedListener(this);
        mKeypad.setOnKeyPadClickListener(this);
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
    }

    public String getNumber() {
        return mNumberInput.getNumber();
    }

    @Override
    public void onInputChanged(String number) {
        listener.numberWasChanged(number);
    }

    public interface Listener {
        void numberWasChanged(String number);
    }
}
