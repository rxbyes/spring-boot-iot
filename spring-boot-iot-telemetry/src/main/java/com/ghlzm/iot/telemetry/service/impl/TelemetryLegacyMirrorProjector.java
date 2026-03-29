package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Telemetry legacy mirror 投影器。
 */
@Service
public class TelemetryLegacyMirrorProjector {

    private final LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter;
    private final DeviceTelemetryMappingService deviceTelemetryMappingService;

    public TelemetryLegacyMirrorProjector(LegacyTdengineTelemetryWriter legacyTdengineTelemetryWriter,
                                          DeviceTelemetryMappingService deviceTelemetryMappingService) {
        this.legacyTdengineTelemetryWriter = legacyTdengineTelemetryWriter;
        this.deviceTelemetryMappingService = deviceTelemetryMappingService;
    }

    public void project(TelemetryProjectionTask task) {
        if (task == null || task.getProperties() == null || task.getProperties().isEmpty() || task.getProductId() == null) {
            return;
        }
        Map<String, TelemetryMetricMapping> mappingMap =
                deviceTelemetryMappingService.listMetricMappingMap(task.getProductId());
        legacyTdengineTelemetryWriter.persist(buildTarget(task), task.getProperties(), mappingMap);
    }

    private DeviceProcessingTarget buildTarget(TelemetryProjectionTask task) {
        Device device = new Device();
        device.setId(task.getDeviceId());
        device.setTenantId(task.getTenantId());
        device.setProductId(task.getProductId());
        device.setDeviceCode(task.getDeviceCode());

        DeviceUpMessage message = new DeviceUpMessage();
        message.setProductKey(task.getProductKey());
        message.setDeviceCode(task.getDeviceCode());
        message.setProtocolCode(task.getProtocolCode());
        message.setTraceId(task.getTraceId());
        message.setMessageType(task.getMessageType());
        message.setTopic(task.getTopic());
        message.setProperties(task.getProperties());
        message.setTimestamp(task.getReportedAt());

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setMessage(message);
        return target;
    }
}
