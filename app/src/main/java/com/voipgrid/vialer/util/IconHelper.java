package com.voipgrid.vialer.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;

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

    private static TextDrawable getCallerIconTextDrawable(String string, int color) {
        return TextDrawable.builder().buildRound(string, color);
    }

    public static Bitmap getCallerIconBitmap(String string, String number, int color) {
        if (color == 0) {
            color = ColorGenerator.MATERIAL.getColor(number);
        }

        Bitmap bitmap = drawableToBitmap(getCallerIconTextDrawable(string, color));

        if (string == null || string.isEmpty()) {
            return addPlaceholderPicture(bitmap, color);
        }

        return bitmap;
    }

    private static Bitmap addPlaceholderPicture(Bitmap circle, int color) {
        Drawable iconDrawable = VialerApplication.get().getResources().getDrawable(R.drawable.ic_person);
        iconDrawable.setColorFilter(manipulateColor(color, 0.9f), PorterDuff.Mode.MULTIPLY);
        Bitmap iconBitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(), iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas iconCanvas = new Canvas(iconBitmap);

        Canvas circleCanvas = new Canvas(circle);

        iconDrawable.setBounds(0, 0, iconCanvas.getWidth(), iconCanvas.getHeight());
        iconDrawable.draw(iconCanvas);
        circleCanvas.drawBitmap(iconBitmap, (circle.getWidth() - iconBitmap.getWidth()) / 2, (circle.getHeight() - iconBitmap.getHeight()) / 2, null);
        return circle;
    }

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r,255),
                Math.min(g,255),
                Math.min(b,255));
    }
}
