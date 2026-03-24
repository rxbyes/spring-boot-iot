package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;

import java.util.Map;

/**
 * 设备遥测 legacy 映射读取服务。
 */
public interface DeviceTelemetryMappingService {

    Map<String, TelemetryMetricMapping> listMetricMappings(Long productId);
}
