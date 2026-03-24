package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.framework.notification.InAppMessagePublishCommand;
import com.ghlzm.iot.framework.notification.InAppMessagePublishResult;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppMessagePublisherImplTest {

    @Mock
    private InAppMessageMapper inAppMessageMapper;
    @Mock
    private SystemContentSchemaSupport systemContentSchemaSupport;

    private InAppMessagePublisherImpl publisher;

    @BeforeEach
    void setUp() {
        publisher = new InAppMessagePublisherImpl(inAppMessageMapper, systemContentSchemaSupport);
    }

    @Test
    void shouldReuseExistingMessageWhenDedupKeyHits() {
        InAppMessage existing = new InAppMessage();
        existing.setId(1001L);
        existing.setDedupKey("dedup-1");
        when(inAppMessageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        InAppMessagePublishResult result = publisher.publish(InAppMessagePublishCommand.builder()
                .messageType("business")
                .priority("high")
                .title("事件已派工")
                .summary("请及时处理")
                .content("正文")
                .targetType("user")
                .targetUserIds(List.of(2L))
                .sourceType("event_dispatch")
                .sourceId("event-1001")
                .build());

        assertEquals(1001L, result.getMessageId());
        assertTrue(result.isDedupKeyHit());
        assertFalse(result.isCreated());
    }

    @Test
    void shouldApplyDefaultExpireTimeForSystemError() {
        ArgumentCaptor<InAppMessage> captor = ArgumentCaptor.forClass(InAppMessage.class);
        when(inAppMessageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Date publishTime = new Date();
        InAppMessagePublishResult result = publisher.publish(InAppMessagePublishCommand.builder()
                .messageType("error")
                .priority("high")
                .title("后台异常待排查")
                .summary("异常摘要")
                .content("异常正文")
                .targetType("role")
                .targetRoleCodes(List.of("OPS_STAFF", "DEVELOPER_STAFF"))
                .sourceType("system_error")
                .sourceId("trace-1001")
                .publishTime(publishTime)
                .build());

        verify(inAppMessageMapper).insert(captor.capture());
        InAppMessage message = captor.getValue();
        assertEquals("system_error", message.getSourceType());
        assertNotNull(message.getDedupKey());
        assertNotNull(message.getExpireTime());
        assertTrue(message.getExpireTime().after(publishTime));
        assertTrue(result.isCreated());
    }
}
