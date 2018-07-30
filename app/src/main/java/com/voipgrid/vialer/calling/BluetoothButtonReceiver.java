package com.voipgrid.vialer.calling;

import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.CALL_BTN;
import static com.voipgrid.vialer.media.BluetoothMediaButtonReceiver.DECLINE_BTN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.voipgrid.vialer.logging.Logger;

public class BluetoothButtonReceiver extends BroadcastReceiver {

    private final Listener mListener;
    private final Logger mLogger;

    public BluetoothButtonReceiver(Listener listener) {
        mListener = listener;
        mLogger = new Logger(this.getClass());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        mLogger.i("mBluetoothButtonReceiver: " + action);

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
