package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备服务实现，负责设备建档以及基于 deviceCode 的最小查询能力。
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final ProductService productService;
    private final DevicePropertyMapper devicePropertyMapper;
    private final ProductModelMapper productModelMapper;
    private final IotProperties iotProperties;

    public DeviceServiceImpl(ProductService productService,
                             DevicePropertyMapper devicePropertyMapper,
                             ProductModelMapper productModelMapper,
                             IotProperties iotProperties) {
        this.productService = productService;
        this.devicePropertyMapper = devicePropertyMapper;
        this.productModelMapper = productModelMapper;
        this.iotProperties = iotProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device addDevice(DeviceAddDTO dto) {
        // 设备建档前先确认产品存在，设备会继承产品的协议和节点类型。
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
        // 激活状态默认值由基础配置统一控制，避免散落在控制层。
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
        // 属性查询等接口都依赖 deviceCode，因此这里集中封装辅助查询逻辑。
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
        // 先解析 deviceCode 对应的设备，再按 deviceId 查询最新属性。
        Device device = getRequiredByCode(deviceCode);
        return devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByDesc(DeviceProperty::getUpdateTime)
        );
    }

    @Override
    public List<DeviceOptionVO> listDeviceOptions() {
        return lambdaQuery()
                .eq(Device::getDeleted, 0)
                .eq(Device::getDeviceStatus, DeviceStatusEnum.ENABLED.getCode())
                .orderByDesc(Device::getCreateTime)
                .list()
                .stream()
                .map(this::toDeviceOption)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeviceMetricOptionVO> listMetricOptions(Long deviceId) {
        Device device = getRequiredById(deviceId);
        Map<String, DeviceMetricOptionVO> optionMap = new LinkedHashMap<>();

        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, device.getProductId())
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
                        .orderByAsc(ProductModel::getSortNo)
                        .orderByAsc(ProductModel::getIdentifier)
        );
        for (ProductModel productModel : productModels) {
            DeviceMetricOptionVO option = new DeviceMetricOptionVO();
            option.setIdentifier(productModel.getIdentifier());
            option.setName(productModel.getModelName() == null || productModel.getModelName().isBlank()
                    ? productModel.getIdentifier()
                    : productModel.getModelName());
            option.setDataType(productModel.getDataType());
            optionMap.put(option.getIdentifier(), option);
        }

        List<DeviceProperty> deviceProperties = devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByAsc(DeviceProperty::getIdentifier)
        );
        for (DeviceProperty deviceProperty : deviceProperties) {
            optionMap.computeIfAbsent(deviceProperty.getIdentifier(), key -> {
                DeviceMetricOptionVO option = new DeviceMetricOptionVO();
                option.setIdentifier(deviceProperty.getIdentifier());
                option.setName(deviceProperty.getPropertyName() == null || deviceProperty.getPropertyName().isBlank()
                        ? deviceProperty.getIdentifier()
                        : deviceProperty.getPropertyName());
                option.setDataType(deviceProperty.getValueType());
                return option;
            });
        }

        return optionMap.values().stream().collect(Collectors.toList());
    }

    private DeviceOptionVO toDeviceOption(Device device) {
        DeviceOptionVO option = new DeviceOptionVO();
        option.setId(device.getId());
        option.setProductId(device.getProductId());
        option.setDeviceCode(device.getDeviceCode());
        option.setDeviceName(device.getDeviceName());
        option.setOnlineStatus(device.getOnlineStatus());
        return option;
    }
}
