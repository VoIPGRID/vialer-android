package com.voipgrid.vialer.media;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import com.voipgrid.vialer.logging.RemoteLogger;


public class BluetoothMediaSessionService extends Service {

    private final static String TAG = BluetoothMediaSessionService.class.getSimpleName();
    private MediaSessionCompat mSession;
    private Context mContext;
    private RemoteLogger mRemoteLogger;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mContext = this;
        mRemoteLogger = new RemoteLogger(this, BluetoothMediaSessionService.class, 1);
        mRemoteLogger.v("onCreate()");
        MediaSessionCompat session = new MediaSessionCompat(
                this,
                TAG
        );
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
        session.setPlaybackState(stateBuilder.build());
        session.setCallback(mSessionCallback);
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        session.setPlaybackToLocal(AudioManager.STREAM_VOICE_CALL);
        mSession = session;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mRemoteLogger.v("onStartCommand");
        mSession.setActive(true);

        MediaButtonReceiver.handleIntent(mSession, intent);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mRemoteLogger.v("onDestroy");
        mSession.setActive(false);

        mSession.release();
    }


    private final MediaSessionCompat.Callback mSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
            KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            mRemoteLogger.d("SessionCallback.sendAnswerBroadcast");
            mRemoteLogger.d("==> event = " + keyEvent);

            if (keyEvent != null) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    BluetoothMediaButtonReceiver.handleKeyEvent(mContext, keyEvent);
                }
            }
            return true;
        }
    };
}
