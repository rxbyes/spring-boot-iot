package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeAttemptLogMapper;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeLogMapper;
import com.ghlzm.iot.system.service.InAppMessageBridgeQueryService;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeLogVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppMessageBridgeQueryServiceImplTest {

    @Mock
    private InAppMessageBridgeLogMapper inAppMessageBridgeLogMapper;
    @Mock
    private InAppMessageBridgeAttemptLogMapper inAppMessageBridgeAttemptLogMapper;
    @Mock
    private SystemContentSchemaSupport systemContentSchemaSupport;

    private InAppMessageBridgeQueryService service;

    @BeforeEach
    void setUp() {
        service = new InAppMessageBridgeQueryServiceImpl(
                inAppMessageBridgeLogMapper,
                inAppMessageBridgeAttemptLogMapper,
                systemContentSchemaSupport
        );
    }

    @Test
    void shouldUseRecentSevenDaysWhenNoRangeProvided() {
        when(inAppMessageBridgeLogMapper.listBridgeLogsForStats(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        InAppMessageBridgeStatsVO stats = service.getBridgeStats(null, null, null, null, null, null, null);

        ArgumentCaptor<Date> startCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> endCaptor = ArgumentCaptor.forClass(Date.class);
        verify(inAppMessageBridgeLogMapper).listBridgeLogsForStats(
                startCaptor.capture(),
                endCaptor.capture(),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null)
        );
        assertNotNull(startCaptor.getValue());
        assertNotNull(endCaptor.getValue());
        assertTrue(endCaptor.getValue().getTime() >= startCaptor.getValue().getTime());
        assertNotNull(stats.getStartTime());
        assertNotNull(stats.getEndTime());
    }

    @Test
    void shouldThrowWhenStartTimeIsAfterEndTime() {
        Date startTime = new Date(2000L);
        Date endTime = new Date(1000L);

        BizException exception = assertThrows(BizException.class, () ->
                service.getBridgeStats(startTime, endTime, null, null, null, null, null));

        assertEquals("开始时间不能晚于结束时间", exception.getMessage());
    }

    @Test
    void shouldAggregateStatsByChannelAndSourceType() {
        when(inAppMessageBridgeLogMapper.listBridgeLogsForStats(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        bridgeLog(1L, "system_error", "ops-webhook", "运维Webhook", "webhook", 1, 2, "2026-03-22 09:00:00"),
                        bridgeLog(2L, "work_order", "ops-webhook", "运维Webhook", "webhook", 0, 1, "2026-03-22 10:00:00"),
                        bridgeLog(3L, "work_order", "ops-wechat", null, "wechat", 1, 3, "2026-03-21 11:00:00")
                ));

        InAppMessageBridgeStatsVO stats = service.getBridgeStats(null, null, null, null, null, null, null);

        assertEquals(3L, stats.getTotalBridgeCount());
        assertEquals(2L, stats.getSuccessCount());
        assertEquals(1L, stats.getPendingRetryCount());
        assertEquals(6L, stats.getTotalAttemptCount());
        assertEquals(2, stats.getTrend().size());
        assertEquals(2, stats.getChannelBuckets().size());
        assertEquals("运维Webhook", stats.getChannelBuckets().get(0).getLabel());
        assertEquals(2L, stats.getChannelBuckets().get(0).getBridgeCount());
        assertEquals("工单状态", stats.getSourceTypeBuckets().get(0).getLabel());
        assertEquals(2L, stats.getSourceTypeBuckets().get(0).getBridgeCount());
    }

    @Test
    void shouldPageLogsAndReturnAttempts() {
        when(inAppMessageBridgeLogMapper.countBridgeLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);
        when(inAppMessageBridgeLogMapper.pageBridgeLogs(any(), any(), any(), any(), any(), any(), any(), eq(0L), eq(10L)))
                .thenReturn(List.of(bridgeLog(10L, "manual", "ops-webhook", "运维Webhook", "webhook", 0, 2, "2026-03-22 11:00:00")));
        when(inAppMessageBridgeAttemptLogMapper.listAttemptsByBridgeLogId(10L))
                .thenReturn(List.of(attempt(2, 0), attempt(1, 1)));

        PageResult<InAppMessageBridgeLogVO> pageResult = service.pageBridgeLogs(
                null,
                null,
                "system",
                "manual",
                "high",
                "ops-webhook",
                0,
                1L,
                10L
        );
        List<InAppMessageBridgeAttemptVO> attempts = service.listBridgeAttempts(10L);

        assertEquals(1L, pageResult.getTotal());
        assertEquals(1, pageResult.getRecords().size());
        assertEquals("ops-webhook", pageResult.getRecords().get(0).getChannelCode());
        assertEquals(2, attempts.size());
        assertEquals(2, attempts.get(0).getAttemptNo());
        verify(inAppMessageBridgeAttemptLogMapper).listAttemptsByBridgeLogId(10L);
    }

    private InAppMessageBridgeLogVO bridgeLog(Long id,
                                              String sourceType,
                                              String channelCode,
                                              String channelName,
                                              String channelType,
                                              int bridgeStatus,
                                              int attemptCount,
                                              String lastAttemptTime) {
        InAppMessageBridgeLogVO log = new InAppMessageBridgeLogVO();
        log.setId(id);
        log.setMessageId(id + 100L);
        log.setMessageType("system");
        log.setPriority("high");
        log.setTitle("桥接记录-" + id);
        log.setSourceType(sourceType);
        log.setChannelCode(channelCode);
        log.setChannelName(channelName);
        log.setChannelType(channelType);
        log.setBridgeScene("in_app_unread_bridge");
        log.setBridgeStatus(bridgeStatus);
        log.setUnreadCount(3);
        log.setAttemptCount(attemptCount);
        log.setLastAttemptTime(lastAttemptTime);
        return log;
    }

    private InAppMessageBridgeAttemptVO attempt(int attemptNo, int bridgeStatus) {
        InAppMessageBridgeAttemptVO attempt = new InAppMessageBridgeAttemptVO();
        attempt.setId((long) attemptNo);
        attempt.setBridgeLogId(10L);
        attempt.setAttemptNo(attemptNo);
        attempt.setBridgeStatus(bridgeStatus);
        attempt.setAttemptTime("2026-03-22 11:00:00");
        return attempt;
    }
}
