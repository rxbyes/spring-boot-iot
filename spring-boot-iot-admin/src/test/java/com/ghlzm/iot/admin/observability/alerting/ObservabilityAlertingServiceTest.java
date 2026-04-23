package com.ghlzm.iot.admin.observability.alerting;

import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.service.RiskGovernanceOpsService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttConsumerRuntimeState;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.service.InAppMessageBridgeAlertQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservabilityAlertingServiceTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Instant NOW = Instant.parse("2026-03-23T04:00:00Z");

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private DeviceAccessErrorLogService deviceAccessErrorLogService;
    @Mock
    private InAppMessageBridgeAlertQueryService inAppMessageBridgeAlertQueryService;
    @Mock
    private RiskGovernanceService riskGovernanceService;
    @Mock
    private RiskGovernanceOpsService riskGovernanceOpsService;
    @Mock
    private MqttMessageConsumer mqttMessageConsumer;
    @Mock
    private MqttConsumerRuntimeState mqttConsumerRuntimeState;
    @Mock
    private ObservabilityAlertNotificationService observabilityAlertNotificationService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private IotProperties iotProperties;
    private ObservabilityAlertingService service;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        iotProperties.getObservability().getAlerting().setEnabled(true);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(auditLogService.countSystemErrorsSince(any())).thenReturn(0L);
        lenient().when(deviceAccessErrorLogService.listFailureStageCountsSince(any())).thenReturn(List.of());
        lenient().when(inAppMessageBridgeAlertQueryService.listFailedAttemptCountsSince(any())).thenReturn(List.of());
        lenient().when(riskGovernanceOpsService.pageOpsAlerts(null, null, 1L, 500L)).thenReturn(PageResult.empty(1L, 500L));
        service = new ObservabilityAlertingService(
                auditLogService,
                deviceAccessErrorLogService,
                inAppMessageBridgeAlertQueryService,
                riskGovernanceService,
                riskGovernanceOpsService,
                mqttMessageConsumer,
                mqttConsumerRuntimeState,
                observabilityAlertNotificationService,
                stringRedisTemplate,
                iotProperties,
                Clock.fixed(NOW, TEST_ZONE)
        );
    }

    @Test
    void shouldTriggerSystemErrorBurstAlert() {
        stubDispatchAlert();
        stubCooldownAcquired();
        when(auditLogService.countSystemErrorsSince(any())).thenReturn(5L);

        service.evaluateAlerts();

        ArgumentCaptor<ObservabilityAlertTrigger> captor = ArgumentCaptor.forClass(ObservabilityAlertTrigger.class);
        verify(observabilityAlertNotificationService).dispatchAlert(captor.capture());
        ObservabilityAlertTrigger trigger = captor.getValue();
        assertEquals("system-error-burst", trigger.ruleType());
        assertEquals("global", trigger.dimensionKey());
        assertEquals(5L, trigger.observedValue());
        assertEquals(5L, trigger.threshold());
        assertEquals(10, trigger.windowMinutes());
    }

    @Test
    void shouldTriggerMqttDisconnectTimeoutAlert() {
        stubDispatchAlert();
        stubCooldownAcquired();
        iotProperties.getMqtt().setEnabled(true);
        when(mqttMessageConsumer.isClusterSingletonEnabled()).thenReturn(true);
        when(mqttMessageConsumer.isLeader()).thenReturn(true);
        when(mqttMessageConsumer.isConnected()).thenReturn(false);
        when(mqttMessageConsumer.getEffectiveClientId()).thenReturn("shared-client-dev-01");
        when(mqttConsumerRuntimeState.snapshot()).thenReturn(new MqttConsumerRuntimeState.Snapshot(
                List.of("$dp"),
                LocalDateTime.of(2026, 3, 23, 11, 0, 0),
                LocalDateTime.of(2026, 3, 23, 11, 54, 0),
                null,
                null,
                LocalDateTime.of(2026, 3, 23, 11, 54, 0),
                "connection",
                "trace-mqtt-001"
        ));

        service.evaluateAlerts();

        ArgumentCaptor<ObservabilityAlertTrigger> captor = ArgumentCaptor.forClass(ObservabilityAlertTrigger.class);
        verify(observabilityAlertNotificationService).dispatchAlert(captor.capture());
        ObservabilityAlertTrigger trigger = captor.getValue();
        assertEquals("mqtt-disconnect-timeout", trigger.ruleType());
        assertTrue(trigger.dimensionKey().contains("shared-client-dev-01"));
        assertEquals(6L, trigger.observedValue());
        assertEquals(5L, trigger.threshold());
        assertEquals(5, trigger.durationMinutes());
    }

    @Test
    void shouldSkipMqttDisconnectTimeoutAlertForStandbyNode() {
        iotProperties.getMqtt().setEnabled(true);
        when(mqttMessageConsumer.isClusterSingletonEnabled()).thenReturn(true);
        when(mqttMessageConsumer.isLeader()).thenReturn(false);

        service.evaluateAlerts();

        verifyNoInteractions(observabilityAlertNotificationService);
    }

    @Test
    void shouldTriggerFailureStageSpikeOnlyForThresholdHits() {
        stubDispatchAlert();
        stubCooldownAcquired();
        when(deviceAccessErrorLogService.listFailureStageCountsSince(any())).thenReturn(List.of(
                new DeviceAccessErrorLogService.FailureStageCount("protocol_decode", 10L),
                new DeviceAccessErrorLogService.FailureStageCount("device_validate", 9L)
        ));

        service.evaluateAlerts();

        ArgumentCaptor<ObservabilityAlertTrigger> captor = ArgumentCaptor.forClass(ObservabilityAlertTrigger.class);
        verify(observabilityAlertNotificationService).dispatchAlert(captor.capture());
        ObservabilityAlertTrigger trigger = captor.getValue();
        assertEquals("failure-stage-spike", trigger.ruleType());
        assertEquals("protocol_decode", trigger.dimensionKey());
        assertEquals(10L, trigger.observedValue());
        assertEquals(10L, trigger.threshold());
    }

    @Test
    void shouldTriggerInAppBridgeFailureBurstAlert() {
        stubDispatchAlert();
        stubCooldownAcquired();
        when(inAppMessageBridgeAlertQueryService.listFailedAttemptCountsSince(any())).thenReturn(List.of(
                new InAppMessageBridgeAlertQueryService.ChannelFailureCount("ops-webhook", "运维Webhook", 3L)
        ));

        service.evaluateAlerts();

        ArgumentCaptor<ObservabilityAlertTrigger> captor = ArgumentCaptor.forClass(ObservabilityAlertTrigger.class);
        verify(observabilityAlertNotificationService).dispatchAlert(captor.capture());
        ObservabilityAlertTrigger trigger = captor.getValue();
        assertEquals("in-app-bridge-failure-burst", trigger.ruleType());
        assertEquals("ops-webhook", trigger.dimensionKey());
        assertEquals(3L, trigger.observedValue());
        assertEquals(3L, trigger.threshold());
        assertEquals(10, trigger.windowMinutes());
    }

    @Test
    void shouldTriggerRiskGovernanceMissingPolicyBurstAlert() {
        stubDispatchAlert();
        stubCooldownAcquired();
        iotProperties.getObservability().getAlerting().getRiskGovernanceMissingPolicy().setThreshold(2);
        when(riskGovernanceService.listMissingPolicyAlertSignals()).thenReturn(List.of(
                new RiskGovernanceService.MissingPolicyAlertSignal(
                        "risk_metric_id:9102",
                        "riskMetricId=9102",
                        9102L,
                        "gpsTotalX",
                        "X轴累计位移",
                        3L,
                        3L
                ),
                new RiskGovernanceService.MissingPolicyAlertSignal(
                        "metric_identifier:gpstotaly",
                        "metricIdentifier=gpsTotalY",
                        null,
                        "gpsTotalY",
                        "Y轴累计位移",
                        1L,
                        1L
                )
        ));

        service.evaluateAlerts();

        ArgumentCaptor<ObservabilityAlertTrigger> captor = ArgumentCaptor.forClass(ObservabilityAlertTrigger.class);
        verify(observabilityAlertNotificationService).dispatchAlert(captor.capture());
        ObservabilityAlertTrigger trigger = captor.getValue();
        assertEquals("risk-governance-missing-policy-burst", trigger.ruleType());
        assertEquals("risk_metric_id:9102", trigger.dimensionKey());
        assertEquals(3L, trigger.observedValue());
        assertEquals(2L, trigger.threshold());
    }

    @Test
    void shouldTriggerRiskGovernanceFieldDriftOpsAlert() {
        stubDispatchAlert();
        stubCooldownAcquired();
        iotProperties.getObservability().getAlerting().getRiskGovernanceOpsAlerts().setEnabled(true);
        iotProperties.getObservability().getAlerting().getRiskGovernanceOpsAlerts().setFieldDriftThreshold(2);
        RiskGovernanceOpsAlertItemVO alertItem = new RiskGovernanceOpsAlertItemVO();
        alertItem.setAlertType("FIELD_DRIFT");
        alertItem.setAlertLabel("字段漂移告警");
        alertItem.setProductId(1001L);
        alertItem.setProductKey("phase1-crack-product");
        alertItem.setProductName("裂缝产品");
        alertItem.setAffectedCount(3L);
        alertItem.setSampleIdentifier("value");
        alertItem.setSampleDetail("raw=value_old");
        when(riskGovernanceOpsService.pageOpsAlerts(null, null, 1L, 500L))
                .thenReturn(PageResult.of(1L, 1L, 500L, List.of(alertItem)));

        service.evaluateAlerts();

        ArgumentCaptor<ObservabilityAlertTrigger> captor = ArgumentCaptor.forClass(ObservabilityAlertTrigger.class);
        verify(observabilityAlertNotificationService).dispatchAlert(captor.capture());
        ObservabilityAlertTrigger trigger = captor.getValue();
        assertEquals("risk-governance-field-drift-burst", trigger.ruleType());
        assertEquals(3L, trigger.observedValue());
        assertEquals(2L, trigger.threshold());
    }

    @Test
    void shouldSkipDispatchWhenCooldownKeyAlreadyExists() {
        when(auditLogService.countSystemErrorsSince(any())).thenReturn(8L);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        service.evaluateAlerts();

        verify(observabilityAlertNotificationService, never()).dispatchAlert(any());
        verify(valueOperations).setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(30)));
    }

    private void stubDispatchAlert() {
        when(observabilityAlertNotificationService.dispatchAlert(any())).thenReturn(
                new ObservabilityAlertNotificationService.DispatchSummary(
                        "observability_alert",
                        1,
                        1,
                        0,
                        List.of("ops-webhook")
                )
        );
    }

    private void stubCooldownAcquired() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }
}
