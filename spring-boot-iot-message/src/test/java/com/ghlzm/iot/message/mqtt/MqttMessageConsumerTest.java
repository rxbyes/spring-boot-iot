package com.ghlzm.iot.message.mqtt;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.framework.config.DiagnosticLoggingConstants;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.message.pipeline.MessageFlowExecutionResult;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingPipeline;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttMessageConsumerTest {

    @Mock
    private UpMessageProcessingPipeline upMessageProcessingPipeline;
    @Mock
    private MqttTopicRouter mqttTopicRouter;
    @Mock
    private MqttConnectionListener mqttConnectionListener;
    @Mock
    private MqttConsumerRuntimeState mqttConsumerRuntimeState;
    @Mock
    private MqttClusterLeadershipService mqttClusterLeadershipService;

    private final Logger logger = (Logger) LoggerFactory.getLogger(DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME);
    private final Level originalLevel = logger.getLevel();

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLevel);
        logger.detachAndStopAllAppenders();
    }

    @Test
    void messageArrivedShouldWriteSlowDispatchSummary() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getPerformance().setSlowMqttThresholdMs(5L);
        MqttInvalidReportGovernanceService governanceService = mock(MqttInvalidReportGovernanceService.class);
        MqttMessageConsumer consumer = new MqttMessageConsumer(
                properties,
                upMessageProcessingPipeline,
                mqttTopicRouter,
                mqttConnectionListener,
                mqttConsumerRuntimeState,
                mqttClusterLeadershipService,
                governanceService
        );
        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setTopic("/sys/demo-product/demo-device-01/thing/property/post");
        rawDeviceMessage.setDeviceCode("demo-device-01");
        rawDeviceMessage.setProductKey("demo-product");
        rawDeviceMessage.setClientId("demo-device-01");
        rawDeviceMessage.setMessageType("property");
        doAnswer(invocation -> {
            LockSupport.parkNanos(20_000_000L);
            DeviceUpMessage upMessage = new DeviceUpMessage();
            upMessage.setTraceId("trace-demo-001");
            upMessage.setDeviceCode("demo-device-01");
            upMessage.setProductKey("demo-product");
            upMessage.setMessageType("property");
            MessageFlowSubmitResult submitResult = new MessageFlowSubmitResult();
            submitResult.setSessionId("session-demo-001");
            submitResult.setTraceId("trace-demo-001");
            submitResult.setStatus("COMPLETED");
            submitResult.setTimelineAvailable(true);
            submitResult.setCorrelationPending(false);

            MessageFlowExecutionResult executionResult = new MessageFlowExecutionResult();
            executionResult.setSubmitResult(submitResult);
            executionResult.setRawDeviceMessage(rawDeviceMessage);
            executionResult.setUpMessage(upMessage);
            return executionResult;
        }).when(upMessageProcessingPipeline).process(any());

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        consumer.messageArrived(
                "/sys/demo-product/demo-device-01/thing/property/post",
                new MqttMessage("{\"temp\":26.5}".getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("event=\"slow_mqtt_dispatch\""));
        assertTrue(message.contains("deviceCode=\"demo-device-01\""));
        assertTrue(message.contains("clientId=\"demo-device-01\""));
    }

    @Test
    void messageArrivedShouldDropEmptyPayloadBeforePipelineWhenCooldownActive() {
        IotProperties properties = new IotProperties();
        MqttInvalidReportGovernanceService governanceService = mock(MqttInvalidReportGovernanceService.class);
        when(governanceService.handleRawEmptyPayload("$dp", null)).thenReturn(InvalidMqttReportDecision.dropSuppressed());

        MqttMessageConsumer consumer = new MqttMessageConsumer(
                properties,
                upMessageProcessingPipeline,
                mqttTopicRouter,
                mqttConnectionListener,
                mqttConsumerRuntimeState,
                mqttClusterLeadershipService,
                governanceService
        );

        consumer.messageArrived("$dp", new MqttMessage(new byte[0]));

        verifyNoInteractions(upMessageProcessingPipeline);
        verify(mqttConnectionListener, never()).onMessageDispatchFailed(any(), any(), any(), any());
    }
}
