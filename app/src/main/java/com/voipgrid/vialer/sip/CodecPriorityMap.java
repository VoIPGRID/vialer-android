package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.User;
import com.voipgrid.vialer.persistence.VoipSettings;

import java.util.HashMap;

public class CodecPriorityMap extends HashMap<String, Short> {

    static final short CODEC_DISABLED = (short) 0;
    static final short CODEC_PRIORITY_MAX = (short) 255;

    private CodecPriorityMap() {
    }

    /**
     * Return the standard codec priority map.
     *
     * @return
     */
    public static CodecPriorityMap get() {
        VoipSettings voipSettings = User.voip;
        CodecPriorityMap codecPriorityMap = new CodecPriorityMap();
        codecPriorityMap.put("opus/48000", voipSettings.getAudioCodec() == VoipSettings.AudioCodec.OPUS ? CODEC_PRIORITY_MAX : CODEC_DISABLED);
        codecPriorityMap.put("ilbc/8000", voipSettings.getAudioCodec() == VoipSettings.AudioCodec.iLBC ? CODEC_PRIORITY_MAX : CODEC_DISABLED);
        return codecPriorityMap;
    }

    /**
     * Searches and normalizes the codec priority mapping and attempts to find the mapped priority.
     *
     * @param codecId
     * @return Short The codec's priority.
     */
    public Short findCodecPriority(String codecId) {
        for(String codec : this.keySet()) {
            if(codecId.toLowerCase().contains(codec.toLowerCase())) {
                return this.get(codec);
            }
        }

        return null;
    }
}
