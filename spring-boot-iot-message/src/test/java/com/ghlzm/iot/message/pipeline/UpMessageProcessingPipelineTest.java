package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.handler.DeviceContractStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceMessageLogStageHandler;
import com.ghlzm.iot.device.service.handler.DevicePayloadApplyStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceRiskDispatchStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceStateStageHandler;
import com.ghlzm.iot.device.service.model.DevicePayloadApplyResult;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowMetricsRecorder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStages;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStatuses;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStep;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import com.ghlzm.iot.message.mqtt.MqttTopicRouter;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.telemetry.service.handler.TelemetryPersistStageHandler;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpMessageProcessingPipelineTest {

    @Mock
    private MessageFlowMetricsRecorder messageFlowMetricsRecorder;
    @Mock
    private MessageFlowTimelineStore messageFlowTimelineStore;
    @Mock
    private MqttTopicRouter mqttTopicRouter;
    @Mock
    private ProtocolAdapterRegistry protocolAdapterRegistry;
    @Mock
    private DeviceContractStageHandler deviceContractStageHandler;
    @Mock
    private DeviceMessageLogStageHandler deviceMessageLogStageHandler;
    @Mock
    private DevicePayloadApplyStageHandler devicePayloadApplyStageHandler;
    @Mock
    private TelemetryPersistStageHandler telemetryPersistStageHandler;
    @Mock
    private DeviceStateStageHandler deviceStateStageHandler;
    @Mock
    private DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler;
    @Mock
    private ProtocolAdapter protocolAdapter;

    private UpMessageProcessingPipeline pipeline;

    @BeforeEach
    void setUp() {
        MessageFlowProperties messageFlowProperties = new MessageFlowProperties();
        messageFlowProperties.setEnabled(true);
        messageFlowProperties.setTtlHours(24);
        messageFlowProperties.setSessionMatchWindowSeconds(120);
        lenient().when(messageFlowTimelineStore.getSession(anyString())).thenReturn(Optional.empty());
        lenient().when(messageFlowTimelineStore.getSessionIdByFingerprint(anyString())).thenReturn(Optional.empty());
        pipeline = new UpMessageProcessingPipeline(
                messageFlowProperties,
                messageFlowMetricsRecorder,
                messageFlowTimelineStore,
                mqttTopicRouter,
                protocolAdapterRegistry,
                deviceContractStageHandler,
                deviceMessageLogStageHandler,
                devicePayloadApplyStageHandler,
                telemetryPersistStageHandler,
                deviceStateStageHandler,
                deviceRiskDispatchStageHandler
        );
        lenient().when(telemetryPersistStageHandler.persist(any()))
                .thenReturn(TelemetryPersistResult.skipped("EMPTY_PROPERTIES"));
    }

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    @Test
    void processShouldBuildImmediateHttpTimelineWithFixedStageOrder() {
        UpMessageProcessingRequest request = buildHttpRequest();
        DeviceUpMessage upMessage = buildUpMessage("demo-device-01", "demo-product", "property", "/message/http/report");
        DeviceProcessingTarget target = buildTarget("demo-device-01", upMessage);
        DevicePayloadApplyResult payloadApplyResult = buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 1));

        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any())).thenReturn(upMessage);
        when(deviceContractStageHandler.resolve(any())).thenReturn(target);
        when(devicePayloadApplyStageHandler.apply(target)).thenReturn(payloadApplyResult);
        when(telemetryPersistStageHandler.persist(target)).thenReturn(TelemetryPersistResult.persisted(1));
        when(deviceRiskDispatchStageHandler.dispatch(target)).thenReturn(true);

        MessageFlowExecutionResult result = pipeline.process(request);

        assertEquals(MessageFlowStatuses.SESSION_COMPLETED, result.getSubmitResult().getStatus());
        assertTrue(Boolean.TRUE.equals(result.getSubmitResult().getTimelineAvailable()));
        assertFalse(Boolean.TRUE.equals(result.getSubmitResult().getCorrelationPending()));
        assertNotNull(result.getSubmitResult().getSessionId());
        assertNotNull(result.getSubmitResult().getTraceId());
        assertEquals(
                List.of(
                        MessageFlowStages.INGRESS,
                        MessageFlowStages.TOPIC_ROUTE,
                        MessageFlowStages.PROTOCOL_DECODE,
                        MessageFlowStages.DEVICE_CONTRACT,
                        MessageFlowStages.MESSAGE_LOG,
                        MessageFlowStages.PAYLOAD_APPLY,
                        MessageFlowStages.TELEMETRY_PERSIST,
                        MessageFlowStages.DEVICE_STATE,
                        MessageFlowStages.RISK_DISPATCH,
                        MessageFlowStages.COMPLETE
                ),
                result.getTimeline().getSteps().stream().map(MessageFlowStep::getStage).toList()
        );
        MessageFlowStep routeStep = findStep(result.getTimeline(), MessageFlowStages.TOPIC_ROUTE);
        assertEquals(MessageFlowStatuses.STEP_SKIPPED, routeStep.getStatus());
        assertEquals("DIRECT_HTTP", routeStep.getBranch());
        MessageFlowStep payloadStep = findStep(result.getTimeline(), MessageFlowStages.PAYLOAD_APPLY);
        assertEquals("PROPERTY", payloadStep.getBranch());
        assertEquals(1, payloadStep.getSummary().get("propertyCount"));
        verify(deviceMessageLogStageHandler).save(target);
        verify(telemetryPersistStageHandler).persist(target);
        verify(deviceStateStageHandler).refresh(target);
        verify(deviceRiskDispatchStageHandler).dispatch(target);

        ArgumentCaptor<MessageFlowTimeline> timelineCaptor = ArgumentCaptor.forClass(MessageFlowTimeline.class);
        verify(messageFlowTimelineStore).saveTimeline(timelineCaptor.capture());
        assertEquals(MessageFlowStatuses.SESSION_COMPLETED, timelineCaptor.getValue().getStatus());
    }

    @Test
    void processShouldBindPendingMqttSessionAfterStandardTopicDecode() {
        String topic = "/sys/demo-product/demo-device-01/thing/property/post";
        UpMessageProcessingRequest request = buildMqttRequest(topic, "{\"temperature\":26.5}");
        RawDeviceMessage rawDeviceMessage = buildRawMessage(topic, "direct", "demo-device-01", "demo-product");
        DeviceUpMessage upMessage = buildUpMessage("demo-device-01", "demo-product", "property", topic);
        DeviceProcessingTarget target = buildTarget("demo-device-01", upMessage);
        DevicePayloadApplyResult payloadApplyResult = buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 1));
        MessageFlowSession pendingSession = new MessageFlowSession();
        pendingSession.setSessionId("session-pending-001");
        pendingSession.setTransportMode("MQTT");
        pendingSession.setStatus(MessageFlowStatuses.SESSION_PUBLISHED);
        pendingSession.setCorrelationPending(Boolean.TRUE);

        when(mqttTopicRouter.toRawMessage(anyString(), any(MqttMessage.class))).thenReturn(rawDeviceMessage);
        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any())).thenReturn(upMessage);
        when(deviceContractStageHandler.resolve(any())).thenReturn(target);
        when(devicePayloadApplyStageHandler.apply(target)).thenReturn(payloadApplyResult);
        when(telemetryPersistStageHandler.persist(target)).thenReturn(TelemetryPersistResult.persisted(1));
        when(messageFlowTimelineStore.getSessionIdByFingerprint(anyString())).thenReturn(Optional.of("session-pending-001"));
        when(messageFlowTimelineStore.getSession("session-pending-001")).thenReturn(Optional.of(pendingSession));

        MessageFlowExecutionResult result = pipeline.process(request);

        assertEquals("session-pending-001", result.getSubmitResult().getSessionId());
        assertEquals(MessageFlowStatuses.SESSION_COMPLETED, result.getSubmitResult().getStatus());
        MessageFlowStep decodeStep = findStep(result.getTimeline(), MessageFlowStages.PROTOCOL_DECODE);
        assertEquals(Boolean.TRUE, decodeStep.getSummary().get("correlationMatched"));

        ArgumentCaptor<MessageFlowSession> sessionCaptor = ArgumentCaptor.forClass(MessageFlowSession.class);
        verify(messageFlowTimelineStore, atLeastOnce()).saveSession(sessionCaptor.capture());
        MessageFlowSession lastSavedSession = sessionCaptor.getAllValues().get(sessionCaptor.getAllValues().size() - 1);
        assertEquals("session-pending-001", lastSavedSession.getSessionId());
        assertEquals(result.getSubmitResult().getTraceId(), lastSavedSession.getTraceId());
        assertEquals("demo-device-01", lastSavedSession.getDeviceCode());
        assertEquals(topic, lastSavedSession.getTopic());
        assertFalse(Boolean.TRUE.equals(lastSavedSession.getCorrelationPending()));
    }

    @Test
    void processShouldKeepLegacyMqttRouteTypeForDollarDpTopic() {
        UpMessageProcessingRequest request = buildMqttRequest("$dp", "plain-text");
        RawDeviceMessage rawDeviceMessage = buildRawMessage("$dp", "legacy", "demo-device-01", "demo-product");
        DeviceUpMessage upMessage = buildUpMessage("demo-device-01", "demo-product", "property", "$dp");
        DeviceProcessingTarget target = buildTarget("demo-device-01", upMessage);

        when(mqttTopicRouter.toRawMessage(anyString(), any(MqttMessage.class))).thenReturn(rawDeviceMessage);
        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any())).thenReturn(upMessage);
        when(deviceContractStageHandler.resolve(any())).thenReturn(target);
        when(devicePayloadApplyStageHandler.apply(target)).thenReturn(buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 1)));
        when(telemetryPersistStageHandler.persist(target)).thenReturn(TelemetryPersistResult.skipped("EMPTY_PROPERTIES"));

        MessageFlowExecutionResult result = pipeline.process(request);

        MessageFlowStep routeStep = findStep(result.getTimeline(), MessageFlowStages.TOPIC_ROUTE);
        assertEquals("legacy", routeStep.getSummary().get("routeType"));
        MessageFlowStep decodeStep = findStep(result.getTimeline(), MessageFlowStages.PROTOCOL_DECODE);
        assertEquals("legacy", decodeStep.getSummary().get("routeType"));
        MessageFlowStep telemetryStep = findStep(result.getTimeline(), MessageFlowStages.TELEMETRY_PERSIST);
        assertEquals(MessageFlowStatuses.STEP_SKIPPED, telemetryStep.getStatus());
        assertEquals("EMPTY_PROPERTIES", telemetryStep.getBranch());
    }

    @Test
    void processShouldKeepSessionCompletedWhenTelemetryPersistFails() {
        UpMessageProcessingRequest request = buildHttpRequest();
        DeviceUpMessage upMessage = buildUpMessage("demo-device-01", "demo-product", "property", "/message/http/report");
        DeviceProcessingTarget target = buildTarget("demo-device-01", upMessage);

        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any())).thenReturn(upMessage);
        when(deviceContractStageHandler.resolve(any())).thenReturn(target);
        when(devicePayloadApplyStageHandler.apply(target))
                .thenReturn(buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 1)));
        when(telemetryPersistStageHandler.persist(target)).thenThrow(new BizException("TDengine unavailable"));
        when(deviceRiskDispatchStageHandler.dispatch(target)).thenReturn(true);

        MessageFlowExecutionResult result = pipeline.process(request);

        assertEquals(MessageFlowStatuses.SESSION_COMPLETED, result.getSubmitResult().getStatus());
        MessageFlowStep telemetryStep = findStep(result.getTimeline(), MessageFlowStages.TELEMETRY_PERSIST);
        assertEquals(MessageFlowStatuses.STEP_FAILED, telemetryStep.getStatus());
        assertEquals("NON_BLOCKING_FAILURE", telemetryStep.getBranch());
        verify(deviceStateStageHandler).refresh(target);
        verify(deviceRiskDispatchStageHandler).dispatch(target);
    }

    @Test
    void processShouldPersistFailedTimelineWhenProtocolDecodeReturnsNull() {
        UpMessageProcessingRequest request = buildHttpRequest();

        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any())).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> pipeline.process(request));
        assertEquals("协议解析结果为空", exception.getMessage());

        ArgumentCaptor<MessageFlowTimeline> timelineCaptor = ArgumentCaptor.forClass(MessageFlowTimeline.class);
        verify(messageFlowTimelineStore).saveTimeline(timelineCaptor.capture());
        MessageFlowTimeline failedTimeline = timelineCaptor.getValue();
        assertEquals(MessageFlowStatuses.SESSION_FAILED, failedTimeline.getStatus());
        MessageFlowStep failedStep = failedTimeline.getSteps().get(failedTimeline.getSteps().size() - 1);
        assertEquals(MessageFlowStages.PROTOCOL_DECODE, failedStep.getStage());
        assertEquals(MessageFlowStatuses.STEP_FAILED, failedStep.getStatus());
        assertEquals("BizException", failedStep.getErrorClass());
        assertEquals("协议解析结果为空", failedStep.getErrorMessage());
    }

    @Test
    void processShouldAppendChildPayloadStepsForChildMessages() {
        UpMessageProcessingRequest request = buildHttpRequest();
        DeviceUpMessage parentMessage = buildUpMessage("gateway-device", "demo-product", "property", "/message/http/report");

        DeviceUpMessage childMessage1 = new DeviceUpMessage();
        childMessage1.setDeviceCode("child-01");
        childMessage1.setProperties(Map.of("temperature", 26.5));
        DeviceUpMessage childMessage2 = new DeviceUpMessage();
        childMessage2.setDeviceCode("child-02");
        childMessage2.setProperties(Map.of("humidity", 68, "pressure", 1013));
        parentMessage.setChildMessages(List.of(childMessage1, childMessage2));

        DeviceProcessingTarget parentTarget = buildTarget("gateway-device", parentMessage);
        DeviceProcessingTarget childTarget1 = buildTarget("child-01", childMessage1);
        childTarget1.setChildTarget(Boolean.TRUE);
        DeviceProcessingTarget childTarget2 = buildTarget("child-02", childMessage2);
        childTarget2.setChildTarget(Boolean.TRUE);

        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any())).thenReturn(parentMessage);
        when(deviceContractStageHandler.resolve(any())).thenReturn(parentTarget, childTarget1, childTarget2);
        when(devicePayloadApplyStageHandler.apply(any()))
                .thenReturn(
                        buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 0, "childMessageCount", 2)),
                        buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 1)),
                        buildPayloadApplyResult("PROPERTY", Map.of("propertyCount", 2))
                );

        MessageFlowExecutionResult result = pipeline.process(request);

        verify(deviceMessageLogStageHandler, times(3)).save(any(DeviceProcessingTarget.class));
        List<MessageFlowStep> payloadSteps = result.getTimeline().getSteps().stream()
                .filter(step -> MessageFlowStages.PAYLOAD_APPLY.equals(step.getStage()))
                .toList();
        assertEquals(3, payloadSteps.size());
        assertEquals("CHILD_PROPERTY", payloadSteps.get(1).getBranch());
        assertEquals("child-01", payloadSteps.get(1).getSummary().get("childDeviceCode"));
        assertEquals(1, payloadSteps.get(1).getSummary().get("metricCount"));
        assertEquals("CHILD_PROPERTY", payloadSteps.get(2).getBranch());
        assertEquals("child-02", payloadSteps.get(2).getSummary().get("childDeviceCode"));
        assertEquals(2, payloadSteps.get(2).getSummary().get("metricCount"));
    }

    private UpMessageProcessingRequest buildHttpRequest() {
        UpMessageProcessingRequest request = new UpMessageProcessingRequest();
        request.setTransportMode("HTTP");
        request.setSessionId("session-http-001");
        request.setProtocolCode("mqtt-json");
        request.setProductKey("demo-product");
        request.setDeviceCode("demo-device-01");
        request.setTopic("/message/http/report");
        request.setClientId("demo-device-01");
        request.setTenantId("1");
        request.setPayload("{\"temperature\":26.5}".getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private UpMessageProcessingRequest buildMqttRequest(String topic, String payload) {
        UpMessageProcessingRequest request = new UpMessageProcessingRequest();
        request.setTransportMode("MQTT");
        request.setTopic(topic);
        request.setPayload(payload.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private RawDeviceMessage buildRawMessage(String topic,
                                             String routeType,
                                             String deviceCode,
                                             String productKey) {
        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setProtocolCode("mqtt-json");
        rawDeviceMessage.setProductKey(productKey);
        rawDeviceMessage.setDeviceCode(deviceCode);
        rawDeviceMessage.setTopic(topic);
        rawDeviceMessage.setTopicRouteType(routeType);
        rawDeviceMessage.setMessageType("property");
        rawDeviceMessage.setClientId(deviceCode);
        rawDeviceMessage.setPayload("{\"temperature\":26.5}".getBytes(StandardCharsets.UTF_8));
        return rawDeviceMessage;
    }

    private DeviceUpMessage buildUpMessage(String deviceCode,
                                           String productKey,
                                           String messageType,
                                           String topic) {
        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setTenantId("1");
        upMessage.setDeviceCode(deviceCode);
        upMessage.setProductKey(productKey);
        upMessage.setProtocolCode("mqtt-json");
        upMessage.setMessageType(messageType);
        upMessage.setTopic(topic);
        upMessage.setDataFormatType("JSON");
        upMessage.setTimestamp(LocalDateTime.of(2026, 3, 23, 9, 30));
        upMessage.setRawPayload("{\"temperature\":26.5}");
        upMessage.setProperties(Map.of("temperature", 26.5));
        return upMessage;
    }

    private DeviceProcessingTarget buildTarget(String deviceCode, DeviceUpMessage upMessage) {
        Device device = new Device();
        device.setId(Math.abs(deviceCode.hashCode()) + 1L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode(deviceCode);

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);
        return target;
    }

    private DevicePayloadApplyResult buildPayloadApplyResult(String branch, Map<String, Object> summary) {
        DevicePayloadApplyResult result = new DevicePayloadApplyResult();
        result.setBranch(branch);
        result.getSummary().putAll(summary);
        return result;
    }

    private MessageFlowStep findStep(MessageFlowTimeline timeline, String stage) {
        return timeline.getSteps().stream()
                .filter(step -> stage.equals(step.getStage()))
                .findFirst()
                .orElseThrow();
    }
}
