package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final ProductService productService;
    private final DevicePropertyMapper devicePropertyMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final IotProperties iotProperties;

    public DeviceServiceImpl(ProductService productService,
                             DevicePropertyMapper devicePropertyMapper,
                             DeviceMessageLogMapper deviceMessageLogMapper,
                             IotProperties iotProperties) {
        this.productService = productService;
        this.devicePropertyMapper = devicePropertyMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.iotProperties = iotProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device addDevice(DeviceAddDTO dto) {
        Product product = productService.getRequiredByProductKey(dto.getProductKey());
        Device existing = lambdaQuery()
                .eq(Device::getDeviceCode, dto.getDeviceCode())
                .eq(Device::getDeleted, 0)
                .one();
        if (existing != null) {
            throw new BizException("设备编码已存在: " + dto.getDeviceCode());
        }

        Device device = new Device();
        device.setProductId(product.getId());
        device.setDeviceName(dto.getDeviceName());
        device.setDeviceCode(dto.getDeviceCode());
        device.setDeviceSecret(dto.getDeviceSecret());
        device.setClientId(dto.getClientId());
        device.setUsername(dto.getUsername());
        device.setPassword(dto.getPassword());
        device.setProtocolCode(product.getProtocolCode());
        device.setNodeType(product.getNodeType());
        device.setOnlineStatus(0);
        boolean activateDefault = iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getActivateDefault());
        device.setActivateStatus(activateDefault ? 1 : 0);
        device.setDeviceStatus(DeviceStatusEnum.ENABLED.getCode());
        device.setFirmwareVersion(dto.getFirmwareVersion());
        device.setIpAddress(dto.getIpAddress());
        device.setAddress(dto.getAddress());
        device.setMetadataJson(dto.getMetadataJson());
        save(device);
        return device;
    }

    @Override
    public Device getRequiredById(Long id) {
        Device device = lambdaQuery()
                .eq(Device::getId, id)
                .eq(Device::getDeleted, 0)
                .one();
        if (device == null) {
            throw new BizException("设备不存在: " + id);
        }
        return device;
    }

    @Override
    public Device getRequiredByCode(String deviceCode) {
        Device device = lambdaQuery()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0)
                .one();
        if (device == null) {
            throw new BizException("设备不存在: " + deviceCode);
        }
        return device;
    }

    @Override
    public List<DeviceProperty> listProperties(String deviceCode) {
        Device device = getRequiredByCode(deviceCode);
        return devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByDesc(DeviceProperty::getUpdateTime)
        );
    }

    @Override
    public List<DeviceMessageLog> listMessageLogs(String deviceCode) {
        Device device = getRequiredByCode(deviceCode);
        return deviceMessageLogMapper.selectList(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getDeviceId, device.getId())
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .last("limit 20")
        );
    }
}
