package com.ghlzm.iot.alarm.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.framework.notification.InAppMessagePublishCommand;
import com.ghlzm.iot.framework.notification.InAppMessagePublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventWorkOrderServiceImplTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(EventWorkOrderServiceImpl.class);
    private final Level originalLevel = logger.getLevel();

    @Mock
    private InAppMessagePublisher inAppMessagePublisher;

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLevel);
        logger.detachAndStopAllAppenders();
    }

    @Test
    void shouldPublishCompletionNoticeForAssignUser() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        EventWorkOrderServiceImpl service = spy(new EventWorkOrderServiceImpl(inAppMessagePublisher));
        EventWorkOrder workOrder = new EventWorkOrder();
        workOrder.setId(201L);
        workOrder.setEventId(301L);
        workOrder.setEventCode("EV-301");
        workOrder.setWorkOrderCode("WO-201");
        workOrder.setAssignUser(12L);
        workOrder.setReceiveUser(22L);
        workOrder.setStatus(2);
        doReturn(workOrder).when(service).getById(201L);
        doReturn(true).when(service).updateById(any(EventWorkOrder.class));

        service.completeProcessing(201L, "processed", "[]");

        ArgumentCaptor<InAppMessagePublishCommand> captor = ArgumentCaptor.forClass(InAppMessagePublishCommand.class);
        verify(inAppMessagePublisher).publish(captor.capture());

        InAppMessagePublishCommand command = captor.getValue();
        assertEquals("work_order", command.getSourceType());
        assertEquals("201:complete", command.getSourceId());
        assertEquals(12L, command.getTargetUserIds().get(0));
        assertEquals("high", command.getPriority());

        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("event=\"work_order_lifecycle\""));
        assertTrue(message.contains("action=\"complete\""));
        assertTrue(message.contains("result=\"success\""));
        assertTrue(message.contains("workOrderId=201"));
        assertTrue(message.contains("eventCode=\"EV-301\""));
        assertTrue(message.contains("assignUser=12"));
    }
}
