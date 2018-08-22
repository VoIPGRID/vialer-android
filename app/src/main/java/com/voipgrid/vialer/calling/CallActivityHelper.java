package com.voipgrid.vialer.calling;

import android.widget.TextView;

public class CallActivityHelper {

    public void updateLabelsBasedOnPhoneNumber(TextView title, TextView subtitle, String number, String callerId) {
        String contactName = null; // TODO this should use the method to lookup the name from the contacts

        if (contactName == null) {
            contactName = callerId;
        }

        if (contactName != null) {
            title.setText(contactName);
            subtitle.setText(number);
            return;
        }

        title.setText(number);
        subtitle.setText("");
    }
}
