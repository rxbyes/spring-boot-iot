package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import com.ghlzm.iot.alarm.mapper.EventWorkOrderMapper;
import com.ghlzm.iot.framework.notification.InAppMessagePublishCommand;
import com.ghlzm.iot.framework.notification.InAppMessagePublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventRecordServiceImplTest {

    @Mock
    private EventWorkOrderMapper eventWorkOrderMapper;
    @Mock
    private InAppMessagePublisher inAppMessagePublisher;

    @Test
    void shouldPublishDispatchNoticeForReceiveUser() {
        EventRecordServiceImpl service = spy(new EventRecordServiceImpl(eventWorkOrderMapper, inAppMessagePublisher));
        EventRecord event = new EventRecord();
        event.setId(101L);
        event.setEventCode("EV-101");
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
    }
}
