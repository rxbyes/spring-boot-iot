package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.InAppMessageRead;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import com.ghlzm.iot.system.mapper.InAppMessageReadMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppMessageServiceImplTest {

    @Mock
    private InAppMessageMapper inAppMessageMapper;
    @Mock
    private InAppMessageReadMapper inAppMessageReadMapper;
    @Mock
    private PermissionService permissionService;

    private InAppMessageServiceImpl inAppMessageService;

    @BeforeEach
    void setUp() {
        inAppMessageService = new InAppMessageServiceImpl(inAppMessageMapper, inAppMessageReadMapper, permissionService);
    }

    @Test
    void shouldAggregateUnreadStatsByAccessibleMessageType() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext("BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(101L, "system", "high", "all", null, null),
                message(102L, "business", "medium", "role", "BUSINESS_STAFF", null),
                message(103L, "error", "critical", "user", null, "2"),
                message(104L, "business", "low", "role", "OPS_STAFF", null)
        ));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(readRecord(102L, userId)));

        InAppMessageUnreadStatsVO stats = inAppMessageService.getMyUnreadStats(userId);

        assertEquals(2L, stats.getTotalUnreadCount());
        assertEquals(1L, stats.getSystemUnreadCount());
        assertEquals(0L, stats.getBusinessUnreadCount());
        assertEquals(1L, stats.getErrorUnreadCount());
    }

    @Test
    void shouldReturnOnlyUnreadAccessibleMessagesForCurrentUser() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext("BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                message(201L, "system", "critical", "all", null, null),
                message(202L, "business", "medium", "role", "BUSINESS_STAFF", null),
                message(203L, "error", "high", "user", null, "99")
        ));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(readRecord(202L, userId)));

        PageResult<InAppMessageAccessVO> result = inAppMessageService.pageMyMessages(userId, null, true, 1L, 10L);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(201L, result.getRecords().get(0).getId());
        assertFalse(Boolean.TRUE.equals(result.getRecords().get(0).getRead()));
    }

    @Test
    void shouldInsertReadRecordWhenMarkingAccessibleMessageRead() {
        Long userId = 2L;
        InAppMessage message = message(301L, "system", "high", "all", null, null);

        when(inAppMessageMapper.selectById(301L)).thenReturn(message);
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext("BUSINESS_STAFF"));
        when(inAppMessageReadMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        inAppMessageService.markMessageRead(userId, 301L);

        verify(inAppMessageReadMapper).insert(any(InAppMessageRead.class));
    }

    private UserAuthContextVO authContext(String... roleCodes) {
        UserAuthContextVO authContext = new UserAuthContextVO();
        authContext.setUserId(2L);
        authContext.setRoleCodes(List.of(roleCodes));
        return authContext;
    }

    private InAppMessage message(Long id,
                                 String messageType,
                                 String priority,
                                 String targetType,
                                 String targetRoleCodes,
                                 String targetUserIds) {
        InAppMessage message = new InAppMessage();
        message.setId(id);
        message.setTenantId(1L);
        message.setMessageType(messageType);
        message.setPriority(priority);
        message.setTitle("消息-" + id);
        message.setSummary("摘要-" + id);
        message.setContent("正文-" + id);
        message.setTargetType(targetType);
        message.setTargetRoleCodes(targetRoleCodes);
        message.setTargetUserIds(targetUserIds);
        message.setStatus(1);
        message.setSortNo(0);
        message.setPublishTime(new Date(System.currentTimeMillis() - 60_000L));
        return message;
    }

    private InAppMessageRead readRecord(Long messageId, Long userId) {
        InAppMessageRead readRecord = new InAppMessageRead();
        readRecord.setMessageId(messageId);
        readRecord.setUserId(userId);
        readRecord.setReadTime(new Date());
        return readRecord;
    }
}
