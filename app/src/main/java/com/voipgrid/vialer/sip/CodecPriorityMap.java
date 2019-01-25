package com.voipgrid.vialer.sip;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;

import java.util.HashMap;

public class CodecPriorityMap extends HashMap<String, Short> {

    private static final short CODEC_DISABLED = (short) 0;
    private static final short CODEC_PRIORITY_MAX = (short) 255;

    private CodecPriorityMap() {
    }

    /**
     * Return the standard codec priority map.
     *
     * @return
     */
    public static CodecPriorityMap get() {
        Preferences preferences = VialerApplication.get().component().getPreferences();
        CodecPriorityMap codecPriorityMap = new CodecPriorityMap();
        codecPriorityMap.put("opus/48000", preferences.getAudioCodec() == Preferences.AUDIO_CODEC_OPUS ? CODEC_PRIORITY_MAX : CODEC_DISABLED);
        codecPriorityMap.put("ilbc/8000", preferences.getAudioCodec() == Preferences.AUDIO_CODEC_ILBC ? CODEC_PRIORITY_MAX : CODEC_DISABLED);
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
