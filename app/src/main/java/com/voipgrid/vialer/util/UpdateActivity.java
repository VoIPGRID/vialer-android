package com.voipgrid.vialer.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.voipgrid.vialer.MainActivity;
import com.voipgrid.vialer.OnUpdateCompleted;
import com.voipgrid.vialer.R;

/**
 * Activity to start the updating process.
 */
public class UpdateActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_logo);

        // Start updateHelper that will check if any actions have to be performed
        // before starting the app. onTaskCompleted will be called when the updateHelper
        // has completed it's work.
        UpdateHelper updateHelper = new UpdateHelper(this, new OnUpdateCompleted() {

            @Override
            public void OnUpdateCompleted() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        });
        updateHelper.execute();
    }
}
