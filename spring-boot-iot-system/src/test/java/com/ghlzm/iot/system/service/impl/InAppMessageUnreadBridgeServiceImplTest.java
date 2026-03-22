package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.InAppMessage;
import com.ghlzm.iot.system.entity.InAppMessageBridgeAttemptLog;
import com.ghlzm.iot.system.entity.InAppMessageBridgeLog;
import com.ghlzm.iot.system.entity.InAppMessageRead;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeAttemptLogMapper;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeLogMapper;
import com.ghlzm.iot.system.mapper.InAppMessageMapper;
import com.ghlzm.iot.system.mapper.InAppMessageReadMapper;
import com.ghlzm.iot.system.service.InAppMessageUnreadBridgeService;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppMessageUnreadBridgeServiceImplTest {

    @Mock
    private InAppMessageMapper inAppMessageMapper;
    @Mock
    private InAppMessageReadMapper inAppMessageReadMapper;
    @Mock
    private InAppMessageBridgeLogMapper inAppMessageBridgeLogMapper;
    @Mock
    private InAppMessageBridgeAttemptLogMapper inAppMessageBridgeAttemptLogMapper;
    @Mock
    private UserService userService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private NotificationChannelDispatcher notificationChannelDispatcher;
    @Mock
    private SystemContentSchemaSupport systemContentSchemaSupport;

    private InAppMessageUnreadBridgeService service;

    @BeforeEach
    void setUp() {
        service = new InAppMessageUnreadBridgeServiceImpl(
                inAppMessageMapper,
                inAppMessageReadMapper,
                inAppMessageBridgeLogMapper,
                inAppMessageBridgeAttemptLogMapper,
                userService,
                permissionService,
                notificationChannelDispatcher,
                systemContentSchemaSupport,
                enabledProperties()
        );
    }

    @Test
    void shouldBridgeUnreadHighPriorityMessageAfterThreshold() {
        InAppMessage message = message(1001L, "high", 31, 101L);
        NotificationChannelDispatcher.DispatchChannel dispatchChannel = dispatchChannel("ops-webhook", 300);

        when(notificationChannelDispatcher.listSceneChannels(InAppMessageUnreadBridgeServiceImpl.BRIDGE_SCENE))
                .thenReturn(List.of(dispatchChannel));
        when(inAppMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(message));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(inAppMessageBridgeLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(userService.listUsers(null, null, null, 1)).thenReturn(List.of(user(101L, "ops_demo", "运维一")));
        when(permissionService.listUserRolesByUserIds(any())).thenReturn(Map.<Long, List<RoleSummaryVO>>of());
        when(notificationChannelDispatcher.send(any(), any())).thenReturn(
                new NotificationChannelDispatcher.DispatchResult(true, 200, "{\"ok\":true}", null)
        );

        service.scanAndBridgeUnreadMessages();

        verify(notificationChannelDispatcher).send(any(), any());
        ArgumentCaptor<InAppMessageBridgeLog> captor = ArgumentCaptor.forClass(InAppMessageBridgeLog.class);
        verify(inAppMessageBridgeLogMapper).insert(captor.capture());
        InAppMessageBridgeLog bridgeLog = captor.getValue();
        assertEquals(message.getId(), bridgeLog.getMessageId());
        assertEquals("ops-webhook", bridgeLog.getChannelCode());
        assertEquals(InAppMessageUnreadBridgeServiceImpl.BRIDGE_SCENE, bridgeLog.getBridgeScene());
        assertEquals(1, bridgeLog.getUnreadCount());
        assertEquals(1, bridgeLog.getBridgeStatus());
        assertEquals(1, bridgeLog.getAttemptCount());
        assertTrue(String.valueOf(bridgeLog.getRecipientSnapshot()).contains("运维一"));

        ArgumentCaptor<InAppMessageBridgeAttemptLog> attemptCaptor = ArgumentCaptor.forClass(InAppMessageBridgeAttemptLog.class);
        verify(inAppMessageBridgeAttemptLogMapper).insert(attemptCaptor.capture());
        InAppMessageBridgeAttemptLog attemptLog = attemptCaptor.getValue();
        assertEquals(bridgeLog.getId(), attemptLog.getBridgeLogId());
        assertEquals(bridgeLog.getAttemptCount(), attemptLog.getAttemptNo());
        assertEquals(bridgeLog.getBridgeStatus(), attemptLog.getBridgeStatus());
    }

    @Test
    void shouldIgnoreReadMessagesAndMessagesBelowThreshold() {
        InAppMessage criticalRecent = message(1002L, "critical", 9, 101L);
        InAppMessage highRead = message(1003L, "high", 31, 102L);
        NotificationChannelDispatcher.DispatchChannel dispatchChannel = dispatchChannel("ops-webhook", 300);

        when(notificationChannelDispatcher.listSceneChannels(InAppMessageUnreadBridgeServiceImpl.BRIDGE_SCENE))
                .thenReturn(List.of(dispatchChannel));
        when(inAppMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(criticalRecent, highRead));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(readRecord(highRead.getId(), 102L)));
        when(inAppMessageBridgeLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(userService.listUsers(null, null, null, 1)).thenReturn(List.of(
                user(101L, "ops_demo", "运维一"),
                user(102L, "manager_demo", "管理二")
        ));
        when(permissionService.listUserRolesByUserIds(any())).thenReturn(Map.<Long, List<RoleSummaryVO>>of());

        service.scanAndBridgeUnreadMessages();

        verify(notificationChannelDispatcher, never()).send(any(), any());
        verify(inAppMessageBridgeLogMapper, never()).insert(any(InAppMessageBridgeLog.class));
        verify(inAppMessageBridgeLogMapper, never()).updateById(any(InAppMessageBridgeLog.class));
    }

    @Test
    void shouldSkipSucceededBridgeAndRecentFailedBridge() {
        InAppMessage succeededMessage = message(1004L, "critical", 20, 101L);
        InAppMessage failedRecentMessage = message(1005L, "high", 40, 102L);
        NotificationChannelDispatcher.DispatchChannel dispatchChannel = dispatchChannel("ops-webhook", 300);

        when(notificationChannelDispatcher.listSceneChannels(InAppMessageUnreadBridgeServiceImpl.BRIDGE_SCENE))
                .thenReturn(List.of(dispatchChannel));
        when(inAppMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(succeededMessage, failedRecentMessage));
        when(inAppMessageReadMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(inAppMessageBridgeLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                bridgeLog(succeededMessage.getId(), "ops-webhook", 1, 600),
                bridgeLog(failedRecentMessage.getId(), "ops-webhook", 0, 60)
        ));
        when(userService.listUsers(null, null, null, 1)).thenReturn(List.of(
                user(101L, "ops_demo", "运维一"),
                user(102L, "manager_demo", "管理二")
        ));
        when(permissionService.listUserRolesByUserIds(any())).thenReturn(Map.<Long, List<RoleSummaryVO>>of());

        service.scanAndBridgeUnreadMessages();

        verify(notificationChannelDispatcher, never()).send(any(), any());
        verify(inAppMessageBridgeLogMapper, never()).insert(any(InAppMessageBridgeLog.class));
        verify(inAppMessageBridgeLogMapper, never()).updateById(any(InAppMessageBridgeLog.class));
    }

    private IotProperties enabledProperties() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getInAppUnreadBridge().setEnabled(true);
        properties.getObservability().getInAppUnreadBridge().setHighThresholdMinutes(30);
        properties.getObservability().getInAppUnreadBridge().setCriticalThresholdMinutes(10);
        properties.getObservability().getInAppUnreadBridge().setScanIntervalSeconds(60);
        return properties;
    }

    private NotificationChannelDispatcher.DispatchChannel dispatchChannel(String channelCode, int minIntervalSeconds) {
        NotificationChannel channel = new NotificationChannel();
        channel.setChannelCode(channelCode);
        channel.setChannelType("webhook");
        channel.setStatus(1);
        channel.setDeleted(0);
        return new NotificationChannelDispatcher.DispatchChannel(
                channel,
                new NotificationChannelDispatcher.ChannelConfig(
                        "https://notify.example.com/hook",
                        Map.of(),
                        List.of(InAppMessageUnreadBridgeServiceImpl.BRIDGE_SCENE),
                        3000,
                        minIntervalSeconds
                )
        );
    }

    private InAppMessage message(Long id, String priority, int publishedMinutesAgo, Long targetUserId) {
        InAppMessage message = new InAppMessage();
        message.setId(id);
        message.setTenantId(1L);
        message.setMessageType("error");
        message.setPriority(priority);
        message.setTitle("消息-" + id);
        message.setSummary("摘要-" + id);
        message.setContent("正文-" + id);
        message.setTargetType("user");
        message.setTargetUserIds(String.valueOf(targetUserId));
        message.setStatus(1);
        message.setPublishTime(new Date(System.currentTimeMillis() - publishedMinutesAgo * 60_000L));
        return message;
    }

    private InAppMessageRead readRecord(Long messageId, Long userId) {
        InAppMessageRead read = new InAppMessageRead();
        read.setMessageId(messageId);
        read.setUserId(userId);
        read.setReadTime(new Date());
        return read;
    }

    private InAppMessageBridgeLog bridgeLog(Long messageId, String channelCode, int status, int lastAttemptSecondsAgo) {
        InAppMessageBridgeLog bridgeLog = new InAppMessageBridgeLog();
        bridgeLog.setId(messageId + 100L);
        bridgeLog.setTenantId(1L);
        bridgeLog.setMessageId(messageId);
        bridgeLog.setChannelCode(channelCode);
        bridgeLog.setBridgeScene(InAppMessageUnreadBridgeServiceImpl.BRIDGE_SCENE);
        bridgeLog.setBridgeStatus(status);
        bridgeLog.setAttemptCount(1);
        bridgeLog.setLastAttemptTime(new Date(System.currentTimeMillis() - lastAttemptSecondsAgo * 1000L));
        return bridgeLog;
    }

    private User user(Long id, String username, String realName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setStatus(1);
        return user;
    }
}
