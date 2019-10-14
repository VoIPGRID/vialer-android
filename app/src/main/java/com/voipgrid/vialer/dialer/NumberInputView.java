package com.voipgrid.vialer.dialer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.voipgrid.vialer.ActivityLifecycleTracker;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.User;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.SecureCalling;

import javax.inject.Inject;

import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Custom NumberInput for the dialer.
 */
public class NumberInputView extends RelativeLayout implements
        View.OnClickListener,
        View.OnLongClickListener,
        TextWatcher {

    @BindView(R.id.edit_text) EditText mNumberInputEditText;
    @BindView(R.id.remove_button) ImageButton mRemoveButton;
    @BindView(R.id.exit_button) ImageButton mExitButton;
    @BindView(R.id.encryption_disabled) ImageButton encryptionDisabledWarning;

    private OnInputChangedListener mListener;
    private SecureCalling secureCalling;

    public NumberInputView(Context context) {
        super(context);
        init();
    }

    public NumberInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NumberInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        secureCalling = SecureCalling.fromContext(getContext());
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_number_input, this);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);

        // Find the number input field and add a TextChangedListener to handle text changes.
        mNumberInputEditText.addTextChangedListener(this);
        mNumberInputEditText.setOnClickListener(this);
        mNumberInputEditText.setOnLongClickListener(this);

        // Find the remove button and add an OnClickListener.
        mRemoveButton.setOnClickListener(this);
        mRemoveButton.setOnLongClickListener(this);

        resetTextToDefaultSize();

        mExitButton.setOnClickListener(v -> {
            mListener.exitButtonWasPressed();
        });

        encryptionDisabledWarning.setVisibility(secureCalling.isEnabled() ? INVISIBLE : VISIBLE);
    }

    private void resetTextToDefaultSize() {
        mNumberInputEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getResources().getDimension(R.dimen.dialpad_number_input_text_size));
    }

    public void setOnInputChangedListener(OnInputChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            // Remove character from input.
            case R.id.remove_button:
                remove();
                break;
            // Set cursor in edit text.
            case R.id.edit_text:
                if (!isEmpty()) {
                    mNumberInputEditText.setCursorVisible(true);
                }
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            // Clear all chars from input.
            case R.id.remove_button:
                clear();
                return true;
            case R.id.edit_text:
                // Right now EditText does not show the "paste" option when cursor is not visible.
                // To show that, make the cursor visible, and return false, letting the EditText
                // show the option by itself.
                mNumberInputEditText.setCursorVisible(true);
                return false;
        }
        return false;
    }

    @OnClick(R.id.encryption_disabled)
    public void encryptionDisabledWarningWasClicked() {

        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    encryptionDisabledWarning.setClickable(false);

                    secureCalling.enable(new SecureCalling.Callback() {
                        @Override
                        public void onSuccess() {
                            User.voip.setHasTlsEnabled(true);
                            encryptionDisabledWarning.setImageResource(R.drawable.ic_lock);
                            encryptionDisabledWarning.setColorFilter(ContextCompat.getColor(getContext(), R.color.color_primary));
                            ActivityLifecycleTracker.removeEncryptionNotification();
                        }

                        @Override
                        public void onFail() {
                            encryptionDisabledWarning.setClickable(true);
                            Toast.makeText(getContext(), R.string.dialer_enable_encryption_failed, Toast.LENGTH_LONG).show();
                        }
                    });
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        new AlertDialog.Builder(getContext())
                .setMessage(R.string.dialer_no_encryption_dialog)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //not used
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() >= 0 && mListener != null) {
            mListener.onInputChanged(s.toString());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        //not used
    }

    /**
     * Remove character from input field
     */
    public void remove() {
        // Check if there is actually text in the input field.
        if (mNumberInputEditText.length() > 0) {
            mRemoveButton.animate();
            Integer startCursorPosition = mNumberInputEditText.getSelectionStart();
            Integer endCursorPosition = mNumberInputEditText.getSelectionEnd();

            // Can not remove on index 0.
            if (startCursorPosition == 0 && endCursorPosition == 0) {
                return;
            }

            // Check if there is an selection to remove. Otherwise remove one character.
            if ((endCursorPosition - startCursorPosition) > 0) {
                removeTextFromInput(startCursorPosition, endCursorPosition);
            } else {
                removeTextFromInput(startCursorPosition - 1, endCursorPosition);
            }
        }
        setCorrectFontSize();
    }

    public void setCorrectFontSize() {
        int charCount = mNumberInputEditText.getText().length();

        if (charCount == 0) {
            resetTextToDefaultSize();
            return;
        }

        float charSize = getTextSize();

        int inputMaxScalingLength = getResources().getInteger(R.integer.dialpad_number_input_max_scaling_length);
        int maxLengthNormalPhoneNumber = getResources().getInteger(R.integer.dialpad_normal_phonenumber_length_max);

        if (charCount > maxLengthNormalPhoneNumber && charCount < inputMaxScalingLength) {
            charSize = charSize / 1.04f;
        }

        mNumberInputEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, charSize);
    }

    /**
     * Get the actual text size. Normal getTextSize returns the size in pixels based on screen
     * density.
     *
     * @return float the textSize of the input field
     */
    private float getTextSize() {
        return mNumberInputEditText.getTextSize() / getResources().getDisplayMetrics().density;
    }

    /**
     * Add text to the number input field
     *
     * @param text
     */
    public void add(CharSequence text) {
        Editable inputText = mNumberInputEditText.getText();
        Integer startCursorPosition = mNumberInputEditText.getSelectionStart();
        Integer endCursorPosition = mNumberInputEditText.getSelectionEnd();

        removeTextFromInput(startCursorPosition, endCursorPosition);

        inputText.insert(startCursorPosition, text);
    }

    private void removeTextFromInput(Integer startCursorPosition, Integer endCursorPosition) {
        // If there is a selection active delete the selected text.
        if ((endCursorPosition - startCursorPosition) > 0) {
            mNumberInputEditText.getText().delete(startCursorPosition, endCursorPosition);
        }

        if (startCursorPosition == mNumberInputEditText.length()) {
            mNumberInputEditText.setCursorVisible(false);
        } else {
            mNumberInputEditText.setCursorVisible(true);
        }
    }

    /**
     * Clear the number input field
     */
    public void clear() {
        mNumberInputEditText.setCursorVisible(false);
        mNumberInputEditText.getText().clear();
        setCorrectFontSize();
    }

    public void setNumber(String number) {
        mNumberInputEditText.setText(number);
        mNumberInputEditText.setSelection(number.length());
    }

    /**
     * Return the number input as String
     *
     * @return String representation of the number
     */
    public String getNumber() {
        return mNumberInputEditText.getText().toString();
    }

    public void enableRemoveButton() {
        mRemoveButton.setVisibility(VISIBLE);
    }

    public interface OnInputChangedListener {
        void onInputChanged(String number);
        void exitButtonWasPressed();
    }

    public boolean isEmpty() {
        return mNumberInputEditText.length() == 0;
    }

    public void enableExitButton() {
        mExitButton.setVisibility(View.VISIBLE);
    }
}
