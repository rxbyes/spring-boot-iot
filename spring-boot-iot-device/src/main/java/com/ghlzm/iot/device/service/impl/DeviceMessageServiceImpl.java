package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:33
 */
@Service
public class DeviceMessageServiceImpl implements DeviceMessageService {

    private final DeviceMapper deviceMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final DevicePropertyMapper devicePropertyMapper;

    public DeviceMessageServiceImpl(DeviceMapper deviceMapper,
                                    DeviceMessageLogMapper deviceMessageLogMapper,
                                    DevicePropertyMapper devicePropertyMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.devicePropertyMapper = devicePropertyMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleUpMessage(DeviceUpMessage upMessage) {
        Device device = findDeviceByCode(upMessage.getTenantId(), upMessage.getDeviceCode());
        if (device == null) {
            throw new BizException("设备不存在: " + upMessage.getDeviceCode());
        }

        saveMessageLog(device, upMessage);
        updateLatestProperties(device, upMessage);
        updateDeviceOnlineStatus(device, upMessage);
    }

    private Device findDeviceByCode(String tenantId, String deviceCode) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0)
                .last("limit 1");

        Long tenantIdValue = parseTenantId(tenantId);
        if (tenantIdValue != null) {
            wrapper.eq(Device::getTenantId, tenantIdValue);
        }

        return deviceMapper.selectOne(wrapper);
    }

    private Long parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            throw new BizException("tenantId 格式错误: " + tenantId);
        }
    }

    private void saveMessageLog(Device device, DeviceUpMessage upMessage) {
        DeviceMessageLog log = new DeviceMessageLog();
        log.setTenantId(device.getTenantId());
        log.setDeviceId(device.getId());
        log.setProductId(device.getProductId());
        log.setMessageType(upMessage.getMessageType());
        log.setTopic(upMessage.getTopic());
        log.setPayload(upMessage.getRawPayload());
        log.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
        log.setCreateTime(LocalDateTime.now());
        deviceMessageLogMapper.insert(log);
    }

    private void updateLatestProperties(Device device, DeviceUpMessage upMessage) {
        Map<String, Object> properties = upMessage.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String identifier = entry.getKey();
            Object value = entry.getValue();

            DeviceProperty property = devicePropertyMapper.selectOne(
                    new LambdaQueryWrapper<DeviceProperty>()
                            .eq(DeviceProperty::getDeviceId, device.getId())
                            .eq(DeviceProperty::getIdentifier, identifier)
                            .last("limit 1")
            );

            if (property == null) {
                property = new DeviceProperty();
                property.setTenantId(device.getTenantId());
                property.setDeviceId(device.getId());
                property.setIdentifier(identifier);
                property.setPropertyName(identifier);
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setCreateTime(LocalDateTime.now());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.insert(property);
            } else {
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.updateById(property);
            }
        }
    }

    private String resolveValueType(Object value) {
        if (value == null) {
            return "string";
        }

        if (value instanceof Integer || value instanceof Long) {
            return "int";
        }
        if (value instanceof Float || value instanceof Double) {
            return "double";
        }
        if (value instanceof Boolean) {
            return "bool";
        }
        return "string";
    }

    private void updateDeviceOnlineStatus(Device device, DeviceUpMessage upMessage) {
        LocalDateTime reportTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();

        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(reportTime);
        update.setLastReportTime(reportTime);
        deviceMapper.updateById(update);
    }
}
