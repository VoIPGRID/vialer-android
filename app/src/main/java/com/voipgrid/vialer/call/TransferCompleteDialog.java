package com.voipgrid.vialer.call;

import android.content.Context;
import android.view.View;

import com.voipgrid.vialer.R;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class TransferCompleteDialog extends SweetAlertDialog {

    TransferCompleteDialog(Context context, String firstPhoneNumber, String secondPhoneNumber) {
        super(context,  SweetAlertDialog.SUCCESS_TYPE);
        this.showCancelButton(false);
        this.setContentText(
                context.getString(R.string.call_transfer_complete_success, firstPhoneNumber, secondPhoneNumber)
        );
        this.setCancelable(false);
        this.setOnShowListener(dialogInterface -> {
            View view = (View) findViewById(R.id.confirm_button).getParent();
            view.setVisibility(View.GONE);
        });
    }

    public static TransferCompleteDialog createAndShow(Context context, String phoneNumber, String secondPhoneNumber) {
        TransferCompleteDialog dialog = new TransferCompleteDialog(context, phoneNumber, secondPhoneNumber);
        dialog.show();
        return dialog;
    }
}
