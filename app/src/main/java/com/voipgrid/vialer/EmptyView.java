package com.voipgrid.vialer;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * EmptyView widget to display a message when the app can not display call records
 */
public class EmptyView extends LinearLayout {

    public EmptyView(Context context, String message) {
        super(context, null, R.style.EmptyViewStyle);

        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.empty_view, this);

        ((TextView) findViewById(R.id.message)).setText(message);
    }
}
