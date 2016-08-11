package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.voipgrid.vialer.R;

/**
 * Created by eltjo on 26/08/15.
 */
public class NumberInputView extends RelativeLayout implements
        View.OnClickListener,
        View.OnLongClickListener,
        TextWatcher {

    private EditText mEditText;
    private ImageButton mRemoveButton;
    private OnInputChangedListener mListener;
    private final int mNormalPhoneNumberLengthMax = 13;

    private float mDefaultTextsize;

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
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_number_input, this);

        // Find the number input field and add a TextChangedListener to handle text changes.
        mEditText = (EditText) findViewById(R.id.edit_text);
        mEditText.addTextChangedListener(this);
        mEditText.setOnClickListener(this);
        mEditText.setOnLongClickListener(this);

        // Find the remove button and add an OnClickListener.
        mRemoveButton = (ImageButton) findViewById(R.id.remove_button);
        mRemoveButton.setOnClickListener(this);
        mRemoveButton.setOnLongClickListener(this);

        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getResources().getDimension(R.dimen.dialpad_number_input_text_size));
        mDefaultTextsize = mEditText.getTextSize();
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
                    mEditText.setCursorVisible(true);
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
                mEditText.setCursorVisible(true);
                return false;
        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //not used
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() >= 0 && mListener != null) {
            mListener.onInputChanged(s.toString());
            mRemoveButton.setVisibility(View.VISIBLE);
        } else {
            mRemoveButton.setVisibility(View.INVISIBLE);
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
        if (mEditText.length() > 0) {
            mRemoveButton.animate();
            Integer startCursorPosition = mEditText.getSelectionStart();
            Integer endCursorPosition = mEditText.getSelectionEnd();

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
        int charCount = mEditText.getText().length();
        float charSize = mDefaultTextsize;

        if (charCount > mNormalPhoneNumberLengthMax) {
            for (int i = charCount; i > mNormalPhoneNumberLengthMax; i--) {
                charSize = charSize / 1.03f;
            }
        }
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, charSize);
    }

    /**
     * Add text to the number input field
     * @param text
     */
    public void add(CharSequence text) {
        Editable inputText = mEditText.getText();
        Integer startCursorPosition = mEditText.getSelectionStart();
        Integer endCursorPosition = mEditText.getSelectionEnd();

        removeTextFromInput(startCursorPosition, endCursorPosition);

        inputText.insert(startCursorPosition, text);
    }

    private void removeTextFromInput(Integer startCursorPosition, Integer endCursorPosition) {
        // If there is a selection active delete the selected text.
        if ((endCursorPosition - startCursorPosition) > 0) {
            mEditText.getText().delete(startCursorPosition, endCursorPosition);
        }

        if (startCursorPosition == mEditText.length()) {
            mEditText.setCursorVisible(false);
        } else {
            mEditText.setCursorVisible(true);
        }
    }

    /**
     * Clear the number input field
     */
    public void clear() {
        mEditText.setCursorVisible(false);
        mEditText.getText().clear();
    }

    public void setNumber(String number) {
        mEditText.setText(number);
        mEditText.setSelection(number.length());
    }

    /**
     * Return the number input as String
     * @return
     */
    public String getNumber() {
        return mEditText.getText().toString();
    }

    public interface OnInputChangedListener {
        void onInputChanged(String number);
    }

    public boolean isEmpty() {
        return mEditText.length() == 0;
    }

}
