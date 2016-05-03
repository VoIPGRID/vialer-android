package com.voipgrid.vialer;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.voipgrid.vialer.util.LoginRequiredActivity;

/**
 * Activity for showing a warning message.
 */
public class WarningActivity extends LoginRequiredActivity {

    public static final String MESSAGE = "key-message";
    public static final String TITLE = "key-title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_warning);

        /* set the Toolbar to use as ActionBar */
        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));

        /* enabled home as up for the Toolbar */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* enabled home button for the Toolbar */
        getSupportActionBar().setHomeButtonEnabled(true);

        ((TextView) findViewById(R.id.text_view_title))
                .setText(getIntent().getStringExtra(TITLE));
        ((TextView) findViewById(R.id.text_view_message))
                .setText(getIntent().getStringExtra(MESSAGE));

    }
}
