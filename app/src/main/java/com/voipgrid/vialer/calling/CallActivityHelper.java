package com.voipgrid.vialer.calling;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.contacts.Contacts;

public class CallActivityHelper {

    private final Contacts mContacts;

    public CallActivityHelper(Contacts contacts) {
        mContacts = contacts;
    }

    public void updateLabelsBasedOnPhoneNumber(TextView title, TextView subtitle, String number, String callerId, ImageView imageView) {
        String contactName = mContacts.getContactNameByPhoneNumber(number);

        if (imageView != null) {
            Bitmap bitmap = mContacts.getContactImageByPhoneNumber(number);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.no_user);
            }
        }

        if (contactName == null) {
            contactName = callerId;
        }

        if (contactName != null && !contactName.isEmpty()) {
            title.setText(contactName);
            subtitle.setText(number);
            return;
        }

        title.setText(number);
        subtitle.setText("");
    }
}
