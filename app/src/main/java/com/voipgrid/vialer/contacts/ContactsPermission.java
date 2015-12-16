package com.voipgrid.vialer.contacts;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.voipgrid.vialer.R;


/**
 * Class for Android 6.0+ related contact permissions.
 */
public class ContactsPermission {

    public static final String mPermissionToCheck = Manifest.permission.READ_CONTACTS;
    public static final String[] mPermissions = new String[] {Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS, Manifest.permission.GET_ACCOUNTS};

    /**
     * Function to check if the we have the contacts permission.
     * @param context Context needed for the check.
     * @return Whether or not we have permission.
     */
    public static boolean hasPermission(Context context) {
        if (ContextCompat.checkSelfPermission(context, mPermissionToCheck) ==
                PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    /**
     * Function to ask the user for the contact permissions.
     * @param activity The activity where to show the permissions dialogs.
     */
    public static void askForPermission(final Activity activity) {
        // Request code for the callback verifying in the Activity.
        final int requestCode = activity.getResources().getInteger(
                R.integer.contact_permission_request_code);
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                ContactsPermission.mPermissionToCheck)) {
            // Function to show a dialog that explains the permissions.
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.permission_contact_dialog_title));
            builder.setMessage(activity.getString(R.string.permission_contact_dialog_message));
            builder.setPositiveButton(activity.getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            ActivityCompat.requestPermissions(activity, mPermissions, requestCode);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            ActivityCompat.requestPermissions(activity, mPermissions, requestCode);
        }
    }
}
