package com.ghlzm.iot.admin.observability.alerting;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservabilityAlertNotificationServiceTest {

    @Test
    void dispatchAlertShouldUseOpsAlertTypeSubscriptionFilter() {
        NotificationChannelDispatcher dispatcher = mock(NotificationChannelDispatcher.class);
        IotProperties properties = new IotProperties();
        ObservabilityAlertNotificationService service =
                new ObservabilityAlertNotificationService(dispatcher, properties);

        when(dispatcher.listSceneChannels(eq("observability_alert"), eq("FIELD_DRIFT")))
                .thenReturn(List.of(dispatchChannel("ops-field-drift"), dispatchChannel("ops-governance-all")));
        when(dispatcher.send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NotificationChannelDispatcher.DispatchResult(true, 200, "ok", null));

        ObservabilityAlertNotificationService.DispatchSummary summary = service.dispatchAlert(
                new ObservabilityAlertTrigger(
                        "risk-governance-field-drift-burst",
                        "field_drift:1001",
                        "裂缝产品",
                        "字段漂移告警",
                        3L,
                        1L,
                        null,
                        null,
                        "字段漂移达到阈值",
                        Map.of("productId", 1001L)
                )
        );

        verify(dispatcher).listSceneChannels("observability_alert", "FIELD_DRIFT");
        assertEquals(2, summary.channelCount());
        assertEquals(List.of("ops-field-drift", "ops-governance-all"), summary.channelCodes());
    }

    private NotificationChannelDispatcher.DispatchChannel dispatchChannel(String channelCode) {
        NotificationChannel channel = new NotificationChannel();
        channel.setId(1L);
        channel.setTenantId(1L);
        channel.setChannelCode(channelCode);
        channel.setChannelName(channelCode);
        channel.setChannelType("webhook");
        channel.setStatus(1);
        channel.setDeleted(0);
        return new NotificationChannelDispatcher.DispatchChannel(
                channel,
                new NotificationChannelDispatcher.ChannelConfig(
                        "https://example.com/" + channelCode,
                        Map.of(),
                        List.of("observability_alert"),
                        List.of("FIELD_DRIFT"),
                        3000,
                        300
                )
        );
    }
}
