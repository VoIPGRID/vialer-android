package com.voipgrid.vialer.sip;

import android.content.Context;
import android.net.Uri;

import com.voipgrid.vialer.R;

/**
 * Class for creating the correct sip addresses.
 */
public class SipUri {
    /**
     * A complete sip address including the sip account id.
     *
     * @param context Context reference.
     * @param sipAccount the sip account id.
     *
     * @return String with sip: in front and the sip account id example: sip:42@test.url.com
     */
    public static String sipAddress(Context context, String sipAccount) {
        return prependSIPUri(
                context,
                sipAccount + "@" + context.getString(R.string.sip_host)
        );
    }

    /**
     * Create a URI version of the sip address.
     *
     * @param context Context reference.
     * @param sipAccount the sip account id.
     *
     * @return URI class with the sip address.
     */
    public static Uri sipAddressUri(Context context, String sipAccount) {
        return Uri.parse(sipAddress(context, sipAccount));
    }

    /**
     * Try to prepend the sip: in front of the given string
     *
     * @param context Context reference.
     * @param uri the uri to prepend sip in front.
     *
     * @return String with sip: in front of it example. sip:test.url.com.
     */
    public static String prependSIPUri(Context context, String uri) {
        String prependedSipUri = uri;
        String sipProtocol = context.getString(R.string.sip_protocol);

        if (!prependedSipUri.startsWith(sipProtocol)) {
            prependedSipUri = sipProtocol + prependedSipUri;
        }

        return prependedSipUri;
    }
}
