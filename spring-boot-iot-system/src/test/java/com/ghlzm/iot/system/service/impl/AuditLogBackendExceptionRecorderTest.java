package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.framework.notification.InAppMessagePublishCommand;
import com.ghlzm.iot.framework.notification.InAppMessagePublisher;
import com.ghlzm.iot.framework.observability.BackendExceptionEvent;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.service.SystemErrorNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogBackendExceptionRecorderTest {

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private InAppMessagePublisher inAppMessagePublisher;
    @Mock
    private SystemErrorNotificationService systemErrorNotificationService;

    @Test
    void shouldPublishInAppMessageForBackendException() {
        AuditLogBackendExceptionRecorder recorder = new AuditLogBackendExceptionRecorder(
                auditLogService,
                inAppMessagePublisher,
                systemErrorNotificationService
        );

        recorder.record(new BackendExceptionEvent(
                "alarm",
                "dispatchEvent",
                "/api/alarm/event/dispatch",
                "POST",
                Map.of("traceId", "trace-2001"),
                new IllegalStateException("派工失败")
        ));

        ArgumentCaptor<InAppMessagePublishCommand> captor = ArgumentCaptor.forClass(InAppMessagePublishCommand.class);
        verify(auditLogService).addLog(any());
        verify(inAppMessagePublisher).publish(captor.capture());
        verify(systemErrorNotificationService).notifySystemError(any(), any());

        InAppMessagePublishCommand command = captor.getValue();
        assertEquals("error", command.getMessageType());
        assertEquals("high", command.getPriority());
        assertEquals("system_error", command.getSourceType());
        assertEquals("/system-log", command.getRelatedPath());
        assertEquals("trace-2001", command.getSourceId());
        assertTrue(command.getTargetRoleCodes().contains("OPS_STAFF"));
        assertTrue(command.getTargetRoleCodes().contains("DEVELOPER_STAFF"));
        assertTrue(command.getTargetRoleCodes().contains("SUPER_ADMIN"));
    }
}
