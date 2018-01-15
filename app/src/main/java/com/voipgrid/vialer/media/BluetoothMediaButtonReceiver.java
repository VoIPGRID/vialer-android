package com.voipgrid.vialer.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.voipgrid.vialer.logging.RemoteLogger;


public class BluetoothMediaButtonReceiver extends BroadcastReceiver {
    public static final String CALL_BTN = "call_btn";
    public static final String HANGUP_BTN = "hangup_btn";
    public static final String DECLINE_BTN = "decl_btn";

    private static boolean mAnswer = false;

    private static RemoteLogger mRemoteLogger;
    private static Context mContext;

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
            mRemoteLogger = new RemoteLogger(BluetoothMediaButtonReceiver.class, 1);
        }

        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = keyEvent.getKeyCode();

            mRemoteLogger.d("handleKeyEvent()");
            mRemoteLogger.d("===> " + keyEvent);

            switch (keyCode) {
                // Headsets with a combined media/call button. These are very common
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                // Headsets with a dedicated call button, separated from the media button
                case KeyEvent.KEYCODE_CALL:
                    if (!mAnswer) {
                        mAnswer = true;
                        sendAnswerBroadcast();
                    }
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                // Some headsets like the gmb berlin sometimes send the pause signal on the media key.
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    mAnswer = !mAnswer;
                    sendAnswerBroadcast();
                    break;
                // Headsets with dedicated hangup button
                case KeyEvent.KEYCODE_MEDIA_STOP:
                case KeyEvent.KEYCODE_ENDCALL:
                    mAnswer = false;
                    sendAnswerBroadcast();
                    break;
                // Currently not used.
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                default:
                    break;
            }
        }
    }

    static void sendAnswerBroadcast() {
        mRemoteLogger.i("sendAnswerBroadcast()");
        mRemoteLogger.i("==> answer: " + mAnswer);
        mContext.sendBroadcast(new Intent(mAnswer ? CALL_BTN : DECLINE_BTN));
    }

    public static void setCallAnswered() {
        mAnswer = true;
    }
}