package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.DeviceInvalidReportState;
import com.ghlzm.iot.device.service.DeviceInvalidReportStateService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.invalidreport.InvalidReportCounterStore;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MqttInvalidReportGovernanceServiceTest {

    @Test
    void shouldNotSuppressDifferentDeviceBeforeOwnThresholdIsReached() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getInvalidReportGovernance().setEnabled(true);
        properties.getObservability().getInvalidReportGovernance().getDeviceNotFound().setThresholdCount(2);
        properties.getObservability().getInvalidReportGovernance().getDeviceNotFound().setCooldownMinutes(30);

        DeviceInvalidReportStateService invalidReportStateService = mock(DeviceInvalidReportStateService.class);
        MqttInvalidReportGovernanceService service = new MqttInvalidReportGovernanceService(
                properties,
                new InMemoryInvalidReportCounterStore(),
                invalidReportStateService
        );

        RawDeviceMessage missingA = buildRawDeviceMessage("demo-product", "missing-a");
        RawDeviceMessage missingB = buildRawDeviceMessage("demo-product", "missing-b");
        byte[] payload = "{\"temp\":26.5}".getBytes(StandardCharsets.UTF_8);

        InvalidMqttReportDecision firstA = service.handleDispatchFailure(
                "$dp",
                payload,
                missingA,
                new BizException("设备不存在: missing-a")
        );
        InvalidMqttReportDecision firstB = service.handleDispatchFailure(
                "$dp",
                payload,
                missingB,
                new BizException("设备不存在: missing-b")
        );
        InvalidMqttReportDecision secondB = service.handleDispatchFailure(
                "$dp",
                payload,
                missingB,
                new BizException("设备不存在: missing-b")
        );
        InvalidMqttReportDecision thirdB = service.handleDispatchFailure(
                "$dp",
                payload,
                missingB,
                new BizException("设备不存在: missing-b")
        );

        assertFalse(firstA.suppressed());
        assertFalse(firstB.suppressed());
        assertFalse(secondB.suppressed());
        assertTrue(thirdB.suppressed());
        verify(invalidReportStateService, org.mockito.Mockito.times(4)).upsertState(any(DeviceInvalidReportState.class));
    }

    private RawDeviceMessage buildRawDeviceMessage(String productKey, String deviceCode) {
        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setTenantId("1");
        rawDeviceMessage.setTopicRouteType("legacy");
        rawDeviceMessage.setProtocolCode("mqtt-json");
        rawDeviceMessage.setProductKey(productKey);
        rawDeviceMessage.setDeviceCode(deviceCode);
        rawDeviceMessage.setClientId(deviceCode);
        rawDeviceMessage.setTraceId("trace-" + deviceCode);
        return rawDeviceMessage;
    }

    private static final class InMemoryInvalidReportCounterStore implements InvalidReportCounterStore {

        private final Map<String, Long> reasonCounts = new HashMap<>();
        private final Set<String> cooldownKeys = new HashSet<>();

        @Override
        public long incrementFailureStage(String failureStage) {
            return 0L;
        }

        @Override
        public long incrementReasonCode(String reasonCode) {
            long next = reasonCounts.getOrDefault(reasonCode, 0L) + 1L;
            reasonCounts.put(reasonCode, next);
            return next;
        }

        @Override
        public long sumFailureStageSince(String failureStage, Instant startInclusive) {
            return 0L;
        }

        @Override
        public boolean tryOpenCooldown(String governanceKey, Duration ttl) {
            return cooldownKeys.add(governanceKey);
        }

        @Override
        public void clearCooldown(String governanceKey) {
            cooldownKeys.remove(governanceKey);
        }
    }
}
