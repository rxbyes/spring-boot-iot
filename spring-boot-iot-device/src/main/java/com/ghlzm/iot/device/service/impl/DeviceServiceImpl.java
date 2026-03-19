package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
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
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备服务实现，负责设备台账、建档和详情维护。
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
    public DeviceDetailVO addDevice(DeviceAddDTO dto) {
        Product product = productService.getRequiredByProductKey(dto.getProductKey());
        ensureDeviceCodeUnique(dto.getDeviceCode(), null);

        Device device = new Device();
        // 创建设备时统一从产品继承协议、节点类型，并落初始化状态字段。
        applyEditableFields(device, dto, product, true);
        save(device);
        return getDetailById(device.getId());
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
    public DeviceDetailVO getDetailById(Long id) {
        return toDetailVO(getRequiredById(id));
    }

    @Override
    public DeviceDetailVO getDetailByCode(String deviceCode) {
        return toDetailVO(getRequiredByCode(deviceCode));
    }

    @Override
    public PageResult<DevicePageVO> pageDevices(Long deviceId,
                                                String productKey,
                                                String deviceCode,
                                                String deviceName,
                                                Integer onlineStatus,
                                                Integer activateStatus,
                                                Integer deviceStatus,
                                                Long pageNum,
                                                Long pageSize) {
        Page<Device> page = PageQueryUtils.buildPage(pageNum, pageSize);
        List<Long> filteredProductIds = resolveFilteredProductIds(productKey);
        if (filteredProductIds != null && filteredProductIds.isEmpty()) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        Page<Device> result = page(page, buildDeviceQueryWrapper(
                deviceId,
                filteredProductIds,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus
        ));
        List<DevicePageVO> records = toPageVOList(result.getRecords());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceDetailVO updateDevice(Long id, DeviceAddDTO dto) {
        Device device = getRequiredById(id);
        Product product = productService.getRequiredByProductKey(dto.getProductKey());
        ensureDeviceCodeUnique(dto.getDeviceCode(), id);
        applyEditableFields(device, dto, product, false);
        updateById(device);
        return getDetailById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        Device device = getRequiredById(id);
        device.setDeleted(1);
        updateById(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDevices(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException("请选择需要删除的设备");
        }
        List<Device> devices = lambdaQuery()
                .in(Device::getId, ids)
                .eq(Device::getDeleted, 0)
                .list();
        if (CollectionUtils.isEmpty(devices)) {
            throw new BizException("未找到可删除的设备");
        }
        devices.forEach(device -> device.setDeleted(1));
        updateBatchById(devices);
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

    private void ensureDeviceCodeUnique(String deviceCode, Long excludeId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Device::getId, excludeId);
        }
        Device existing = getBaseMapper().selectOne(wrapper);
        if (existing != null) {
            throw new BizException("设备编码已存在: " + deviceCode);
        }
    }

    private void applyEditableFields(Device device, DeviceAddDTO dto, Product product, boolean initializeDefaults) {
        device.setProductId(product.getId());
        device.setDeviceName(dto.getDeviceName());
        device.setDeviceCode(dto.getDeviceCode());
        device.setDeviceSecret(dto.getDeviceSecret());
        device.setClientId(dto.getClientId());
        device.setUsername(dto.getUsername());
        device.setPassword(dto.getPassword());
        device.setProtocolCode(product.getProtocolCode());
        device.setNodeType(product.getNodeType());
        device.setFirmwareVersion(dto.getFirmwareVersion());
        device.setIpAddress(dto.getIpAddress());
        device.setAddress(dto.getAddress());
        device.setMetadataJson(dto.getMetadataJson());

        if (initializeDefaults) {
            device.setOnlineStatus(0);
            device.setActivateStatus(resolveActivateStatus(dto.getActivateStatus()));
            device.setDeviceStatus(resolveDeviceStatus(dto.getDeviceStatus()));
            return;
        }

        if (dto.getActivateStatus() != null) {
            device.setActivateStatus(dto.getActivateStatus());
        }
        if (dto.getDeviceStatus() != null) {
            device.setDeviceStatus(dto.getDeviceStatus());
        }
    }

    private int resolveActivateStatus(Integer activateStatus) {
        if (activateStatus != null) {
            return activateStatus;
        }
        boolean activateDefault = iotProperties.getDevice() != null
                && Boolean.TRUE.equals(iotProperties.getDevice().getActivateDefault());
        return activateDefault ? 1 : 0;
    }

    private int resolveDeviceStatus(Integer deviceStatus) {
        return deviceStatus != null ? deviceStatus : DeviceStatusEnum.ENABLED.getCode();
    }

    private LambdaQueryWrapper<Device> buildDeviceQueryWrapper(Long deviceId,
                                                               List<Long> filteredProductIds,
                                                               String deviceCode,
                                                               String deviceName,
                                                               Integer onlineStatus,
                                                               Integer activateStatus,
                                                               Integer deviceStatus) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null) {
            wrapper.eq(Device::getId, deviceId);
        }
        if (!CollectionUtils.isEmpty(filteredProductIds)) {
            wrapper.in(Device::getProductId, filteredProductIds);
        }
        if (StringUtils.hasText(deviceCode)) {
            wrapper.like(Device::getDeviceCode, deviceCode.trim());
        }
        if (StringUtils.hasText(deviceName)) {
            wrapper.like(Device::getDeviceName, deviceName.trim());
        }
        if (onlineStatus != null) {
            wrapper.eq(Device::getOnlineStatus, onlineStatus);
        }
        if (activateStatus != null) {
            wrapper.eq(Device::getActivateStatus, activateStatus);
        }
        if (deviceStatus != null) {
            wrapper.eq(Device::getDeviceStatus, deviceStatus);
        }
        wrapper.orderByDesc(Device::getUpdateTime).orderByDesc(Device::getId);
        return wrapper;
    }

    private List<Long> resolveFilteredProductIds(String productKey) {
        if (!StringUtils.hasText(productKey)) {
            return null;
        }
        return productService.lambdaQuery()
                .like(Product::getProductKey, productKey.trim())
                .eq(Product::getDeleted, 0)
                .list()
                .stream()
                .map(Product::getId)
                .toList();
    }

    private List<DevicePageVO> toPageVOList(List<Device> devices) {
        if (CollectionUtils.isEmpty(devices)) {
            return List.of();
        }
        Map<Long, Product> productMap = loadProductMap(devices.stream().map(Device::getProductId).toList());
        return devices.stream()
                .map(device -> toPageVO(device, productMap.get(device.getProductId())))
                .toList();
    }

    private DeviceDetailVO toDetailVO(Device device) {
        Product product = productService.getById(device.getProductId());
        DeviceDetailVO detail = new DeviceDetailVO();
        detail.setId(device.getId());
        detail.setProductId(device.getProductId());
        detail.setProductKey(product != null ? product.getProductKey() : null);
        detail.setProductName(product != null ? product.getProductName() : null);
        detail.setDeviceName(device.getDeviceName());
        detail.setDeviceCode(device.getDeviceCode());
        detail.setDeviceSecret(device.getDeviceSecret());
        detail.setClientId(device.getClientId());
        detail.setUsername(device.getUsername());
        detail.setPassword(device.getPassword());
        detail.setProtocolCode(device.getProtocolCode());
        detail.setNodeType(device.getNodeType());
        detail.setOnlineStatus(device.getOnlineStatus());
        detail.setActivateStatus(device.getActivateStatus());
        detail.setDeviceStatus(device.getDeviceStatus());
        detail.setFirmwareVersion(device.getFirmwareVersion());
        detail.setIpAddress(device.getIpAddress());
        detail.setAddress(device.getAddress());
        detail.setMetadataJson(device.getMetadataJson());
        detail.setLastOnlineTime(device.getLastOnlineTime());
        detail.setLastOfflineTime(device.getLastOfflineTime());
        detail.setLastReportTime(device.getLastReportTime());
        detail.setCreateTime(device.getCreateTime());
        detail.setUpdateTime(device.getUpdateTime());
        return detail;
    }

    private DevicePageVO toPageVO(Device device, Product product) {
        DevicePageVO row = new DevicePageVO();
        row.setId(device.getId());
        row.setProductId(device.getProductId());
        row.setProductKey(product != null ? product.getProductKey() : null);
        row.setProductName(product != null ? product.getProductName() : null);
        row.setDeviceName(device.getDeviceName());
        row.setDeviceCode(device.getDeviceCode());
        row.setProtocolCode(device.getProtocolCode());
        row.setNodeType(device.getNodeType());
        row.setOnlineStatus(device.getOnlineStatus());
        row.setActivateStatus(device.getActivateStatus());
        row.setDeviceStatus(device.getDeviceStatus());
        row.setFirmwareVersion(device.getFirmwareVersion());
        row.setIpAddress(device.getIpAddress());
        row.setAddress(device.getAddress());
        row.setLastOnlineTime(device.getLastOnlineTime());
        row.setLastOfflineTime(device.getLastOfflineTime());
        row.setLastReportTime(device.getLastReportTime());
        row.setCreateTime(device.getCreateTime());
        row.setUpdateTime(device.getUpdateTime());
        return row;
    }

    private Map<Long, Product> loadProductMap(List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Map.of();
        }
        return productService.listByIds(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left));
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
