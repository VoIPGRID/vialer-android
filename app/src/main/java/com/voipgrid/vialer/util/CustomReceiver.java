package com.voipgrid.vialer.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;

/**
 *
 * Reciever used for custom events. Created to support all sorts of headset buttons. Wireless and wired.
 */
public class CustomReceiver extends BroadcastReceiver {

    public static final String CALL_BTN = "call_btn";
    public static final String DECLINE_BTN = "decl_btn";
    public static IntentFilter mMediaButtonFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = keyEvent.getKeyCode();
                    switch (keyCode) {
                        // Headsets with a combined media/call button. These are very common
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            onMediabutton(context);
                            return;
                        // Some headsets like the gmb berlin sometimes send the pause signal on the media key.
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            onMediabutton(context);
                            return;
                        // Wired one key headsets. Currently not used.
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            return;
                        // Headsets with a dedicated call button, seperated from the media button
                        case KeyEvent.KEYCODE_CALL:
                            onMediabutton(context);
                            return;
                        // Currently not used.
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            return;
                        // Currently not used.
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            return;
                        // Headsets with dedicated hangup button
                        case KeyEvent.KEYCODE_ENDCALL:
                            hangup(context);
                            return;
                    }
                }
            }
        }
    }

    protected void onMediabutton(Context context) {
        context.sendBroadcast(new Intent(CALL_BTN));
    }

    private void hangup(Context context) {
        context.sendBroadcast(new Intent(DECLINE_BTN));
    }
}

