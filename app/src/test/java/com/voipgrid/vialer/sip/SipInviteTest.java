package com.voipgrid.vialer.sip;

import static org.junit.Assert.*;

import org.junit.Test;

public class SipInviteTest {
    
    private static final String INVITE_PACKET_WITHOUT_REMOTE_PARTY_ID = "INVITE sip:111111" 
            + "@111.1111.111.111:46571;transport=TLS;ob SIP/2.0\n"
            + "    Record-Route: <sip:111.1111.111.111:5061;transport=tls;r2=on;lr;ftag=as6ed4bb38>\n"
            + "    Record-Route: <sip:111.1111.111.111:5063;r2=on;lr;ftag=as6ed4bb38>\n"
            + "    Record-Route: <sip:111.1111.111.111;lr;ftag=as6ed4bb38;cc=34333034;" 
            + "eu=7369703a3139352e3335asdsd31393a353036333b6c72;mc=from-dutch;" 
            + "did=219.0f84e5e>\n"
            + "    Via: SIP/2.0/TLS 111.1111.111.111:5061;" 
            + "branch=z9hG4bK2e7b.5028b83612a4f99677ea593a5668050f.0\n"
            + "    Via: SIP/2.0/UDP 111.1111.111.111:5060;branch=z9hG4bK2e7b.c2c75f.0\n"
            + "    Via: SIP/2.0/UDP 111.1111.111.111:5060;received=111.1111.111.111;" 
            + "branch=z9hG4bK4fb52963;rport=5060\n"
            + "    Max-Forwards: 70\n"
            + "    From: \"user\" <sip:247@sip.nl>;tag=as6ed4bb38\n"
            + "    To: <sip:169710067@sip.nl:5060>\n"
            + "    Contact: <sip:247@111.1111.111.111:5060>\n"
            + "    Call-ID: 58fbca094582108b5de681626078a187@sip.nl\n"
            + "    CSeq: 102 INVITE\n"
            + "    User-Agent: VGUA-13vg9~d9\n"
            + "    Date: Thu, 20 Dec 2018 14:04:03 GMT\n"
            + "    Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO, " 
            + "PUBLISH, MESSAGE\n"
            + "    Supported: replaces, timer\n"
            + "    Content-Type: application/sdp\n"
            + "    Content-Length: 414\n"
            + "    \n"
            + "    v=0\n"
            + "    o=- 171813371 171813371 IN IP4 111.1111.111.111\n"
            + "    s=VGUA-13vg9~d9\n"
            + "    c=IN IP4 111.1111.111.111\n"
            + "    t=0 0\n"
            + "    m=audio 18208 RTP/SAVP 97 8 9 18 101\n"
            + "    a=crypto:1 AES_CM_128_HMAC_SHA1_80 " 
            + "inline:SY2LsdC6m68cE4cEIpShMMDutVdheX8ZvK02Q4t6\n"
            + "    a=rtpmap:97 iLBC/8000\n"
            + "    a=rtpmap:8 PCMA/8000\n"
            + "    a=rtpmap:9 G722/8000\n"
            + "    a=rtpmap:18 G729/8000\n"
            + "    a=fmtp:18 annexb=no\n"
            + "    a=rtpmap:101 telephone-event/8000\n"
            + "    a=fmtp:101 0-16\n"
            + "    a=maxptime:150\n"
            + "    a=sendrecv";
    
    private static final String INVITE_PACKET_WITH_REMOTE_PARTY_ID = "INVITE sip:169710067" 
            + "@111.1111.111.111:47923;transport=TLS;ob SIP/2.0\n"
            + "    Record-Route: <sip:111.1111.111.111:5061;transport=tls;r2=on;lr;ftag=as6ea2452c>\n"
            + "    Record-Route: <sip:111.1111.111.111:5063;r2=on;lr;ftag=as6ea2452c>\n"
            + "    Record-Route: <sip:111.1111.111.111;lr;ftag=as6ea2452c;cc=34333034;" 
            + "eu=7369703a3139352e33352e3131342e3131393a353036333b6c72;mc=rpid-dutch;" 
            + "did=adb.51d33734>\n"
            + "    Via: SIP/2.0/TLS 111.1111.111.111:5061;" 
            + "branch=z9hG4bK9aa6.160c30c04f8f4437631037b8349be1f3.0\n"
            + "    Via: SIP/2.0/UDP 111.1111.111.111:5060;branch=z9hG4bK9aa6.15b393a2.0\n"
            + "    Via: SIP/2.0/UDP 111.1111.111.111:5060;received=111.1111.111.111;" 
            + "branch=z9hG4bK4852c900;rport=5060\n"
            + "    Max-Forwards: 70\n"
            + "    From: \"user\" <sip:169710067@sip.nl>;tag=as6ea2452c\n"
            + "    To: <sip:1111112@sip.nl:5060>\n"
            + "    Contact: <sip:11111@111.1111.111.111:5060>\n"
            + "    Call-ID: 7ca4ab6b0ff3f6cc1a2143304b123394@sip.nl\n"
            + "    CSeq: 102 INVITE\n"
            + "    User-Agent: VGUA-13vg9~d9\n"
            + "    Date: Thu, 20 Dec 2018 14:05:00 GMT\n"
            + "    Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO, " 
            + "PUBLISH, MESSAGE\n"
            + "    Supported: replaces, timer\n"
            + "    Content-Type: application/sdp\n"
            + "    Content-Length: 414\n"
            + "    Remote-Party-ID: \"user\" <sip:247@sip.nl>;privacy=off;screen=yes\n"
            + "    \n"
            + "    v=0\n"
            + "    o=- 177642988 177642988 IN IP4 111.1111.111.111\n"
            + "    s=VGUA-13vg9~d9\n"
            + "    c=IN IP4 111.1111.111.111\n"
            + "    t=0 0\n"
            + "    m=audio 13958 RTP/SAVP 97 8 9 18 101\n"
            + "    a=crypto:1 AES_CM_128_HMAC_SHA1_80 " 
            + "inline:AOC3tNyHXrtNVITL5dKjUI8vQmB/c3hSrVqJ4XI1\n"
            + "    a=rtpmap:97 iLBC/8000\n"
            + "    a=rtpmap:8 PCMA/8000\n"
            + "    a=rtpmap:9 G722/8000\n"
            + "    a=rtpmap:18 G729/8000\n"
            + "    a=fmtp:18 annexb=no\n"
            + "    a=rtpmap:101 telephone-event/8000\n"
            + "    a=fmtp:101 0-16\n"
            + "    a=maxptime:150\n"
            + "    a=sendrecv";

    @Test
    public void it_checks_if_an_invite_has_a_remote_party_id() {
        assertTrue(new SipInvite(INVITE_PACKET_WITH_REMOTE_PARTY_ID).hasRemotePartyId());
        assertFalse(new SipInvite(INVITE_PACKET_WITHOUT_REMOTE_PARTY_ID).hasRemotePartyId());
    }

    @Test
    public void it_extracts_the_correct_information_from_the_header_fields() {
        assertEquals("user", new SipInvite(INVITE_PACKET_WITH_REMOTE_PARTY_ID).getRemotePartyId().name);
        assertEquals("247", new SipInvite(INVITE_PACKET_WITH_REMOTE_PARTY_ID).getRemotePartyId().number);
    }
}