package com.voipgrid.vialer.util;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * There are multiple fragments that require nearly the exact same logic but they must extend different
 * classes, this helper is used so we don't need to repeat the code in both fragments.
 */
public class TwoFactorFragmentHelper {

    private final EditText mCodeField;
    private final ClipboardHelper mClipboardHelper;
    private InputMethodManager mInputMethodManager;

    public TwoFactorFragmentHelper(Context context, EditText codeField) {
        mCodeField = codeField;
        mClipboardHelper = ClipboardHelper.fromContext(context);
        mInputMethodManager =  (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Requests focus from the 2fa field and prompts the keyboard to appear, this is so the user
     * can begin typing in the field immediately (as there is only one field in the view).
     *
     */
    public void focusOnTokenField() {
        mCodeField.requestFocus();
        mInputMethodManager.showSoftInput(mCodeField, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Automatically paste the 2fa code from the clipboard into the code field if there is a valid
     * code stored.
     *
     */
    public void pasteCodeFromClipboard() {
        String pasteData = mClipboardHelper.getMostRecentString();

        if (pasteData == null) {
            return;
        }

        if (!looksLike2faCode(pasteData)) {
            return;
        }

        mCodeField.setText(pasteData);
        mCodeField.setSelection(pasteData.length());
    }

    /**
     * Check if the paste data contains a valid value from Google Authenticator.
     *
     * @param potentialCode
     * @return
     */
    private boolean looksLike2faCode(String potentialCode) {
        return potentialCode.matches("[0-9]{6}");
    }
}
