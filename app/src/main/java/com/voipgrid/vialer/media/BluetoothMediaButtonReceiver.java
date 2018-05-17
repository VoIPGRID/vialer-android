package com.voipgrid.vialer.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.voipgrid.vialer.bluetooth.BluetoothKeyNormalizer;
import com.voipgrid.vialer.logging.RemoteLogger;


public class BluetoothMediaButtonReceiver extends BroadcastReceiver {
    public static final String CALL_BTN = "call_btn";
    public static final String HANGUP_BTN = "hangup_btn";
    public static final String DECLINE_BTN = "decl_btn";

    private static boolean mAnswer = false;

    private static RemoteLogger mRemoteLogger;
    private static Context mContext;

    private static final BluetoothKeyNormalizer sBluetoothKeyNormalizer = BluetoothKeyNormalizer.defaultAliases();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        mRemoteLogger.d("onReceive : " + action);

        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null) {
                handleKeyEvent(context, keyEvent);
            }
        }
    }

    public static void handleKeyEvent(Context context, KeyEvent keyEvent) {
        mContext = context;

        if (mRemoteLogger == null) {
            mRemoteLogger = new RemoteLogger(BluetoothMediaButtonReceiver.class).enableConsoleLogging();
        }

        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) return;

        Integer keyCode = sBluetoothKeyNormalizer.normalize(keyEvent.getKeyCode());

        if(keyCode == null) {
            mRemoteLogger.i("Unable to handle KeyEvent: " + keyEvent.getKeyCode());
            return;
        }

        mRemoteLogger.d("handleKeyEvent()");
        mRemoteLogger.d("===> " + keyEvent);

        if(keyCode == KeyEvent.KEYCODE_CALL) {
            if (!mAnswer) {
                mAnswer = true;
                sendAnswerBroadcast();
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            mAnswer = !mAnswer;
            sendAnswerBroadcast();
        }
        else if(keyCode == KeyEvent.KEYCODE_ENDCALL) {
            mAnswer = false;
            sendAnswerBroadcast();
        }
    }

    static void sendAnswerBroadcast() {
        mRemoteLogger.i("sendAnswerBroadcast()");
        mRemoteLogger.i("==> answer: " + mAnswer);
        mContext.sendBroadcast(new Intent(mAnswer ? CALL_BTN : DECLINE_BTN));
    }

    public static void setCallAnswered(boolean answer) {
        mAnswer = answer;
    }
}