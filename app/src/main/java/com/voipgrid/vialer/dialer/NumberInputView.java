package com.voipgrid.vialer.dialer;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
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

        /* find the number input field and add a TextChangedListener to handle text changes */
        mEditText = (EditText) findViewById(R.id.edit_text);
        mEditText.addTextChangedListener(this);
        mEditText.setOnClickListener(this);
        mEditText.setOnLongClickListener(this);

        /* find the remove button and add an OnClickListener */
        mRemoveButton = (ImageButton) findViewById(R.id.remove_button);
        mRemoveButton.setOnClickListener(this);
        mRemoveButton.setOnLongClickListener(this);
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

        /* set visibility for the delete Button based on number of characters */
        mRemoveButton.setVisibility(count == 0 ? View.INVISIBLE : View.VISIBLE);

        mListener.onInputChanged(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {
        //not used
    }

    /**
     * Remove last character from number input field
     */
    public void remove() {
        mEditText.setCursorVisible(false);
        String text = mEditText.getText().toString();
        int length = text.length();
        if(length > 0) {
            mEditText.setText(text.substring(0, length - 1));
        }
    }

    /**
     * Add text to the number input field
     * @param text
     */
    public void add(CharSequence text) {
        mEditText.getText().append(text);
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
    }

    /**
     * Return the number input as String
     * @return
     */
    public String getNumber() {
        return mEditText.getText().toString();
    }

    interface OnInputChangedListener {
        public void onInputChanged(String number);
    }

    public boolean isEmpty() {
        return mEditText.length() == 0;
    }

}
