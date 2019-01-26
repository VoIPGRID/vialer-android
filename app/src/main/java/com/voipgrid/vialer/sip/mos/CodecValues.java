package com.voipgrid.vialer.sip.mos;

import org.pjsip.pjsua2.StreamInfo;

import java.util.ArrayList;

class CodecValues extends ArrayList<CodecValues.CodecValue> {

    private CodecValues() {

    }

    public static CodecValues get() {
        CodecValues codecValues = new CodecValues();
        codecValues.add(new CodecValue("PCMA", 0.0, 4.3, 0.125));
        codecValues.add(new CodecValue("PCMU", 0.0, 4.3, 0.125));
        codecValues.add(new CodecValue("G722", 0.0, 4.3, 4.0));
        codecValues.add(new CodecValue("iLBC", 32.0, 50.0, 25.0));
        codecValues.add(new CodecValue("Speex", 18.0, 21, 30));
        codecValues.add(new CodecValue("GSM", 28, 34, 25));
        codecValues.add(new CodecValue("opus", 11, 12, 20));
        return codecValues;
    }

    /**
     * Find the CodecValue for the current stream.
     *
     * @param streamInfo
     * @return
     */
    public static CodecValue getCodecValue(StreamInfo streamInfo) {
        for (CodecValue entry : get()) {
            String codecName = entry.getName();

            if (streamInfo.getCodecName().startsWith(codecName)) {
                entry.setPacketSize(streamInfo.getAudCodecParam().getInfo().getFrameLen());
                return entry;
            }
        }

        return null;
    }

    static final class CodecValue {
        private final String name;
        private double impairment;
        private double bpl;
        private double frameSize;
        private double packetSize;

        CodecValue(String name, double impairment, double bpl, double frameSize) {
            this.name = name;
            this.impairment = impairment;
            this.bpl = bpl;
            this.frameSize = frameSize;
        }

        double getImpairment() {
            return impairment;
        }

        double getBpl() {
            return bpl;
        }

        double getFrameSize() {
            return frameSize;
        }

        double getPacketSize() {
            return packetSize;
        }

        void setPacketSize(double packetSize) {
            this.packetSize = packetSize;
        }

        String getName() {
            return name;
        }
    }
}
