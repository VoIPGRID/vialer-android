package com.voipgrid.vialer.logging.file;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class LogFileCreator {

    private static final String LOG_FILE_NAME = "LogentriesLogStorage.log";

    private Context mContext;

    public LogFileCreator(Context context) {
        mContext = context;
    }

    /**
     * Creates the log file if it does not exist.
     *
     * @return File The log file.
     */
    @SuppressWarnings("UnusedReturnValue")
    public File createIfDoesNotExist() {
        File logFile = new File(mContext.getFilesDir(), LOG_FILE_NAME);
        if(!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return logFile;
    }
}
