package com.ghlzm.iot.device.service.handler;

import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 风险分发 stage。
 */
@Component
public class DeviceRiskDispatchStageHandler {

    private final ApplicationEventPublisher eventPublisher;

    public DeviceRiskDispatchStageHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public boolean dispatch(DeviceProcessingTarget target) {
        if (eventPublisher == null
                || target.getMessage() == null
                || target.getMessage().getProperties() == null
                || target.getMessage().getProperties().isEmpty()) {
            return false;
        }
        Map<String, Object> copiedProperties = new LinkedHashMap<>(target.getMessage().getProperties());
        DeviceRiskEvaluationEvent event = new DeviceRiskEvaluationEvent(
                target.getDevice().getTenantId(),
                target.getDevice().getId(),
                target.getDevice().getDeviceCode(),
                target.getDevice().getDeviceName(),
                target.getDevice().getProductId(),
                target.getMessage().getProductKey(),
                target.getMessage().getProtocolCode(),
                target.getMessage().getMessageType(),
                target.getMessage().getTopic(),
                hasText(target.getMessage().getTraceId()) ? target.getMessage().getTraceId() : TraceContextHolder.getTraceId(),
                target.getMessage().getTimestamp() == null ? LocalDateTime.now() : target.getMessage().getTimestamp(),
                copiedProperties
        );
        eventPublisher.publishEvent(event);
        return true;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
