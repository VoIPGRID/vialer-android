package com.voipgrid.vialer.sip;

import java.util.HashMap;

public class CodecPriorityMap extends HashMap<String, Short> {

    public static final short CODEC_DISABLED = (short) 0;
    public static final short CODEC_PRIORITY_MAX = (short) 255;

    private CodecPriorityMap() {
    }

    /**
     * Return the standard codec priority map.
     *
     * @return
     */
    public static CodecPriorityMap get() {
        CodecPriorityMap codecPriorityMap = new CodecPriorityMap();
        codecPriorityMap.put("ilbc/8000", CODEC_PRIORITY_MAX);
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
