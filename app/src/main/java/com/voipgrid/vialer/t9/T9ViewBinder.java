package com.voipgrid.vialer.t9;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Html;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.util.IconHelper;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class T9ViewBinder implements SimpleCursorAdapter.ViewBinder {

    private final Context context;

    public T9ViewBinder(Context context) {
        this.context = context;
    }

    /**
     * Binds the Cursor column defined by the specified index to the specified view.
     */
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (view.getId() == R.id.text_view_contact_icon) {
            // The class stores a contact uri for which
            // we can retrieve a photo.
            boolean hasThumbnail = false;
            String thumbnailUriString = cursor.getString(columnIndex);
            if (thumbnailUriString != null) {
                Bitmap thumbNail = loadContactThumbnail(thumbnailUriString);
                if (thumbNail != null) {
                    hasThumbnail = true;
                    ((CircleImageView) view).setImageBitmap(thumbNail);
                }
            }

            if (!hasThumbnail) {
                String firstLetter = cursor.getString(1).replaceAll("\\<.*?>", "").substring(0, 1);
                Bitmap bitmapImage = IconHelper.getCallerIconBitmap(firstLetter, cursor.getString(0), 0);
                ((CircleImageView) view).setImageBitmap(bitmapImage);
            }
            return true;
        } else if (view instanceof TextView) {
            String result = cursor.getString(columnIndex);
            ((TextView) view).setText(Html.fromHtml(result != null ? result : ""));
            return true;
        }
        return false;
    }

    /**
     * Function to get the bitmap from the uri.
     *
     * @param thumbnailUriString String uri of the file
     * @return Bitmap of the uri.
     */
    private Bitmap loadContactThumbnail(String thumbnailUriString) {
        Uri thumbUri = Uri.parse(thumbnailUriString);
        AssetFileDescriptor afd = null;
        try {
            afd = context.getContentResolver().openAssetFileDescriptor(thumbUri, "r");
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            if (fileDescriptor != null) {
                // Decodes the bitmap
                return BitmapFactory.decodeFileDescriptor(
                        fileDescriptor, null, null);
            }
        } catch (FileNotFoundException e) {

        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }
}