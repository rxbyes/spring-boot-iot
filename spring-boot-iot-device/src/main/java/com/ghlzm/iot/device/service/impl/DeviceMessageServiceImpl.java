package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;
    private final IotProperties iotProperties;

    public DeviceMessageServiceImpl(DeviceMapper deviceMapper,
                                    DeviceMessageLogMapper deviceMessageLogMapper,
                                    DevicePropertyMapper devicePropertyMapper,
                                    ProductMapper productMapper,
                                    ProductModelMapper productModelMapper,
                                    IotProperties iotProperties) {
        this.deviceMapper = deviceMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
        this.iotProperties = iotProperties;
    }

    @Override
    public List<DeviceMessageLog> listMessageLogs(String deviceCode) {
        // 消息日志查询也统一先通过 deviceCode 定位设备，避免控制层感知主键细节。
        Device device = findDeviceByCode(deviceCode);
        if (device == null) {
            throw new BizException("设备不存在: " + deviceCode);
        }
        return deviceMessageLogMapper.selectList(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getDeviceId, device.getId())
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .last("limit 20")
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleUpMessage(DeviceUpMessage upMessage) {
        // 上报处理链路保留给后续任务使用，这里仅补充注释，不扩展业务行为。
        Device device = findDeviceByCode(upMessage.getDeviceCode());
        if (device == null) {
            throw new BizException("设备不存在: " + upMessage.getDeviceCode());
        }
        if (device.getProtocolCode() == null || !device.getProtocolCode().equals(upMessage.getProtocolCode())) {
            throw new BizException("设备协议不匹配: " + upMessage.getDeviceCode());
        }

        String productKey = fetchProductKey(device);
        if (productKey == null || upMessage.getProductKey() == null || !upMessage.getProductKey().equalsIgnoreCase(productKey)) {
            throw new BizException("设备所属产品不匹配: " + upMessage.getDeviceCode());
        }

        saveMessageLog(device, upMessage);
        updateLatestProperties(device, upMessage);
        updateDeviceOnlineStatus(device, upMessage);
    }

    private Device findDeviceByCode(String deviceCode) {
        return deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, deviceCode)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
    }

    private void saveMessageLog(Device device, DeviceUpMessage upMessage) {
        // 原始消息日志独立落库，方便后续排障与审计查询。
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

        // 最新属性表只保留当前值，因此这里按标识符执行“存在则更新，不存在则新增”。
        Map<String, ProductModel> propertyModels = listPropertyModels(device.getProductId());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String identifier = entry.getKey();
            Object value = entry.getValue();
            ProductModel productModel = propertyModels.get(identifier);

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
                property.setPropertyName(productModel == null ? identifier : productModel.getModelName());
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value, productModel));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setCreateTime(LocalDateTime.now());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.insert(property);
            } else {
                property.setPropertyName(productModel == null ? property.getPropertyName() : productModel.getModelName());
                property.setPropertyValue(value == null ? null : String.valueOf(value));
                property.setValueType(resolveValueType(value, productModel));
                property.setReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
                property.setUpdateTime(LocalDateTime.now());
                devicePropertyMapper.updateById(property);
            }
        }
    }

    private void updateDeviceOnlineStatus(Device device, DeviceUpMessage upMessage) {
        // 一期先基于最近一次上报刷新在线状态和最后上报时间。
        LocalDateTime reportTime = upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp();

        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(reportTime);
        update.setLastReportTime(reportTime);
        boolean activateDefault = iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getActivateDefault());
        if (activateDefault) {
            update.setActivateStatus(1);
        }
        deviceMapper.updateById(update);
    }

    private Map<String, ProductModel> listPropertyModels(Long productId) {
        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
        );
        return productModels.stream().collect(Collectors.toMap(ProductModel::getIdentifier, Function.identity(), (left, right) -> left));
    }

    private String resolveValueType(Object value, ProductModel productModel) {
        // 优先采用物模型中声明的数据类型，缺失时再按运行时值推断。
        if (productModel != null && productModel.getDataType() != null && !productModel.getDataType().isBlank()) {
            return productModel.getDataType();
        }
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

    private String fetchProductKey(Device device) {
        Product product = productMapper.selectById(device.getProductId());
        return product == null ? null : product.getProductKey();
    }
}
