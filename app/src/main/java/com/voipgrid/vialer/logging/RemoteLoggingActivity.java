package com.voipgrid.vialer.logging;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity that implements our own send logs to developer option.
 */
public class RemoteLoggingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new RemoteUncaughtExceptionHandler(this));
    }
}
