package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.EventWorkOrder;
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
class EventWorkOrderServiceImplTest {

    @Mock
    private InAppMessagePublisher inAppMessagePublisher;

    @Test
    void shouldPublishCompletionNoticeForAssignUser() {
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

        service.completeProcessing(201L, "已完成", "[]");

        ArgumentCaptor<InAppMessagePublishCommand> captor = ArgumentCaptor.forClass(InAppMessagePublishCommand.class);
        verify(inAppMessagePublisher).publish(captor.capture());

        InAppMessagePublishCommand command = captor.getValue();
        assertEquals("work_order", command.getSourceType());
        assertEquals("201:complete", command.getSourceId());
        assertEquals(12L, command.getTargetUserIds().get(0));
        assertEquals("high", command.getPriority());
    }
}
