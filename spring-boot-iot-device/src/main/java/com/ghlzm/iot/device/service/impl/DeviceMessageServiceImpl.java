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
    @Transactional(rollbackFor = Exception.class)
    public void handleUpMessage(DeviceUpMessage upMessage) {
        Device device = findDeviceByCode(upMessage.getDeviceCode());
        if (device == null) {
            throw new BizException("设备不存在: " + upMessage.getDeviceCode());
        }
        if (!device.getProtocolCode().equals(upMessage.getProtocolCode())) {
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
        Device update = new Device();
        update.setId(device.getId());
        update.setOnlineStatus(1);
        update.setLastOnlineTime(LocalDateTime.now());
        update.setLastReportTime(upMessage.getTimestamp() == null ? LocalDateTime.now() : upMessage.getTimestamp());
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
        return productModels.stream().collect(Collectors.toMap(ProductModel::getIdentifier, Function.identity(), (a, b) -> a));
    }

    private String resolveValueType(Object value, ProductModel productModel) {
        if (productModel != null && productModel.getDataType() != null) {
            return productModel.getDataType();
        }
        return value == null ? "string" : value.getClass().getSimpleName().toLowerCase();
    }

    private String fetchProductKey(Device device) {
        Product product = productMapper.selectById(device.getProductId());
        return product == null ? null : product.getProductKey();
    }
}
