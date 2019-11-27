package com.voipgrid.vialer.util;

import android.content.Context;

import com.chaos.view.PinView;

/**
 * There are multiple fragments that require nearly the exact same logic but they must extend different
 * classes, this helper is used so we don't need to repeat the code in both fragments.
 */
public class TwoFactorFragmentHelper {

    private PinView mCodeField;
    private ClipboardHelper mClipboardHelper;

    public TwoFactorFragmentHelper(Context context, PinView codeField) {
        mCodeField = codeField;
        mClipboardHelper = ClipboardHelper.fromContext(context);
    }

    /**
     * Automatically paste the 2fa code from the clipboard into the code field if there is a valid
     * code stored.
     *
     */
    public Boolean pasteCodeFromClipboard() {
        String pasteData = mClipboardHelper.getMostRecentString();

        if (pasteData == null) {
            return false;
        }

        if (!looksLike2faCode(pasteData)) {
            return false;
        }

        mCodeField.setText(pasteData);
        mCodeField.setSelection(pasteData.length());
        return true;
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
