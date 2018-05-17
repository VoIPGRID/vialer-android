package com.voipgrid.vialer.test.logging.formatting;

import static org.junit.Assert.assertFalse;

import com.voipgrid.vialer.fcm.FcmMessagingService;
import com.voipgrid.vialer.logging.formatting.formatters.PayloadAnonymizer;

import org.junit.Test;

public class PayloadAnonymizerTest {
    private String dummyPayloadLog = "FcmMessagingService Payload: {message_start_time=1.517230273029763E9, caller_id=, phonenumber=+41501109999, type=call, unique_key=209779d101c25d5d486304900cc8e75e, response_api=https://vialerpush.voipgrid.nl/api/call-response/}";

    @Test
    public void it_will_not_anonymize_unless_it_is_a_payload_log_message() {
        PayloadAnonymizer payloadAnonymizer = new PayloadAnonymizer();

        boolean result = payloadAnonymizer.shouldFormat("TAG", dummyPayloadLog);
        assertFalse(result);
    }

    @Test
    public void it_anonymizes_payload_logs() {
        PayloadAnonymizer payloadAnonymizer = new PayloadAnonymizer();

        String result = payloadAnonymizer.format(FcmMessagingService.class.getSimpleName(), dummyPayloadLog);
        assertFalse(result.contains("41501109999"));
    }
}
