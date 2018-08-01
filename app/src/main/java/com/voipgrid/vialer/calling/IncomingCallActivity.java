package com.voipgrid.vialer.calling;

import android.os.Bundle;
import android.widget.Button;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.LoginRequiredActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IncomingCallActivity extends LoginRequiredActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        ButterKnife.bind(this);
        VialerApplication.get().component().inject(this);
    }

}
