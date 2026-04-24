package com.ghlzm.iot.message.service.capability;

import com.ghlzm.iot.device.service.CommandRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CapabilityFeedbackHandlerTest {

    @Mock
    private CommandRecordService commandRecordService;

    @Test
    void handleShouldMarkSuccessByMsgId() {
        CapabilityFeedbackParser parser = new CapabilityFeedbackParser();
        CapabilityFeedbackHandler handler = new CapabilityFeedbackHandler(parser, commandRecordService);

        CapabilityFeedback feedback = handler.handle("/broadcast/demo-device-01/feedback", "$cmd=stop&result=sucd&message=done&msgid=1776999000000");

        assertTrue(feedback.valid());
        verify(commandRecordService).markSuccessByCommandId(
                eq("1776999000000"),
                eq("$cmd=stop&result=sucd&message=done&msgid=1776999000000"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void handleShouldMarkFailedByMsgId() {
        CapabilityFeedbackParser parser = new CapabilityFeedbackParser();
        CapabilityFeedbackHandler handler = new CapabilityFeedbackHandler(parser, commandRecordService);

        CapabilityFeedback feedback = handler.handle("/broadcast/demo-device-01/feedback", "$cmd=stop&result=fail&message=boom&msgid=1776999000001");

        assertTrue(feedback.valid());
        verify(commandRecordService).markFailedByCommandId(
                eq("1776999000001"),
                eq("$cmd=stop&result=fail&message=boom&msgid=1776999000001"),
                eq("boom"),
                any(LocalDateTime.class)
        );
    }
}
