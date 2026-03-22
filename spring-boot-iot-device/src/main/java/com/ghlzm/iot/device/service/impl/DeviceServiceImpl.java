package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceReplaceDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceBatchAddErrorVO;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.device.vo.DeviceReplaceResultVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备服务实现，负责设备台账、建档和详情维护。
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private static final int GATEWAY_NODE_TYPE = 2;
    private static final int SUB_DEVICE_NODE_TYPE = 3;

    private final ProductService productService;
    private final DevicePropertyMapper devicePropertyMapper;
    private final ProductModelMapper productModelMapper;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

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
        Device device = createDeviceRecord(dto);
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
        Product product = productService.getRequiredByProductKey(normalizeRequiredText(dto.getProductKey()));
        resolveDeviceArchiveContract(product);
        ensureDeviceCodeUnique(normalizeRequiredText(dto.getDeviceCode()), id);
        applyEditableFields(device, dto, product, false);
        updateById(device);
        return getDetailById(id);
    }

    @Override
    public DeviceBatchAddResultVO batchAddDevices(List<DeviceAddDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException("请至少提供一条设备数据");
        }

        List<String> createdDeviceCodes = new ArrayList<>();
        List<DeviceBatchAddErrorVO> errors = new ArrayList<>();

        for (int index = 0; index < items.size(); index++) {
            DeviceAddDTO item = items.get(index);
            int rowNo = index + 1;
            try {
                validateBatchAddItem(item, rowNo);
                Device device = createDeviceRecord(item);
                createdDeviceCodes.add(device.getDeviceCode());
            } catch (Exception ex) {
                DeviceBatchAddErrorVO error = new DeviceBatchAddErrorVO();
                error.setRowNo(rowNo);
                error.setDeviceCode(item == null ? null : item.getDeviceCode());
                error.setMessage(resolveErrorMessage(ex));
                errors.add(error);
            }
        }

        DeviceBatchAddResultVO result = new DeviceBatchAddResultVO();
        result.setTotalCount(items.size());
        result.setSuccessCount(createdDeviceCodes.size());
        result.setFailureCount(errors.size());
        result.setCreatedDeviceCodes(createdDeviceCodes);
        result.setErrors(errors);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceReplaceResultVO replaceDevice(Long id, DeviceReplaceDTO dto) {
        Device sourceDevice = getRequiredById(id);
        if (sourceDevice.getDeviceCode().equals(normalizeRequiredText(dto.getDeviceCode()))) {
            throw new BizException("新设备编码不能与原设备编码相同");
        }

        Product targetProduct = resolveReplacementProduct(sourceDevice, dto.getProductKey());
        LocalDateTime replacementTime = LocalDateTime.now();
        DeviceAddDTO replacementDto = buildReplacementCreateDTO(sourceDevice, dto, targetProduct, replacementTime);
        Device replacementDevice = createDeviceRecord(replacementDto);

        Integer previousOnlineStatus = sourceDevice.getOnlineStatus();
        sourceDevice.setOnlineStatus(0);
        sourceDevice.setActivateStatus(0);
        sourceDevice.setDeviceStatus(DeviceStatusEnum.DISABLED.getCode());
        if (sourceDevice.getLastOfflineTime() == null || Integer.valueOf(1).equals(previousOnlineStatus)) {
            sourceDevice.setLastOfflineTime(replacementTime);
        }
        sourceDevice.setMetadataJson(buildSourceReplacementMetadata(sourceDevice.getMetadataJson(), replacementDevice, replacementTime));
        updateById(sourceDevice);

        DeviceReplaceResultVO result = new DeviceReplaceResultVO();
        result.setSourceDeviceId(sourceDevice.getId());
        result.setSourceDeviceCode(sourceDevice.getDeviceCode());
        result.setSourceDeviceName(sourceDevice.getDeviceName());
        result.setTargetDeviceId(replacementDevice.getId());
        result.setTargetDeviceCode(replacementDevice.getDeviceCode());
        result.setTargetDeviceName(replacementDevice.getDeviceName());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        getRequiredById(id);
        if (!removeById(id)) {
            throw new BizException("设备删除失败，请稍后重试");
        }
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
        List<Long> existingIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        if (!removeByIds(existingIds)) {
            throw new BizException("设备批量删除失败，请稍后重试");
        }
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
    public List<DeviceOptionVO> listDeviceOptions(boolean includeDisabled) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeleted, 0)
                .orderByDesc(Device::getCreateTime);
        if (!includeDisabled) {
            wrapper.eq(Device::getDeviceStatus, DeviceStatusEnum.ENABLED.getCode());
        }
        List<Device> devices = list(wrapper);
        Map<Long, Product> productMap = loadProductMap(devices.stream().map(Device::getProductId).toList());
        return devices.stream()
                .map(device -> toDeviceOption(device, productMap.get(device.getProductId())))
                .toList();
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

    private Device createDeviceRecord(DeviceAddDTO dto) {
        Product product = productService.getRequiredByProductKey(normalizeRequiredText(dto.getProductKey()));
        resolveDeviceArchiveContract(product);
        ensureDeviceCodeUnique(normalizeRequiredText(dto.getDeviceCode()), null);

        Device device = new Device();
        // 创建设备时统一从产品继承协议、节点类型，并落初始化状态字段。
        applyEditableFields(device, dto, product, true);
        save(device);
        return device;
    }

    private void ensureProductEnabledForDeviceArchive(Product product) {
        if (product != null && ProductStatusEnum.DISABLED.getCode().equals(product.getStatus())) {
            throw new BizException("产品已停用，禁止继续建档: " + product.getProductKey());
        }
    }

    private DeviceArchiveContract resolveDeviceArchiveContract(Product product) {
        if (product == null || product.getId() == null || product.getId() <= 0) {
            throw new BizException("产品信息无效，禁止继续建档: " + (product == null ? "<unknown>" : product.getProductKey()));
        }
        ensureProductEnabledForDeviceArchive(product);

        String protocolCode = normalizeOptionalText(product.getProtocolCode());
        if (protocolCode == null) {
            throw new BizException("产品未配置接入协议，禁止继续建档: " + product.getProductKey());
        }
        if (product.getNodeType() == null) {
            throw new BizException("产品未配置节点类型，禁止继续建档: " + product.getProductKey());
        }
        return new DeviceArchiveContract(product.getId(), protocolCode, product.getNodeType());
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

    private void validateBatchAddItem(DeviceAddDTO item, int rowNo) {
        if (item == null) {
            throw new BizException("第 " + rowNo + " 行为空");
        }
        if (!StringUtils.hasText(normalizeOptionalText(item.getProductKey()))) {
            throw new BizException("第 " + rowNo + " 行缺少产品 Key");
        }
        if (!StringUtils.hasText(normalizeOptionalText(item.getDeviceName()))) {
            throw new BizException("第 " + rowNo + " 行缺少设备名称");
        }
        if (!StringUtils.hasText(normalizeOptionalText(item.getDeviceCode()))) {
            throw new BizException("第 " + rowNo + " 行缺少设备编码");
        }
        if (item.getParentDeviceId() != null && StringUtils.hasText(normalizeOptionalText(item.getParentDeviceCode()))) {
            throw new BizException("第 " + rowNo + " 行父设备主键和父设备编码不能同时填写");
        }
    }

    private Product resolveReplacementProduct(Device sourceDevice, String productKey) {
        String normalizedProductKey = normalizeOptionalText(productKey);
        if (normalizedProductKey != null) {
            return productService.getRequiredByProductKey(normalizedProductKey);
        }
        Product product = productService.getById(sourceDevice.getProductId());
        if (product == null || product.getDeleted() != null && product.getDeleted() == 1) {
            throw new BizException("原设备所属产品不存在，无法执行更换");
        }
        return product;
    }

    private DeviceAddDTO buildReplacementCreateDTO(Device sourceDevice,
                                                   DeviceReplaceDTO dto,
                                                   Product product,
                                                   LocalDateTime replacementTime) {
        DeviceAddDTO replacement = new DeviceAddDTO();
        replacement.setProductKey(product.getProductKey());
        replacement.setDeviceName(normalizeRequiredText(dto.getDeviceName()));
        replacement.setDeviceCode(normalizeRequiredText(dto.getDeviceCode()));
        replacement.setParentDeviceId(dto.getParentDeviceId());
        replacement.setParentDeviceCode(normalizeOptionalText(dto.getParentDeviceCode()));
        replacement.setDeviceSecret(resolveReplacementText(dto.getDeviceSecret(), sourceDevice.getDeviceSecret()));
        replacement.setClientId(resolveReplacementText(dto.getClientId(), sourceDevice.getClientId()));
        replacement.setUsername(resolveReplacementText(dto.getUsername(), sourceDevice.getUsername()));
        replacement.setPassword(resolveReplacementText(dto.getPassword(), sourceDevice.getPassword()));
        replacement.setActivateStatus(dto.getActivateStatus());
        replacement.setDeviceStatus(dto.getDeviceStatus());
        replacement.setFirmwareVersion(resolveReplacementText(dto.getFirmwareVersion(), sourceDevice.getFirmwareVersion()));
        replacement.setIpAddress(resolveReplacementText(dto.getIpAddress(), sourceDevice.getIpAddress()));
        replacement.setAddress(resolveReplacementText(dto.getAddress(), sourceDevice.getAddress()));
        String metadataJson = normalizeOptionalText(dto.getMetadataJson());
        replacement.setMetadataJson(buildTargetReplacementMetadata(
                metadataJson != null ? metadataJson : sourceDevice.getMetadataJson(),
                sourceDevice,
                replacementTime
        ));
        return replacement;
    }

    private String resolveReplacementText(String candidate, String fallback) {
        String normalized = normalizeOptionalText(candidate);
        return normalized != null ? normalized : fallback;
    }

    private String buildTargetReplacementMetadata(String baseMetadataJson,
                                                  Device sourceDevice,
                                                  LocalDateTime replacementTime) {
        ObjectNode metadata = parseMetadataObject(baseMetadataJson);
        metadata.put("replacementSourceDeviceId", String.valueOf(sourceDevice.getId()));
        metadata.put("replacementSourceDeviceCode", sourceDevice.getDeviceCode());
        metadata.put("replacementSourceDeviceName", sourceDevice.getDeviceName());
        metadata.put("replacementType", "device_swap");
        metadata.put("replacementRecordedAt", replacementTime.toString());
        return writeMetadata(metadata);
    }

    private String buildSourceReplacementMetadata(String baseMetadataJson, Device replacementDevice, LocalDateTime replacementTime) {
        ObjectNode metadata = parseMetadataObject(baseMetadataJson);
        // 原设备保留库存记录，但显式标记已被替换，方便后续台账追踪。
        metadata.put("assetStatus", "replaced");
        metadata.put("replacedByDeviceId", String.valueOf(replacementDevice.getId()));
        metadata.put("replacedByDeviceCode", replacementDevice.getDeviceCode());
        metadata.put("replacedByDeviceName", replacementDevice.getDeviceName());
        metadata.put("replacedAt", replacementTime.toString());
        return writeMetadata(metadata);
    }

    private ObjectNode parseMetadataObject(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(metadataJson);
            if (jsonNode instanceof ObjectNode objectNode) {
                return objectNode.deepCopy();
            }
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("value", jsonNode);
            return wrapper;
        } catch (Exception ex) {
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("_rawMetadata", metadataJson);
            return wrapper;
        }
    }

    private String writeMetadata(ObjectNode metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            throw new BizException("写入设备扩展信息失败");
        }
    }

    private String resolveErrorMessage(Exception ex) {
        if (ex instanceof BizException) {
            return ex.getMessage();
        }
        if (StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return "导入失败，请检查设备数据";
    }

    private void applyEditableFields(Device device, DeviceAddDTO dto, Product product, boolean initializeDefaults) {
        DeviceArchiveContract archiveContract = resolveDeviceArchiveContract(product);
        Device parentDevice = resolveParentDevice(dto, device.getId());
        device.setProductId(archiveContract.productId());
        device.setGatewayId(resolveGatewayId(archiveContract.nodeType(), parentDevice));
        device.setParentDeviceId(parentDevice != null ? parentDevice.getId() : null);
        device.setDeviceName(normalizeRequiredText(dto.getDeviceName()));
        device.setDeviceCode(normalizeRequiredText(dto.getDeviceCode()));
        device.setDeviceSecret(normalizeOptionalText(dto.getDeviceSecret()));
        device.setClientId(normalizeOptionalText(dto.getClientId()));
        device.setUsername(normalizeOptionalText(dto.getUsername()));
        device.setPassword(normalizeOptionalText(dto.getPassword()));
        device.setProtocolCode(archiveContract.protocolCode());
        device.setNodeType(archiveContract.nodeType());
        device.setFirmwareVersion(normalizeOptionalText(dto.getFirmwareVersion()));
        device.setIpAddress(normalizeOptionalText(dto.getIpAddress()));
        device.setAddress(normalizeOptionalText(dto.getAddress()));
        device.setMetadataJson(normalizeOptionalText(dto.getMetadataJson()));

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
        Map<Long, Device> relationDeviceMap = loadRelationDeviceMap(devices);
        return devices.stream()
                .map(device -> toPageVO(device, productMap.get(device.getProductId()), relationDeviceMap))
                .toList();
    }

    private DeviceDetailVO toDetailVO(Device device) {
        Product product = productService.getById(device.getProductId());
        Map<Long, Device> relationDeviceMap = loadDeviceMap(java.util.Arrays.asList(device.getGatewayId(), device.getParentDeviceId()));
        DeviceDetailVO detail = new DeviceDetailVO();
        detail.setId(device.getId());
        detail.setProductId(device.getProductId());
        detail.setGatewayId(device.getGatewayId());
        detail.setParentDeviceId(device.getParentDeviceId());
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
        applyRelationFields(detail, relationDeviceMap);
        return detail;
    }

    private DevicePageVO toPageVO(Device device, Product product, Map<Long, Device> relationDeviceMap) {
        DevicePageVO row = new DevicePageVO();
        row.setId(device.getId());
        row.setProductId(device.getProductId());
        row.setGatewayId(device.getGatewayId());
        row.setParentDeviceId(device.getParentDeviceId());
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
        applyRelationFields(row, relationDeviceMap);
        return row;
    }

    private Map<Long, Product> loadProductMap(List<Long> productIds) {
        List<Long> filteredIds = productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(filteredIds)) {
            return Collections.emptyMap();
        }
        return productService.listByIds(filteredIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left));
    }

    private Map<Long, Device> loadRelationDeviceMap(List<Device> devices) {
        return loadDeviceMap(devices.stream()
                .flatMap(device -> java.util.stream.Stream.of(device.getGatewayId(), device.getParentDeviceId()))
                .toList());
    }

    private Map<Long, Device> loadDeviceMap(List<Long> deviceIds) {
        List<Long> filteredIds = deviceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(filteredIds)) {
            return Collections.emptyMap();
        }
        return lambdaQuery()
                .in(Device::getId, filteredIds)
                .eq(Device::getDeleted, 0)
                .list()
                .stream()
                .collect(Collectors.toMap(Device::getId, device -> device, (left, right) -> left));
    }

    private void applyRelationFields(DeviceDetailVO detail, Map<Long, Device> relationDeviceMap) {
        Device gatewayDevice = findRelationDevice(relationDeviceMap, detail.getGatewayId());
        Device parentDevice = findRelationDevice(relationDeviceMap, detail.getParentDeviceId());
        detail.setGatewayDeviceCode(gatewayDevice != null ? gatewayDevice.getDeviceCode() : null);
        detail.setGatewayDeviceName(gatewayDevice != null ? gatewayDevice.getDeviceName() : null);
        detail.setParentDeviceCode(parentDevice != null ? parentDevice.getDeviceCode() : null);
        detail.setParentDeviceName(parentDevice != null ? parentDevice.getDeviceName() : null);
    }

    private void applyRelationFields(DevicePageVO row, Map<Long, Device> relationDeviceMap) {
        Device gatewayDevice = findRelationDevice(relationDeviceMap, row.getGatewayId());
        Device parentDevice = findRelationDevice(relationDeviceMap, row.getParentDeviceId());
        row.setGatewayDeviceCode(gatewayDevice != null ? gatewayDevice.getDeviceCode() : null);
        row.setGatewayDeviceName(gatewayDevice != null ? gatewayDevice.getDeviceName() : null);
        row.setParentDeviceCode(parentDevice != null ? parentDevice.getDeviceCode() : null);
        row.setParentDeviceName(parentDevice != null ? parentDevice.getDeviceName() : null);
    }

    private Device findRelationDevice(Map<Long, Device> relationDeviceMap, Long relationDeviceId) {
        if (relationDeviceId == null || CollectionUtils.isEmpty(relationDeviceMap)) {
            return null;
        }
        return relationDeviceMap.get(relationDeviceId);
    }

    private Device resolveParentDevice(DeviceAddDTO dto, Long currentDeviceId) {
        String parentDeviceCode = normalizeOptionalText(dto.getParentDeviceCode());
        if (dto.getParentDeviceId() != null && parentDeviceCode != null) {
            throw new BizException("父设备主键和父设备编码不能同时填写");
        }
        if (dto.getParentDeviceId() == null && parentDeviceCode == null) {
            return null;
        }

        Device parentDevice = dto.getParentDeviceId() != null
                ? getRequiredById(dto.getParentDeviceId())
                : getRequiredByCode(parentDeviceCode);

        if (currentDeviceId != null && currentDeviceId.equals(parentDevice.getId())) {
            throw new BizException("父设备不能选择当前设备");
        }
        ensureNoParentCycle(currentDeviceId, parentDevice);
        return parentDevice;
    }

    private void ensureNoParentCycle(Long currentDeviceId, Device parentDevice) {
        if (currentDeviceId == null || parentDevice == null) {
            return;
        }
        Set<Long> visitedIds = new HashSet<>();
        Device current = parentDevice;
        while (current != null) {
            Long currentId = current.getId();
            if (currentId == null || !visitedIds.add(currentId)) {
                return;
            }
            if (currentDeviceId.equals(currentId)) {
                throw new BizException("父设备不能选择当前设备或其子设备");
            }
            if (current.getParentDeviceId() == null) {
                return;
            }
            current = findDeviceOrNull(current.getParentDeviceId());
        }
    }

    private Device findDeviceOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return lambdaQuery()
                .eq(Device::getId, id)
                .eq(Device::getDeleted, 0)
                .one();
    }

    private Long resolveGatewayId(Integer nodeType, Device parentDevice) {
        if (!Integer.valueOf(SUB_DEVICE_NODE_TYPE).equals(nodeType) || parentDevice == null) {
            return null;
        }
        if (Integer.valueOf(GATEWAY_NODE_TYPE).equals(parentDevice.getNodeType())) {
            return parentDevice.getId();
        }
        return parentDevice.getGatewayId();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequiredText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BizException("必填字段不能为空");
        }
        return normalized;
    }

    private DeviceOptionVO toDeviceOption(Device device, Product product) {
        DeviceOptionVO option = new DeviceOptionVO();
        option.setId(device.getId());
        option.setProductId(device.getProductId());
        option.setGatewayId(device.getGatewayId());
        option.setParentDeviceId(device.getParentDeviceId());
        option.setProductKey(product != null ? product.getProductKey() : null);
        option.setProductName(product != null ? product.getProductName() : null);
        option.setDeviceCode(device.getDeviceCode());
        option.setDeviceName(device.getDeviceName());
        option.setNodeType(device.getNodeType());
        option.setOnlineStatus(device.getOnlineStatus());
        option.setDeviceStatus(device.getDeviceStatus());
        return option;
    }

    private record DeviceArchiveContract(Long productId, String protocolCode, Integer nodeType) {
    }
}
