package com.voipgrid.vialer;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

/**
 * Created by eltjo on 26/08/15.
 */
public class NumberInputEditText extends RelativeLayout implements
        View.OnClickListener,
        View.OnLongClickListener,
        TextWatcher {

    private EditText mEditText;
    private ImageButton mRemoveButton;
    private OnInputChangedListener mListener;

    public NumberInputEditText(Context context) {
        super(context);
        init();
    }

    public NumberInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NumberInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
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

        /* remove character from input when remove button is clicked */
        if(view.getId() == R.id.remove_button) {
            remove();
        }
    }

    @Override
    public boolean onLongClick(View view) {

        /* clear character from input when remove button is long clicked */
        if(view.getId() == R.id.remove_button) {
            clear();
            return true;
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

}
