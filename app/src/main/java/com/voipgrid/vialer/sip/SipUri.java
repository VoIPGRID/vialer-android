package com.voipgrid.vialer.sip;

import android.content.Context;
import android.net.Uri;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.util.PhoneNumberUtils;

/**
 * Created by eltjo on 02/09/15.
 */
public class SipUri {

    /**
     * Build Sip Uri
     *
     * @param protocol
     * @param number
     * @param host
     * @return
     */
    public static Uri build(String protocol, String number, String host) {
        return Uri.parse(String.format(
                "%s%s@%s",
                protocol,
                PhoneNumberUtils.format(number),
                host
        ));
    }

    /**
     * Build Sip Uri
     *
     * @param context
     * @param number
     * @return
     */
    public static Uri build(Context context, String number) {
        return build(context.getString(R.string.sip_protocol),
                number,
                context.getString(R.string.sip_host)
        );
    }

    public static Uri buildRegistrar(Context context) {
        return Uri.parse(
                context.getString(R.string.sip_protocol) + context.getString(R.string.sip_host)
        );
    }
}
