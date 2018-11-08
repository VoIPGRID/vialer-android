package com.voipgrid.vialer.logging.formatting;

import static org.junit.Assert.assertFalse;

import com.voipgrid.vialer.logging.formatting.formatters.PayloadAnonymizer;
import com.voipgrid.vialer.logging.formatting.formatters.SipLogAnonymizer;
import com.voipgrid.vialer.sip.SipService;

import org.junit.Test;


public class SipLogAnonymizerTest {

    private String dummySipLog =
            "SipService           pjsua_core.c  .RX 1482 bytes Request msg INVITE/cseq=102 (rdata0x7c1607db40) from TLS 192.168.1.1:5061:\n" +
                    "                                                  INVITE sip:198710033@217.21.195.34:39824;transport=TLS;ob SIP/2.0\n" +
                    "                                                  Record-Route: <sip:192.168.1.1:5061;transport=tls;r2=on;lr;ftag=as28c59a87>\n" +
                    "                                                  Record-Route: <sip:192.168.1.1:5063;r2=on;lr;ftag=as28c59a87>\n" +
                    "                                                  Record-Route: <sip:192.168.1.1;lr;ftag=as28c59a87;cc=34333034;eu=7369703a3139352e33352e3131352e3131393a353036333b72323d6f6e3b6c72;mc=from-dutch;did=fc1.54987cc6>\n" +
                    "                                                  Via: SIP/2.0/TLS 192.168.1.1:5061;branch=z9hG4bKf906.717ee751.0\n" +
                    "                                                  Via: SIP/2.0/UDP 192.168.1.1:5060;branch=z9hG4bKf906.40b70b36.0\n" +
                    "                                                  Via: SIP/2.0/UDP 10.0.0.1:5060;received=10.0.0.1;branch=z9hG4bK41167a5a;rport=5060\n" +
                    "                                                  Max-Forwards: 70\n" +
                    "                                                  From: <sip:0102003041@voipgrid.nl>;tag=as28c59a87\n" +
                    "                                                  To: <sip:198710033@voipgrid.nl:5060>\n" +
                    "                                                  Contact: <sip:0102003041@10.0.0.1:5060>\n" +
                    "                                                  Call-ID: 34eb776d3b733462150cc4b23516245a@voipgrid.nl\n" +
                    "                                                  CSeq: 102 INVITE\n" +
                    "                                                  User-Agent: VGUA-11vg18\n" +
                    "                                                  Date: Mon, 29 Jan 2018 12:41:24 GMT\n" +
                    "                                                  Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO, PUBLISH, MESSAGE\n" +
                    "                                                  Supported: replaces, timer\n" +
                    "                                                  Content-Type: application/sdp\n" +
                    "                                                  Content-Length: 408";
    @Test
    public void it_will_not_anonymize_unless_it_is_a_sip_log_message() {
        PayloadAnonymizer sipLogAnonymizer = new PayloadAnonymizer();

        assertFalse(sipLogAnonymizer.shouldFormat("TAG", dummySipLog));
    }

    @Test
    public void it_anonymizes_sip_logs() {
        SipLogAnonymizer sipLogAnonymizer = new SipLogAnonymizer();


        String result = sipLogAnonymizer.format(SipService.class.getSimpleName(),dummySipLog);
        assertFalse(result.contains("0102003041@voipgrid.nl"));
        assertFalse(result.contains("198710033@voipgrid.nl"));
        assertFalse(result.contains("0102003041@10.0.0.1"));
    }
}