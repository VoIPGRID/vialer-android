package com.voipgrid.vialer.calling;

import androidx.annotation.StringDef;

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

    String CALL_IS_CONNECTED = "callIsConnected";
    String CALL_BLUETOOTH_ACTIVE = "bluetoothAvailable";
    String CALL_BLUETOOTH_CONNECTED = "bluetoothConnected";
}
