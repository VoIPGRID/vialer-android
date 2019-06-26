package com.voipgrid.vialer.t9;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.contacts.ContactsSyncTask;
import com.voipgrid.vialer.dialer.DialerActivity;

public class ContactsImportProgressUpdater implements Runnable {

    private final DialerActivity activity;
    private final TextView progressText;
    private final ProgressBar progressBar;

    /**
     * The frequency to check the current status of the contacts import.
     */
    private static final int POLL_TIME_MS = 500;

    private ContactsImportProgressUpdater(DialerActivity activity, TextView progressText,
            ProgressBar progressBar) {
        this.activity = activity;
        this.progressText = progressText;
        this.progressBar = progressBar;
    }

    public static Thread start(DialerActivity activity, TextView progressText,
            ProgressBar progressBar) {
        Thread thread = new Thread(new ContactsImportProgressUpdater(activity, progressText, progressBar));
        thread.start();
        return thread;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (ContactsSyncTask.getProgress().isComplete()) {
                this.activity.runOnUiThread(this.activity::refreshUi);
                return;
            }

            this.activity.runOnUiThread(() -> {
                this.progressText.setText(this.activity.getString(
                        R.string.dialer_contacts_not_processed,
                        ContactsSyncTask.getProgress().getProcessed(),
                        ContactsSyncTask.getProgress().getTotal())
                );
                this.progressBar.setProgress(getCompletionPercentage());
            });

            try {
                Thread.sleep(POLL_TIME_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int getCompletionPercentage() {
        return (int) (((float) ContactsSyncTask.getProgress().getProcessed() / (float) ContactsSyncTask.getProgress().getTotal()) * 100);
    }
}
