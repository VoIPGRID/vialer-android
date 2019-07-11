package com.voipgrid.vialer.bluetooth;

import static android.view.KeyEvent.KEYCODE_CALL;
import static android.view.KeyEvent.KEYCODE_ENDCALL;
import static android.view.KeyEvent.KEYCODE_MEDIA_PAUSE;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.voipgrid.vialer.logging.Logger;
import com.voipgrid.vialer.notifications.MediaButtonNotification;
import com.voipgrid.vialer.sip.SipService;

public class BluetoothMediaSessionService extends Service {

    public static final String SHOULD_NOT_START_IN_FOREGROUND_EXTRA = "com.voipgrid.vialer.media.SHOULD_NOT_START_IN_FOREGROUND_EXTRA";

    private MediaSessionCompat mSession;
    private Logger mLogger;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mLogger = new Logger(BluetoothMediaSessionService.class);
        mLogger.v("onCreate()");
        MediaSessionCompat session = new MediaSessionCompat(this, getClass().getSimpleName());
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        session.setPlaybackState(stateBuilder.build());
        session.setCallback(new BluetoothButtonHandler(this));
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        session.setPlaybackToLocal(AudioManager.STREAM_VOICE_CALL);
        mSession = session;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLogger.v("onStartCommand");

        if (shouldBecomeForegroundService(intent)) {
            startForeground(1, new MediaButtonNotification().build());
        }

        mSession.setActive(true);

        MediaButtonReceiver.handleIntent(mSession, intent);

        return START_NOT_STICKY;
    }

    /**
     * Determines if this service should be run as a foreground service based on the passed intent and the OS version.
     *
     * @param intent
     * @return TRUE if the service should be run in foreground
     */
    private boolean shouldBecomeForegroundService(Intent intent) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ! intent.getBooleanExtra(SHOULD_NOT_START_IN_FOREGROUND_EXTRA, false);
    }

    @Override
    public void onDestroy() {
        mLogger.v("onDestroy");
        mSession.setActive(false);

        mSession.release();
    }

    /**
     * Start this service.
     *
     * @param context
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, BluetoothMediaSessionService.class);
        intent.putExtra(SHOULD_NOT_START_IN_FOREGROUND_EXTRA, true);
        context.startService(intent);
    }


    private final class BluetoothButtonHandler extends MediaSessionCompat.Callback {

        private Logger logger;
        private Context context;
        private BluetoothKeyNormalizer bluetoothKeyNormalizer;

        private BluetoothButtonHandler(Context context) {
            this.context = context;
            this.logger = new Logger(this);
            this.bluetoothKeyNormalizer = BluetoothKeyNormalizer.defaultAliases();
        }

        @Override
        public boolean onMediaButtonEvent(final Intent mediaButtonEvent) {
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            logger.i("Received key event:" + keyEvent.getCharacters());

            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) return true;

            Integer code = bluetoothKeyNormalizer.normalize(keyEvent.getKeyCode());

            if (code == null) {
                mLogger.e("Received a key code that we don't know how to handle: " + keyEvent.getKeyCode());
                return true;
            }

            logger.i("Key event has been normalized from " + keyEvent.getKeyCode() + " to " + code);

            String action = convertKeycodeToSipAction(code);

            if (action != null) {
                SipService.performActionOnSipService(context, action);
            }

            stopSelf();

            return true;
        }

        /**
         * Based on the provided key code, perform an action on the sip service.
         *
         * @param code
         * @return
         */
        private String convertKeycodeToSipAction(int code) {
            switch (code) {
                case KEYCODE_CALL:
                    return SipService.Actions.ANSWER_INCOMING_CALL;
                case KEYCODE_ENDCALL:
                    return SipService.Actions.END_CALL;
            }

            return null;
        }
    }
}