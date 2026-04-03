package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.NotificationChannelMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationChannelServiceImplTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private NotificationChannelMapper notificationChannelMapper;
    @Mock
    private SystemDictValueSupport systemDictValueSupport;

    private NotificationChannelServiceImpl notificationChannelService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, NotificationChannel.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        notificationChannelService = spy(new NotificationChannelServiceImpl(permissionService, systemDictValueSupport));
        Field field = findField(notificationChannelService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(notificationChannelService, notificationChannelMapper);
    }

    @Test
    void shouldFilterScopedChannelPageToCurrentTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        Page<NotificationChannel> page = new Page<>(1L, 10L);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(notificationChannelService).page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class));

        invokeScopedPageChannels(99L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<NotificationChannel>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(notificationChannelService).page(org.mockito.ArgumentMatchers.any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldRejectCrossTenantChannelDelete() throws Exception {
        NotificationChannel channel = new NotificationChannel();
        channel.setId(1L);
        channel.setTenantId(2L);
        channel.setDeleted(0);

        when(notificationChannelMapper.selectById(1L)).thenReturn(channel);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> invokeScopedDeleteChannel(99L, 1L));
        assertEquals("通知渠道不存在或无权访问", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownChannelTypeWhenAddingChannel() {
        NotificationChannel channel = new NotificationChannel();
        channel.setTenantId(1L);
        channel.setChannelCode("bad-type");
        channel.setChannelName("坏渠道");
        channel.setChannelType("slack");
        channel.setConfig("{\"url\":\"https://example.com\"}");

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));
        when(notificationChannelMapper.selectCount(org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(systemDictValueSupport.normalizeRequiredLowerCase(
                eq(99L),
                eq("notification_channel_type"),
                eq("slack"),
                eq("渠道类型"),
                eq(Set.of("email", "sms", "webhook", "wechat", "feishu", "dingtalk"))
        )).thenThrow(new BizException("渠道类型不合法"));

        BizException exception = assertThrows(BizException.class, () -> notificationChannelService.addChannel(99L, channel));
        assertEquals("渠道类型不合法", exception.getMessage());
    }

    private Object invokeScopedPageChannels(Long currentUserId,
                                            String channelName,
                                            String channelCode,
                                            String channelType,
                                            Long pageNum,
                                            Long pageSize) throws Exception {
        try {
            Method method = NotificationChannelServiceImpl.class.getMethod(
                    "pageChannels",
                    Long.class,
                    String.class,
                    String.class,
                    String.class,
                    Long.class,
                    Long.class
            );
            return method.invoke(notificationChannelService, currentUserId, channelName, channelCode, channelType, pageNum, pageSize);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageChannels overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private void invokeScopedDeleteChannel(Long currentUserId, Long channelId) throws Exception {
        try {
            Method method = NotificationChannelServiceImpl.class.getMethod("deleteChannel", Long.class, Long.class);
            method.invoke(notificationChannelService, currentUserId, channelId);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped deleteChannel overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
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

    private Exception unwrap(InvocationTargetException exception) throws Exception {
        if (exception.getTargetException() instanceof Exception target) {
            return target;
        }
        throw exception;
    }
}
