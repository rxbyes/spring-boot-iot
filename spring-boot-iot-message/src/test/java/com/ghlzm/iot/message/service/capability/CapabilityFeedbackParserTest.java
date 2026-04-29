package com.ghlzm.iot.message.service.capability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityFeedbackParserTest {

    private final CapabilityFeedbackParser parser = new CapabilityFeedbackParser();

    @Test
    void parseShouldHandleSuccessFeedback() {
        CapabilityFeedback feedback = parser.parse("$cmd=stop&result=sucd&message=%E5%81%9C%E6%AD%A2%E6%92%AD%E6%94%BE&msgid=1776999000000");

        assertTrue(feedback.valid());
        assertEquals("stop", feedback.cmd());
        assertEquals("sucd", feedback.result());
        assertEquals("1776999000000", feedback.msgid());
        assertEquals("停止播放", feedback.message());
    }

    @Test
    void parseShouldReturnInvalidForMalformedPayload() {
        CapabilityFeedback feedback = parser.parse("cmd=stop&message=bad");

        assertFalse(feedback.valid());
        assertEquals("反馈报文缺少 cmd/result/msgid", feedback.invalidReason());
    }
}
