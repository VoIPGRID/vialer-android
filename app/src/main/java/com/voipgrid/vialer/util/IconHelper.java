package com.voipgrid.vialer.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.amulyakhare.textdrawable.TextDrawable;

/**
 * Class to create contact icons with a character written in them.
 * Includes functions to retrieve either as bitmap or as TextDrawable.
 */
public class IconHelper {

    private static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 96;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 96;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static TextDrawable getCallerIconTextDrawable (String string, int color) {
        TextDrawable textDrawable = TextDrawable.builder()
                .buildRound(string, color);
        return textDrawable;
    }

    public static Bitmap getCallerIconBitmap (String string, int color) {
        TextDrawable textDrawable = getCallerIconTextDrawable(string, color);
        Bitmap bitmap = drawableToBitmap(textDrawable);
        return bitmap;
    }
}
