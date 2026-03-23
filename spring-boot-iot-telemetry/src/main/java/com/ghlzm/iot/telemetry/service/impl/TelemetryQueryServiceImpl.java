package com.ghlzm.iot.telemetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 时序查询服务实现。
 */
@Service
public class TelemetryQueryServiceImpl implements TelemetryQueryService {

    private final IotProperties iotProperties;
    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final TdengineTelemetryStorageService tdengineTelemetryStorageService;

    public TelemetryQueryServiceImpl(IotProperties iotProperties,
                                     DeviceMapper deviceMapper,
                                     ProductMapper productMapper,
                                     DevicePropertyMapper devicePropertyMapper,
                                     TdengineTelemetryStorageService tdengineTelemetryStorageService) {
        this.iotProperties = iotProperties;
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.tdengineTelemetryStorageService = tdengineTelemetryStorageService;
    }

    @Override
    public Map<String, Object> getLatest(Long deviceId) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getId, deviceId)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
        if (device == null) {
            throw new BizException("设备不存在: " + deviceId);
        }
        Product product = device.getProductId() == null ? null : productMapper.selectById(device.getProductId());
        if ("tdengine".equalsIgnoreCase(normalizeStorageType())) {
            return buildTdengineResponse(device, product);
        }
        return buildMysqlResponse(device, product);
    }

    private Map<String, Object> buildTdengineResponse(Device device, Product product) {
        List<TelemetryLatestPoint> latestPoints = tdengineTelemetryStorageService.listLatestPoints(device.getId());
        Map<String, Object> properties = new LinkedHashMap<>();
        LocalDateTime latestReportedAt = null;
        String latestTraceId = null;
        for (TelemetryLatestPoint latestPoint : latestPoints) {
            properties.put(latestPoint.getMetricCode(), latestPoint.getValue());
            if (latestReportedAt == null
                    || (latestPoint.getReportedAt() != null && latestPoint.getReportedAt().isAfter(latestReportedAt))) {
                latestReportedAt = latestPoint.getReportedAt();
                latestTraceId = latestPoint.getTraceId();
            }
        }
        return buildResponse(device, product, properties, latestReportedAt, latestTraceId);
    }

    private Map<String, Object> buildMysqlResponse(Device device, Product product) {
        List<DeviceProperty> deviceProperties = devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByAsc(DeviceProperty::getIdentifier)
        );
        Map<String, Object> properties = new LinkedHashMap<>();
        LocalDateTime latestReportedAt = null;
        for (DeviceProperty deviceProperty : deviceProperties) {
            properties.put(deviceProperty.getIdentifier(), parseMysqlPropertyValue(deviceProperty));
            if (latestReportedAt == null
                    || (deviceProperty.getReportTime() != null && deviceProperty.getReportTime().isAfter(latestReportedAt))) {
                latestReportedAt = deviceProperty.getReportTime();
            }
        }
        return buildResponse(device, product, properties, latestReportedAt, null);
    }

    private Map<String, Object> buildResponse(Device device,
                                              Product product,
                                              Map<String, Object> properties,
                                              LocalDateTime reportTime,
                                              String traceId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("deviceId", device.getId());
        response.put("deviceCode", device.getDeviceCode());
        response.put("productId", device.getProductId());
        response.put("productKey", product == null ? null : product.getProductKey());
        response.put("storageType", normalizeStorageType());
        response.put("reportTime", reportTime);
        response.put("traceId", traceId);
        response.put("properties", properties);
        return response;
    }

    private Object parseMysqlPropertyValue(DeviceProperty deviceProperty) {
        if (deviceProperty.getPropertyValue() == null) {
            return null;
        }
        String normalizedType = deviceProperty.getValueType() == null
                ? ""
                : deviceProperty.getValueType().trim().toLowerCase(Locale.ROOT);
        try {
            return switch (normalizedType) {
                case "int", "long" -> Long.parseLong(deviceProperty.getPropertyValue());
                case "double", "float", "decimal", "number" -> Double.parseDouble(deviceProperty.getPropertyValue());
                case "bool", "boolean" -> parseBoolean(deviceProperty.getPropertyValue());
                default -> deviceProperty.getPropertyValue();
            };
        } catch (Exception ex) {
            return deviceProperty.getPropertyValue();
        }
    }

    private Boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    private String normalizeStorageType() {
        if (iotProperties.getTelemetry() == null || iotProperties.getTelemetry().getStorageType() == null) {
            return "mysql";
        }
        return iotProperties.getTelemetry().getStorageType().trim().toLowerCase(Locale.ROOT);
    }
}
