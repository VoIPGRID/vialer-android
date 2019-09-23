package com.voipgrid.vialer.statistics;

public interface StatsConstants {
    String KEY_OS = "os";
    String KEY_OS_VERSION = "os_version";
    String KEY_APP_VERSION = "app_version";
    String KEY_DEVICE_MANUFACTURER = "device_manufacturer";
    String KEY_DEVICE_MODEL = "device_model";
    String KEY_APP_STATUS = "app_status";
    String KEY_MIDDLEWARE_KEY = "middleware_unique_key";
    String KEY_NETWORK = "network";
    String KEY_NETWORK_OPERATOR = "network_operator";
    String KEY_CLIENT_COUNTRY = "client_country";
    String KEY_LOG_ID = "log_id";
    String KEY_CALL_ID = "call_id";
    String KEY_CALL_DIRECTION = "direction";
    String KEY_BLUETOOTH_AUDIO_ENABLED = "bluetooth_audio";
    String KEY_BLUETOOTH_DEVICE_NAME = "bluetooth_device";
    String KEY_MIDDLEWARE_ATTEMPTS = "attempt";
    String KEY_FAILED_REASON = "failed_reason";
    String KEY_TIME_TO_INITIAL_RESPONSE = "time_to_initial_response";
    String KEY_CALL_SETUP_SUCCESSFUL = "call_setup_successful";
    String KEY_HANGUP_REASON = "hangup_reason";
    String KEY_SIP_USER_ID = "sip_user_id";
    String KEY_CONNECTION_TYPE = "connection_type";
    String KEY_ACCOUNT_CONNECTION_TYPE = "account_connection_type";
    String KEY_CALL_DURATION = "call_duration";
    String KEY_RX_PACKETS = "rx_packets";
    String KEY_TX_PACKETS = "tx_packets";
    String KEY_MOS = "mos";
    String KEY_CODEC = "codec";

    String VALUE_OS = "Android";
    String VALUE_APP_STATUS_ALPHA = "Alpha";
    String VALUE_APP_STATUS_BETA = "Beta";
    String VALUE_APP_STATUS_PRODUCTION = "Production";

    String VALUE_NETWORK_WIFI = "WiFi";
    String VALUE_NETWORK_4G = "4G";
    String VALUE_NETWORK_3G = "3G";
    String VALUE_NETWORK_UNKNOWN = "unknown";
    String VALUE_NETWORK_NO_CONNECTION = "NoConnection";
    String VALUE_ACCOUNT_CONNECTION_TYPE_TLS = "TLS";
    String VALUE_ACCOUNT_CONNECTION_TYPE_TCP = "TCP";

    String VALUE_BLUETOOTH_AUDIO_ENABLED_TRUE = "yes";
    String VALUE_BLUETOOTH_AUDIO_ENABLED_FALSE = "no";

    String VALUE_FAILED_REASON_NO_AUDIO = "AUDIO_RX_TX";
    String VALUE_FAILED_REASON_NO_AUDIO_SENT = "AUDIO_TX";
    String VALUE_FAILED_REASON_NO_AUDIO_RECEIVED = "AUDIO_RX";
    String VALUE_FAILED_NO_CALL_RECEIVED_FROM_ASTERISK = "OK_MIDDLEWARE_NO_CALL";
    String VALUE_FAILED_INSUFFICIENT_NETWORK = "INSUFFICIENT_NETWORK";
    String VALUE_FAILED_GSM_CALL_IN_PROGRESS = "DECLINED_ANOTHER_CALL_IN_PROGRESS";
    String VALUE_FAILED_VIALER_CALL_IN_PROGRESS = "DECLINED_ANOTHER_VIALER_CALL_IN_PROGRESS";
    String VALUE_FAILED_NATIVE_CALL_IN_PROGRESS = "DECLINED_ANOTHER_VIALER_CALL_IN_PROGRESS";
    String VALUE_FAILED_REASON_DECLINED = "DECLINED";
    String VALUE_FAILED_REASON_COMPLETED_ELSEWHERE = "CALL_COMPLETED_ELSEWHERE";
    String VALUE_FAILED_REASON_ORIGINATOR_CANCELLED = "ORIGINATOR_CANCELLED";

    String VALUE_CALL_SETUP_SUCCESSFUL = "true";
    String VALUE_CALL_SETUP_FAILED = "false";

    String VALUE_CALL_DIRECTION_OUTGOING = "outgoing";
    String VALUE_CALL_DIRECTION_INCOMING = "incoming";

    String VALUE_HANGUP_REASON_USER = "user";
    String VALUE_HANGUP_REASON_REMOTE = "REMOTE";
}
