package com.voipgrid.vialer.calling;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface CallingConstants {
    @StringDef({TYPE_INCOMING_CALL, TYPE_OUTGOING_CALL, TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL, TYPE_CONNECTED_CALL})
    @Retention(RetentionPolicy.SOURCE)
    @interface CallTypes {}

    String TYPE_OUTGOING_CALL = "type-outgoing-call";
    String TYPE_INCOMING_CALL = "type-incoming-call";
    String TYPE_NOTIFICATION_ACCEPT_INCOMING_CALL = "type-incoming-accept-call-notification";
    String TYPE_CONNECTED_CALL = "type-connected-call";
    String CONTACT_NAME = "contact-name";
    String PHONE_NUMBER = "phone-number";
    String TAG_CALL_CONNECTED_FRAGMENT = "callConnectedFragment";
    String TAG_CALL_INCOMING_FRAGMENT = "callIncomingFragment";
    String TAG_CALL_LOCK_RING_FRAGMENT = "callLockRingFragment";
    String TAG_CALL_KEY_PAD_FRAGMENT = "callKeyPadFragment";
    String TAG_CALL_TRANSFER_FRAGMENT = "callTransferFragment";
    String TAG_CALL_TRANSFER_COMPLETE_FRAGMENT = "callTransferCompleteFragment";

    String MAP_ORIGINAL_CALLER_PHONE_NUMBER = "originalCallerPhoneNumber";
    String MAP_ORIGINAL_CALLER_ID = "originalCallerId";
    String MAP_TRANSFERRED_PHONE_NUMBER = "transferredNumber";
    String MAP_SECOND_CALL_IS_CONNECTED = "secondCallIsConnected";

    String CALL_IS_CONNECTED = "callIsConnected";
}
