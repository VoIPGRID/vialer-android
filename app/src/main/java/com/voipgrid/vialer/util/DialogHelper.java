package com.voipgrid.vialer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.voipgrid.vialer.R;

/**
 * DialogHelper is a class used for displaying dialogs in a uniform way.
 */
public class DialogHelper {

    /**
     * Function for displaying an alert.
     * @param context The context for showing the alert.
     * @param title Title for the alert message.
     * @param message Message for the alert.
     */
    public static void displayAlert(Context context, String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
