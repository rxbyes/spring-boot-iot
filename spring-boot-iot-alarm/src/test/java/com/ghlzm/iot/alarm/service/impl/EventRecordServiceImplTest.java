package com.ghlzm.iot.alarm.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.alarm.mapper.EventWorkOrderMapper;
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
class EventRecordServiceImplTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(EventRecordServiceImpl.class);
    private final Level originalLevel = logger.getLevel();

    @Mock
    private EventWorkOrderMapper eventWorkOrderMapper;
    @Mock
    private InAppMessagePublisher inAppMessagePublisher;

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLevel);
        logger.detachAndStopAllAppenders();
    }

    @Test
    void shouldPublishDispatchNoticeForReceiveUser() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        EventRecordServiceImpl service = spy(new EventRecordServiceImpl(eventWorkOrderMapper, inAppMessagePublisher));
        EventRecord event = new EventRecord();
        event.setId(101L);
        event.setEventCode("EV-101");
        event.setDeviceCode("DEV-101");
        event.setRiskLevel("high");
        event.setStatus(0);
        doReturn(event).when(service).getRequiredById(101L);
        doReturn(true).when(service).updateById(any(EventRecord.class));

        service.dispatchEvent(101L, 11L, 22L);

        ArgumentCaptor<InAppMessagePublishCommand> captor = ArgumentCaptor.forClass(InAppMessagePublishCommand.class);
        verify(eventWorkOrderMapper).insert(any(EventWorkOrder.class));
        verify(inAppMessagePublisher).publish(captor.capture());

        InAppMessagePublishCommand command = captor.getValue();
        assertEquals("event_dispatch", command.getSourceType());
        assertEquals("101", command.getSourceId());
        assertEquals("/event-disposal", command.getRelatedPath());
        assertEquals(22L, command.getTargetUserIds().get(0));
        assertEquals("business", command.getMessageType());

        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("event=\"event_lifecycle\""));
        assertTrue(message.contains("action=\"dispatch\""));
        assertTrue(message.contains("result=\"success\""));
        assertTrue(message.contains("eventId=101"));
        assertTrue(message.contains("deviceCode=\"DEV-101\""));
        assertTrue(message.contains("receiveUser=22"));
    }
}
