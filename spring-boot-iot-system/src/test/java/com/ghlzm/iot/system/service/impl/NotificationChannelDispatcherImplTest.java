package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import com.ghlzm.iot.system.service.NotificationChannelService;
import com.ghlzm.iot.system.service.NotificationHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationChannelDispatcherImplTest {

    @Mock
    private NotificationChannelService notificationChannelService;

    @Mock
    private NotificationHttpClient notificationHttpClient;

    private NotificationChannelDispatcherImpl dispatcher;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        dispatcher = new NotificationChannelDispatcherImpl(
                notificationChannelService,
                notificationHttpClient,
                new IotProperties(),
                objectMapper
        );
    }

    @Test
    void listSceneChannelsShouldFilterByOpsAlertTypeWhenConfigured() {
        when(notificationChannelService.listChannels(null, null, null)).thenReturn(List.of(
                channel("ops-field-drift", """
                        {"url":"https://example.com/drift","scenes":["observability_alert"],"opsAlertTypes":["FIELD_DRIFT"]}
                        """),
                channel("ops-contract-diff", """
                        {"url":"https://example.com/diff","scenes":["observability_alert"],"opsAlertTypes":["CONTRACT_DIFF"]}
                        """),
                channel("ops-all", """
                        {"url":"https://example.com/all","scenes":["observability_alert"]}
                        """)
        ));

        List<NotificationChannelDispatcher.DispatchChannel> channels =
                dispatcher.listSceneChannels("observability_alert", "FIELD_DRIFT");

        assertEquals(2, channels.size());
        assertEquals(
                List.of("ops-field-drift", "ops-all"),
                channels.stream().map(item -> item.channel().getChannelCode()).toList()
        );
        assertTrue(channels.stream().allMatch(item -> item.config().opsAlertTypes().isEmpty()
                || item.config().opsAlertTypes().contains("FIELD_DRIFT")));
    }

    private NotificationChannel channel(String channelCode, String config) {
        NotificationChannel channel = new NotificationChannel();
        channel.setId(1L);
        channel.setTenantId(1L);
        channel.setChannelName(channelCode);
        channel.setChannelCode(channelCode);
        channel.setChannelType("webhook");
        channel.setConfig(config);
        channel.setStatus(1);
        channel.setSortNo(1);
        channel.setDeleted(0);
        return channel;
    }
}
