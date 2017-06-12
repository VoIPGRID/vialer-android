package com.voipgrid.vialer.permissions;

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
 * Class that handles asking for the phone permissions.
 */
public class PhonePermission {

    public static final int GRANTED = 0;
    public static final int DENIED = 1;
    public static final int BLOCKED = 2;
    public static boolean firstRequest = true;

    public static final String mPermissionToCheck = Manifest.permission.READ_PHONE_STATE;
    public static final String[] mPermissions = new String[] {
            Manifest.permission.READ_PHONE_STATE,
    };

    /**
     * Function to check if the we have the phone permission.
     * @param context Context needed for the check.
     * @return Whether or not we have permission.
     */
    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, mPermissionToCheck) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private static int getPermissionStatus(Activity activity, String androidPermissionName) {
        if(ContextCompat.checkSelfPermission(activity, androidPermissionName) !=
                PackageManager.PERMISSION_GRANTED) {
            if(!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    androidPermissionName)){
                return BLOCKED;
            }
            return DENIED;
        }
        return GRANTED;
    }

    private static void showPermissionDeniedDialog(final Activity activity){
        final int requestCode = activity.getResources().getInteger(
                R.integer.phone_permission_request_code);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.permission_phone_dialog_title));
        builder.setMessage(
                activity.getResources().getString(R.string.permission_phone_dialog_message));
        builder.setPositiveButton(activity.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        ActivityCompat.requestPermissions(activity, mPermissions, requestCode);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Function to ask the user for the phone permissions.
     * @param activity The activity where to show the permissions dialogs.
     */
    public static void askForPermission(final Activity activity) {
        int permissionStatus = getPermissionStatus(activity, mPermissionToCheck);

        // Request code for the callback verifying in the Activity.
        final int requestCode = activity.getResources().getInteger(
                R.integer.microphone_permission_request_code);

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                PhonePermission.mPermissionToCheck)) {

            if (permissionStatus == DENIED) {
                // Permission has previously been denied.
                showPermissionDeniedDialog(activity);
            }

        } else if(permissionStatus == BLOCKED && firstRequest) {
            // Permission has not yet been requested. Request it immediately.
            ActivityCompat.requestPermissions(activity, mPermissions, requestCode);
            firstRequest = false;
        }
    }
}
