package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.CALL_BTN;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.RemoteLogger;

public class BluetoothButtonReceiver extends BroadcastReceiver {

    private final Listener mListener;
    private final RemoteLogger mRemoteLogger;

    public BluetoothButtonReceiver(Listener listener) {
        mListener = listener;
        mRemoteLogger = new RemoteLogger(this.getClass()).enableConsoleLogging();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        mRemoteLogger.i("mBluetoothButtonReceiver: " + action);

        if (action.equals(CALL_BTN)) {
            mListener.bluetoothCallButtonWasPressed();
        } else if (action.equals(DECLINE_BTN)) {
            mListener.bluetoothDeclineButtonWasPressed();
        }
    }

    interface Listener {
        void bluetoothCallButtonWasPressed();
        void bluetoothDeclineButtonWasPressed();
    }
}
