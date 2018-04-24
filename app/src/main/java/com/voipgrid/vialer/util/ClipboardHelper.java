package com.voipgrid.vialer.util;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.Nullable;

public class ClipboardHelper {

    private ClipboardManager mClipboard;

    public ClipboardHelper(ClipboardManager clipboardManager) {
        mClipboard = clipboardManager;
    }

    public static ClipboardHelper fromContext(Context context) {
        return new ClipboardHelper(
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE));
    }

    /**
     * Find the most recent string in the clipboard.
     *
     * @return The most recent string in the clipboard, if one does not exist, then null.
     */
    public @Nullable String getMostRecentString() {
        if (!mClipboard.hasPrimaryClip()) return null;

        if (!mClipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
            return null;
        }

        ClipData.Item item = mClipboard.getPrimaryClip().getItemAt(0);

        return item.getText().toString();
    }
}
