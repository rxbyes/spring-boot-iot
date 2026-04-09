package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.InAppMessageRead;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import com.ghlzm.iot.system.mapper.InAppMessageReadMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.InAppMessageAccessVO;
import com.ghlzm.iot.system.vo.InAppMessageStatsVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
    @Mock
    private UserService userService;
    @Mock
    private SystemContentSchemaSupport systemContentSchemaSupport;

    private InAppMessageServiceImpl inAppMessageService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InAppMessage.class);
        TableInfoHelper.initTableInfo(assistant, InAppMessageRead.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        inAppMessageService = spy(new InAppMessageServiceImpl(
                inAppMessageMapper,
                inAppMessageReadMapper,
                permissionService,
                userService,
                systemContentSchemaSupport
        ));
        Field field = findField(inAppMessageService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(inAppMessageService, inAppMessageMapper);
        lenient().when(permissionService.getDataPermissionContext(any(Long.class)))
                .thenAnswer(invocation -> dataPermissionContext(invocation.getArgument(0), 1L, false));
    }

    @Test
    void shouldAggregateUnreadStatsByAccessibleMessageType() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(userId, 1L, "BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any())).thenReturn(List.of(
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
    void shouldQueryUnreadStatsWithoutSelectingMessageBodyColumns() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(userId, 1L, "BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any())).thenReturn(List.of(
                message(101L, "system", "high", "all", null, null)
        ));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        inAppMessageService.getMyUnreadStats(userId);

        ArgumentCaptor<QueryWrapper<InAppMessage>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(inAppMessageMapper).selectList(captor.capture());
        QueryWrapper<InAppMessage> queryWrapper = captor.getValue();

        String sqlSelect = String.valueOf(queryWrapper.getSqlSelect()).toLowerCase(Locale.ROOT);
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("message_type"));
        assertTrue(sqlSelect.contains("target_type"));
        assertFalse(sqlSelect.contains("content"));
        assertFalse(sqlSelect.contains("summary"));
    }

    @Test
    void shouldReturnOnlyUnreadAccessibleMessagesForCurrentUser() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(userId, 1L, "BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any())).thenReturn(List.of(
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
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(userId, 1L, "BUSINESS_STAFF"));
        when(inAppMessageReadMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        inAppMessageService.markMessageRead(userId, 301L);

        verify(inAppMessageReadMapper).insert(any(InAppMessageRead.class));
    }

    @Test
    void shouldSortUnreadMessagesBeforeReadMessages() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(userId, 1L, "BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any())).thenReturn(List.of(
                message(401L, "system", "critical", "all", null, null),
                message(402L, "business", "low", "all", null, null),
                message(403L, "error", "high", "all", null, null)
        ));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(readRecord(401L, userId)));

        PageResult<InAppMessageAccessVO> result = inAppMessageService.pageMyMessages(userId, null, false, 1L, 10L);

        assertEquals(List.of(403L, 402L, 401L), result.getRecords().stream().map(InAppMessageAccessVO::getId).toList());
        assertFalse(Boolean.TRUE.equals(result.getRecords().get(0).getRead()));
        assertTrue(Boolean.TRUE.equals(result.getRecords().get(2).getRead()));
    }

    @Test
    void shouldRejectDeletingAutomaticMessage() {
        InAppMessage message = message(501L, "error", "high", "role", "OPS_STAFF", null);
        message.setSourceType("system_error");
        when(inAppMessageMapper.selectById(501L)).thenReturn(message);

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.deleteMessage(501L, 1L));

        assertEquals("系统自动消息只允许查看或停用", exception.getMessage());
    }

    @Test
    void shouldDeleteManualMessageViaLogicDeleteOperation() {
        InAppMessage message = message(511L, "business", "medium", "all", null, null);
        message.setSourceType("manual");
        when(inAppMessageMapper.selectById(511L)).thenReturn(message);
        when(inAppMessageMapper.deleteById(511L)).thenReturn(1);

        inAppMessageService.deleteMessage(511L, 1L);

        verify(inAppMessageService).removeById(511L);
        verify(inAppMessageMapper, never()).updateById(any(InAppMessage.class));
    }

    @Test
    void shouldThrowWhenManualMessageLogicDeleteFails() {
        InAppMessage message = message(512L, "business", "medium", "all", null, null);
        message.setSourceType("manual");
        when(inAppMessageMapper.selectById(512L)).thenReturn(message);
        when(inAppMessageMapper.deleteById(512L)).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.deleteMessage(512L, 1L));

        assertEquals("站内消息删除失败", exception.getMessage());
    }

    @Test
    void shouldRejectEditingAutomaticMessageContent() {
        InAppMessage existing = message(601L, "error", "high", "role", "OPS_STAFF", null);
        existing.setSourceType("system_error");
        existing.setSourceId("trace-1001");
        when(inAppMessageMapper.selectById(601L)).thenReturn(existing);

        InAppMessage updating = message(601L, "error", "high", "role", "OPS_STAFF", null);
        updating.setSourceType("system_error");
        updating.setSourceId("trace-1001");
        updating.setContent("新的正文");

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.updateMessage(updating, 1L));

        assertEquals("系统自动消息只允许查看或停用", exception.getMessage());
    }

    @Test
    void shouldFilterAdminPageByCurrentTenant() {
        Long currentUserId = 9L;
        when(permissionService.getDataPermissionContext(currentUserId)).thenReturn(dataPermissionContext(currentUserId, 88L, false));
        when(inAppMessageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        inAppMessageService.pageMessages(currentUserId, null, null, null, null, null, null, 1L, 10L);

        ArgumentCaptor<LambdaQueryWrapper<InAppMessage>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(inAppMessageMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
        assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().values().contains(88L));
    }

    @Test
    void shouldRejectCrossTenantAdminDetailAccess() {
        Long currentUserId = 9L;
        InAppMessage message = message(701L, "system", "high", "all", null, null);
        message.setTenantId(8L);
        when(permissionService.getDataPermissionContext(currentUserId)).thenReturn(dataPermissionContext(currentUserId, 7L, false));
        when(inAppMessageMapper.selectById(701L)).thenReturn(message);

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.getById(currentUserId, 701L));

        assertEquals("站内消息不存在或无权访问", exception.getMessage());
    }

    @Test
    void shouldUseOperatorTenantWhenAddingMessage() {
        Long operatorId = 9L;
        InAppMessage message = message(801L, "business", "medium", "all", null, null);
        message.setTenantId(99L);
        when(permissionService.getDataPermissionContext(operatorId)).thenReturn(dataPermissionContext(operatorId, 7L, false));
        when(inAppMessageMapper.selectById(801L)).thenReturn(message);

        inAppMessageService.addMessage(message, operatorId);

        ArgumentCaptor<InAppMessage> messageCaptor = ArgumentCaptor.forClass(InAppMessage.class);
        verify(inAppMessageMapper).insert(messageCaptor.capture());
        assertEquals(7L, messageCaptor.getValue().getTenantId());
    }

    @Test
    void shouldFilterMyUnreadStatsByCurrentTenant() {
        Long userId = 2L;
        when(permissionService.getDataPermissionContext(userId)).thenReturn(dataPermissionContext(userId, 88L, false));
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(userId, 88L, "BUSINESS_STAFF"));
        when(inAppMessageMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                message(901L, "system", "high", "all", null, null)
        ));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        inAppMessageService.getMyUnreadStats(userId);

        ArgumentCaptor<QueryWrapper<InAppMessage>> messageWrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(inAppMessageMapper).selectList(messageWrapperCaptor.capture());
        assertTrue(messageWrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
        assertTrue(messageWrapperCaptor.getValue().getParamNameValuePairs().values().contains(88L));

        ArgumentCaptor<LambdaQueryWrapper<InAppMessageRead>> readWrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(inAppMessageReadMapper).selectList(readWrapperCaptor.capture());
        assertTrue(readWrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
        assertTrue(readWrapperCaptor.getValue().getParamNameValuePairs().values().contains(88L));
    }

    @Test
    void shouldRejectCrossTenantCurrentUserMessageDetail() {
        Long userId = 2L;
        InAppMessage message = message(902L, "system", "high", "all", null, null);
        message.setTenantId(2L);
        when(permissionService.getDataPermissionContext(userId)).thenReturn(dataPermissionContext(userId, 1L, false));
        when(inAppMessageMapper.selectById(902L)).thenReturn(message);

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.getMyMessageDetail(userId, 902L));

        assertEquals("站内消息不存在", exception.getMessage());
    }

    @Test
    void shouldFilterStatsByCurrentTenant() {
        Long currentUserId = 9L;
        when(permissionService.getDataPermissionContext(currentUserId)).thenReturn(dataPermissionContext(currentUserId, 88L, false));
        when(inAppMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        InAppMessageStatsVO stats = inAppMessageService.getMessageStats(currentUserId, null, null, null, null);

        ArgumentCaptor<LambdaQueryWrapper<InAppMessage>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(inAppMessageMapper).selectList(wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
        assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().values().contains(88L));
        assertEquals(0L, stats.getTotalDeliveryCount());
    }

    @Test
    void shouldThrowSchemaHintWhenInAppMessageTableMissing() {
        doThrow(new BizException(SystemContentSchemaSupport.SCHEMA_HINT))
                .when(systemContentSchemaSupport)
                .ensureInAppMessageReady();

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.pageMessages(null, null, null, null, null, null, 1L, 10L));

        assertEquals(SystemContentSchemaSupport.SCHEMA_HINT, exception.getMessage());
    }

    @Test
    void shouldThrowSchemaHintWhenReadTableMissing() {
        doThrow(new BizException(SystemContentSchemaSupport.SCHEMA_HINT))
                .when(systemContentSchemaSupport)
                .ensureInAppMessageReadReady();

        BizException exception = assertThrows(BizException.class,
                () -> inAppMessageService.pageMyMessages(2L, null, false, 1L, 10L));

        assertEquals(SystemContentSchemaSupport.SCHEMA_HINT, exception.getMessage());
    }

    private UserAuthContextVO authContext(Long userId, Long tenantId, String... roleCodes) {
        UserAuthContextVO authContext = new UserAuthContextVO();
        authContext.setUserId(userId);
        authContext.setTenantId(tenantId);
        authContext.setRoleCodes(List.of(roleCodes));
        return authContext;
    }

    private DataPermissionContext dataPermissionContext(Long userId, Long tenantId, boolean superAdmin) {
        return new DataPermissionContext(userId, tenantId, 1L, DataScopeType.TENANT, superAdmin);
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

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
