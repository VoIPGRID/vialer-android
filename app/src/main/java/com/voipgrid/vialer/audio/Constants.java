package com.voipgrid.vialer.audio;

/**
 * Constants what state the audio and call can have.
 */
interface Constants {

    /**
     * Routes where the audio is going.
     */
    int ROUTE_INVALID = -1;
    int ROUTE_EARPIECE = 0;
    int ROUTE_SPEAKER = 1;
    int ROUTE_HEADSET = 2;
    int ROUTE_BT = 3;

    int STATE_WIRED_HS_INVALID = -1;
    int STATE_WIRED_HS_UNPLUGGED = 0;
    int STATE_WIRED_HS_PLUGGED = 1;
}
