package com.ghlzm.iot.device.service.handler;

import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消息日志落库 stage。
 */
@Component
public class DeviceMessageLogStageHandler {

    private final DeviceMessageLogMapper deviceMessageLogMapper;

    public DeviceMessageLogStageHandler(DeviceMessageLogMapper deviceMessageLogMapper) {
        this.deviceMessageLogMapper = deviceMessageLogMapper;
    }

    public DeviceMessageLog save(DeviceProcessingTarget target) {
        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setTenantId(target.getDevice().getTenantId());
        logRecord.setDeviceId(target.getDevice().getId());
        logRecord.setProductId(target.getDevice().getProductId());
        logRecord.setTraceId(hasText(target.getMessage().getTraceId())
                ? target.getMessage().getTraceId()
                : TraceContextHolder.getTraceId());
        logRecord.setDeviceCode(target.getDevice().getDeviceCode());
        logRecord.setProductKey(target.getMessage().getProductKey());
        logRecord.setMessageType(target.getMessage().getMessageType());
        logRecord.setTopic(target.getMessage().getTopic());
        logRecord.setPayload(target.getMessage().getRawPayload());
        logRecord.setReportTime(target.getMessage().getTimestamp() == null ? LocalDateTime.now() : target.getMessage().getTimestamp());
        logRecord.setCreateTime(LocalDateTime.now());
        deviceMessageLogMapper.insert(logRecord);
        return logRecord;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
