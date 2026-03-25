package com.ghlzm.iot.device.service;

import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;

import java.util.Map;

/**
 * 设备遥测 legacy 映射读取服务。
 */
public interface DeviceTelemetryMappingService {

    /**
     * 查询产品下的遥测映射定义。
     */
    Map<String, TelemetryMetricMapping> listMetricMappingMap(Long productId);
}
