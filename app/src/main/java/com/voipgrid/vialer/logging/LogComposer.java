package com.voipgrid.vialer.logging;

import com.voipgrid.vialer.logging.tracing.CallerLocator;

public class LogComposer {

    private DeviceInformation mDeviceInformation;
    private String mIdentifier;
    private String mAppVersion;
    private CallerLocator mCallerLocator;

    public LogComposer(DeviceInformation deviceInformation, String identifier, String appVersion) {
        mDeviceInformation = deviceInformation;
        mIdentifier = identifier;
        mAppVersion = appVersion;
    }

    /**
     * Function to format a message to include severity level and identifier.
     *
     * @param tag Tag that indicates the severity.
     * @param message
     * @return String The composed and formatted log message.
     */
    public String compose(String level, String tag, String message) {
        return level + " " + mIdentifier + " - " + mAppVersion + " - " + mDeviceInformation.getDeviceName()  + " - " + mDeviceInformation.getConnectionType() + " - " + tag + " - " + message;
    }
}
