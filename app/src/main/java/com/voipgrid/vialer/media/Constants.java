package com.voipgrid.vialer.media;

import android.media.AudioManager;

/**
 * Created by redmerloen on 6/2/17.
 */

public class Constants {

    /**
     * The audio mode the app is using.
     */
    static final int DEFAULT_AUDIO_MODE = AudioManager.MODE_IN_COMMUNICATION;

    /**
     * Routes where the audio is going.
     */
    static final int ROUTE_INVALID = -1;
    static final int ROUTE_EARPIECE = 0;
    static final int ROUTE_SPEAKER = 1;
    static final int ROUTE_HEADSET = 2;
    static final int ROUTE_BT = 3;

    /**
     * States a call can be in globally.
     */
    static final int CALL_INVALID = -1;
    static final int CALL_RINGING = 0;
    static final int CALL_ANSWERED = 1;
    static final int CALL_OUTGOING = 2;

}
