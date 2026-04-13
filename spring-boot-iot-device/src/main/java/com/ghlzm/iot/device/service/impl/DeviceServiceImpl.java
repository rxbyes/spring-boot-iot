package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.device.DeviceBindingCapabilitySupport;
import com.ghlzm.iot.common.device.DeviceBindingCapabilityType;
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
import com.ghlzm.iot.device.entity.RiskMetricCatalogReadModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.RiskMetricCatalogReadMapper;
import com.ghlzm.iot.device.service.DeviceInvalidReportStateService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.service.UnregisteredDeviceRosterService;
import com.ghlzm.iot.device.vo.DeviceBatchAddErrorVO;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.device.vo.DeviceReplaceResultVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.OrganizationService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
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
    private static final int REGISTERED_STATUS = 1;
    private static final int UNREGISTERED_STATUS = 0;
    private static final String REGISTERED_SOURCE = "registry";

    private final ProductService productService;
    private final DevicePropertyMapper devicePropertyMapper;
    private final ProductModelMapper productModelMapper;
    private final RiskMetricCatalogReadMapper riskMetricCatalogReadMapper;
    private final UnregisteredDeviceRosterService unregisteredDeviceRosterService;
    private final IotProperties iotProperties;
    private final DeviceInvalidReportStateService invalidReportStateService;
    private final PermissionService permissionService;
    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceServiceImpl(ProductService productService,
                             DevicePropertyMapper devicePropertyMapper,
                             ProductModelMapper productModelMapper,
                             RiskMetricCatalogReadMapper riskMetricCatalogReadMapper,
                             UnregisteredDeviceRosterService unregisteredDeviceRosterService,
                             IotProperties iotProperties,
                             DeviceInvalidReportStateService invalidReportStateService,
                             PermissionService permissionService,
                             OrganizationService organizationService) {
        this.productService = productService;
        this.devicePropertyMapper = devicePropertyMapper;
        this.productModelMapper = productModelMapper;
        this.riskMetricCatalogReadMapper = riskMetricCatalogReadMapper;
        this.unregisteredDeviceRosterService = unregisteredDeviceRosterService;
        this.iotProperties = iotProperties;
        this.invalidReportStateService = invalidReportStateService;
        this.permissionService = permissionService;
        this.organizationService = organizationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceDetailVO addDevice(DeviceAddDTO dto) {
        return addDevice(null, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceDetailVO addDevice(Long currentUserId, DeviceAddDTO dto) {
        Device device = createDeviceRecord(currentUserId, dto);
        resolveInvalidReportState(normalizeRequiredText(dto.getProductKey()), device.getDeviceCode());
        return currentUserId == null ? getDetailById(device.getId()) : getDetailById(currentUserId, device.getId());
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
    public Device getRequiredById(Long currentUserId, Long id) {
        Device device = getRequiredById(id);
        ensureDeviceAccessible(currentUserId, device);
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
    public Device getRequiredByCode(Long currentUserId, String deviceCode) {
        Device device = getRequiredByCode(deviceCode);
        ensureDeviceAccessible(currentUserId, device);
        return device;
    }

    @Override
    public DeviceDetailVO getDetailById(Long id) {
        return getDetailById(null, id);
    }

    @Override
    public DeviceDetailVO getDetailById(Long currentUserId, Long id) {
        Device device = getRequiredById(id);
        ensureDeviceAccessible(currentUserId, device);
        return toDetailVO(currentUserId, device);
    }

    @Override
    public DeviceDetailVO getDetailByCode(String deviceCode) {
        return getDetailByCode(null, deviceCode);
    }

    @Override
    public DeviceDetailVO getDetailByCode(Long currentUserId, String deviceCode) {
        Device device = getRequiredByCode(deviceCode);
        ensureDeviceAccessible(currentUserId, device);
        return toDetailVO(currentUserId, device);
    }

    @Override
    public PageResult<DevicePageVO> pageDevices(Long deviceId,
                                                String keyword,
                                                String productKey,
                                                String productName,
                                                String deviceCode,
                                                String deviceName,
                                                Integer onlineStatus,
                                                Integer activateStatus,
                                                Integer deviceStatus,
                                                Integer registrationStatus,
                                                Long pageNum,
                                                Long pageSize) {
        return pageDevices(null,
                deviceId,
                keyword,
                productKey,
                productName,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus,
                registrationStatus,
                pageNum,
                pageSize);
    }

    @Override
    public PageResult<DevicePageVO> pageDevices(Long currentUserId,
                                                Long deviceId,
                                                String keyword,
                                                String productKey,
                                                String productName,
                                                String deviceCode,
                                                String deviceName,
                                                Integer onlineStatus,
                                                Integer activateStatus,
                                                Integer deviceStatus,
                                                Integer registrationStatus,
                                                Long pageNum,
                                                Long pageSize) {
        Page<Device> page = PageQueryUtils.buildPage(pageNum, pageSize);
        long current = page.getCurrent();
        long size = page.getSize();
        if (Integer.valueOf(UNREGISTERED_STATUS).equals(registrationStatus)) {
            return pageUnregisteredDevices(currentUserId, deviceId, keyword, productKey, productName, deviceCode, deviceName,
                    onlineStatus, activateStatus, deviceStatus, current, size);
        }

        boolean hasExplicitProductFilter = StringUtils.hasText(productKey) || StringUtils.hasText(productName);
        List<Long> filteredProductIds = resolveFilteredProductIds(currentUserId, productKey, productName);
        List<Long> keywordMatchedProductIds = resolveKeywordMatchedProductIds(currentUserId, keyword);

        if (Integer.valueOf(REGISTERED_STATUS).equals(registrationStatus)) {
            if (hasExplicitProductFilter && CollectionUtils.isEmpty(filteredProductIds)) {
                return PageResult.empty(current, size);
            }
            return pageRegisteredDevices(currentUserId,
                    deviceId,
                    keyword,
                    filteredProductIds,
                    keywordMatchedProductIds,
                    deviceCode,
                    deviceName,
                    onlineStatus,
                    activateStatus,
                    deviceStatus,
                    current,
                    size);
        }

        return pageCombinedDevices(currentUserId,
                deviceId,
                keyword,
                filteredProductIds,
                keywordMatchedProductIds,
                productKey,
                productName,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus,
                current,
                size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceDetailVO updateDevice(Long id, DeviceAddDTO dto) {
        return updateDevice(null, id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceDetailVO updateDevice(Long currentUserId, Long id, DeviceAddDTO dto) {
        Device device = getRequiredById(id);
        ensureDeviceAccessible(currentUserId, device);
        Product product = resolveAccessibleProduct(currentUserId, normalizeRequiredText(dto.getProductKey()));
        resolveDeviceArchiveContract(product);
        ensureDeviceCodeUnique(resolveEffectiveTenantId(currentUserId, device.getTenantId()),
                normalizeRequiredText(dto.getDeviceCode()),
                id);
        applyEditableFields(currentUserId, device, dto, product, false);
        updateById(device);
        return currentUserId == null ? getDetailById(id) : getDetailById(currentUserId, id);
    }

    @Override
    public DeviceBatchAddResultVO batchAddDevices(List<DeviceAddDTO> items) {
        return batchAddDevices(null, items);
    }

    @Override
    public DeviceBatchAddResultVO batchAddDevices(Long currentUserId, List<DeviceAddDTO> items) {
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
                Device device = createDeviceRecord(currentUserId, item);
                resolveInvalidReportState(normalizeRequiredText(item.getProductKey()), device.getDeviceCode());
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
        return replaceDevice(null, id, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceReplaceResultVO replaceDevice(Long currentUserId, Long id, DeviceReplaceDTO dto) {
        Device sourceDevice = getRequiredById(id);
        ensureDeviceAccessible(currentUserId, sourceDevice);
        if (sourceDevice.getDeviceCode().equals(normalizeRequiredText(dto.getDeviceCode()))) {
            throw new BizException("新设备编码不能与原设备编码相同");
        }

        Product targetProduct = resolveReplacementProduct(currentUserId, sourceDevice, dto.getProductKey());
        LocalDateTime replacementTime = LocalDateTime.now();
        DeviceAddDTO replacementDto = buildReplacementCreateDTO(sourceDevice, dto, targetProduct, replacementTime);
        Device replacementDevice = createDeviceRecord(currentUserId, replacementDto);
        resolveInvalidReportState(targetProduct.getProductKey(), replacementDevice.getDeviceCode());

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
        deleteDevice(null, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long currentUserId, Long id) {
        Device device = getRequiredById(id);
        ensureDeviceAccessible(currentUserId, device);
        if (!removeById(id)) {
            throw new BizException("设备删除失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDevices(List<Long> ids) {
        batchDeleteDevices(null, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDevices(Long currentUserId, List<Long> ids) {
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
        devices.forEach(device -> ensureDeviceAccessible(currentUserId, device));
        List<Long> existingIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        if (!removeByIds(existingIds)) {
            throw new BizException("设备批量删除失败，请稍后重试");
        }
    }

    @Override
    public List<DeviceProperty> listProperties(String deviceCode) {
        return listProperties(null, deviceCode);
    }

    @Override
    public List<DeviceProperty> listProperties(Long currentUserId, String deviceCode) {
        Device device = getRequiredByCode(deviceCode);
        ensureDeviceAccessible(currentUserId, device);
        List<DeviceProperty> properties = devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByDesc(DeviceProperty::getUpdateTime)
        );
        overlayLatestPropertyNames(device, properties);
        return properties;
    }

    @Override
    public List<DeviceOptionVO> listDeviceOptions(boolean includeDisabled) {
        return listDeviceOptions(null, includeDisabled);
    }

    @Override
    public List<DeviceOptionVO> listDeviceOptions(Long currentUserId, boolean includeDisabled) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeleted, 0)
                .orderByDesc(Device::getCreateTime);
        applyDeviceScope(wrapper, currentUserId);
        if (!includeDisabled) {
            wrapper.eq(Device::getDeviceStatus, DeviceStatusEnum.ENABLED.getCode());
        }
        List<Device> devices = list(wrapper);
        Map<Long, Product> productMap = loadProductMap(resolveScopedTenantId(currentUserId), devices.stream().map(Device::getProductId).toList());
        Set<Long> productsWithFormalMetrics = loadProductsWithFormalMetrics(devices.stream().map(Device::getProductId).toList());
        return devices.stream()
                .map(device -> toDeviceOption(
                        device,
                        productMap.get(device.getProductId()),
                        productsWithFormalMetrics.contains(device.getProductId())
                ))
                .toList();
    }

    @Override
    public List<DeviceMetricOptionVO> listMetricOptions(Long deviceId) {
        return listMetricOptions(null, deviceId);
    }

    @Override
    public List<DeviceMetricOptionVO> listMetricOptions(Long currentUserId, Long deviceId) {
        Device device = getRequiredById(deviceId);
        ensureDeviceAccessible(currentUserId, device);
        Map<String, DeviceMetricOptionVO> optionMap = new LinkedHashMap<>();
        Map<String, Long> publishedRiskMetricIds = loadPublishedRiskMetricIds(device.getProductId());

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
            option.setRiskMetricId(publishedRiskMetricIds.get(option.getIdentifier()));
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
                option.setRiskMetricId(publishedRiskMetricIds.get(deviceProperty.getIdentifier()));
                return option;
            });
        }

        return optionMap.values().stream().collect(Collectors.toList());
    }

    private Map<String, Long> loadPublishedRiskMetricIds(Long productId) {
        if (productId == null) {
            return Map.of();
        }
        List<RiskMetricCatalogReadModel> rows = riskMetricCatalogReadMapper.selectList(
                new LambdaQueryWrapper<RiskMetricCatalogReadModel>()
                        .eq(RiskMetricCatalogReadModel::getProductId, productId)
                        .eq(RiskMetricCatalogReadModel::getEnabled, 1)
                        .eq(RiskMetricCatalogReadModel::getDeleted, 0)
        );
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        return rows.stream()
                .filter(row -> StringUtils.hasText(row.getContractIdentifier()) && row.getId() != null)
                .collect(Collectors.toMap(
                        RiskMetricCatalogReadModel::getContractIdentifier,
                        RiskMetricCatalogReadModel::getId,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private void overlayLatestPropertyNames(Device device, List<DeviceProperty> properties) {
        if (device == null || device.getProductId() == null || CollectionUtils.isEmpty(properties)) {
            return;
        }
        Map<String, String> latestNameMap = productModelMapper.selectList(
                        new LambdaQueryWrapper<ProductModel>()
                                .eq(ProductModel::getProductId, device.getProductId())
                                .eq(ProductModel::getModelType, "property")
                                .eq(ProductModel::getDeleted, 0)
                ).stream()
                .filter(model -> StringUtils.hasText(model.getIdentifier()) && StringUtils.hasText(model.getModelName()))
                .collect(Collectors.toMap(
                        ProductModel::getIdentifier,
                        ProductModel::getModelName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (latestNameMap.isEmpty()) {
            return;
        }
        properties.forEach(property -> {
            String latestName = latestNameMap.get(property.getIdentifier());
            if (StringUtils.hasText(latestName)) {
                property.setPropertyName(latestName);
            }
        });
    }

    private Device createDeviceRecord(Long currentUserId, DeviceAddDTO dto) {
        Product product = resolveAccessibleProduct(currentUserId, normalizeRequiredText(dto.getProductKey()));
        resolveDeviceArchiveContract(product);
        ensureDeviceCodeUnique(resolveEffectiveTenantId(currentUserId, product.getTenantId()),
                normalizeRequiredText(dto.getDeviceCode()),
                null);

        Device device = new Device();
        device.setTenantId(resolveEffectiveTenantId(currentUserId, product.getTenantId()));
        // 创建设备时统一从产品继承协议、节点类型，并落初始化状态字段。
        applyEditableFields(currentUserId, device, dto, product, true);
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

    private void ensureDeviceCodeUnique(Long tenantId, String deviceCode, Long excludeId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0);
        if (tenantId != null) {
            wrapper.eq(Device::getTenantId, tenantId);
        }
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

    private Product resolveReplacementProduct(Long currentUserId, Device sourceDevice, String productKey) {
        String normalizedProductKey = normalizeOptionalText(productKey);
        if (normalizedProductKey != null) {
            return resolveAccessibleProduct(currentUserId, normalizedProductKey);
        }
        Product product = productService.getById(sourceDevice.getProductId());
        if (product == null || product.getDeleted() != null && product.getDeleted() == 1) {
            throw new BizException("原设备所属产品不存在，无法执行更换");
        }
        ensureProductAccessible(currentUserId, product);
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

    private void resolveInvalidReportState(String productKey, String deviceCode) {
        if (!StringUtils.hasText(productKey) || !StringUtils.hasText(deviceCode)) {
            return;
        }
        invalidReportStateService.markResolvedByDevice(productKey.trim(), deviceCode.trim(), LocalDateTime.now());
    }

    private void applyEditableFields(Long currentUserId, Device device, DeviceAddDTO dto, Product product, boolean initializeDefaults) {
        DeviceArchiveContract archiveContract = resolveDeviceArchiveContract(product);
        Device parentDevice = resolveParentDevice(currentUserId, dto, device.getId());
        DeviceOrganizationAssignment organizationAssignment = resolveWritableOrganizationAssignment(
                currentUserId,
                resolveEffectiveTenantId(currentUserId, product.getTenantId())
        );
        device.setProductId(archiveContract.productId());
        device.setTenantId(resolveEffectiveTenantId(currentUserId, product.getTenantId()));
        if (organizationAssignment != null) {
            device.setOrgId(organizationAssignment.orgId());
            device.setOrgName(organizationAssignment.orgName());
        }
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

    private LambdaQueryWrapper<Device> buildDeviceQueryWrapper(Long currentUserId,
                                                               Long deviceId,
                                                               String keyword,
                                                               List<Long> filteredProductIds,
                                                               List<Long> keywordMatchedProductIds,
                                                               String deviceCode,
                                                               String deviceName,
                                                               Integer onlineStatus,
                                                               Integer activateStatus,
                                                               Integer deviceStatus) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        applyDeviceScope(wrapper, currentUserId);
        if (deviceId != null) {
            wrapper.eq(Device::getId, deviceId);
        }
        if (!CollectionUtils.isEmpty(filteredProductIds)) {
            wrapper.in(Device::getProductId, filteredProductIds);
        }
        String normalizedKeyword = normalizeOptionalText(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(condition -> {
                condition.like(Device::getDeviceCode, normalizedKeyword)
                        .or()
                        .like(Device::getDeviceName, normalizedKeyword);
                if (!CollectionUtils.isEmpty(keywordMatchedProductIds)) {
                    condition.or().in(Device::getProductId, keywordMatchedProductIds);
                }
            });
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

    private List<Long> resolveFilteredProductIds(Long currentUserId, String productKey, String productName) {
        String normalizedProductKey = normalizeOptionalText(productKey);
        String normalizedProductName = normalizeOptionalText(productName);
        if (!StringUtils.hasText(normalizedProductKey) && !StringUtils.hasText(normalizedProductName)) {
            return null;
        }
        Long tenantId = resolveScopedTenantId(currentUserId);
        return productService.lambdaQuery()
                .eq(tenantId != null, Product::getTenantId, tenantId)
                .like(StringUtils.hasText(normalizedProductKey), Product::getProductKey, normalizedProductKey)
                .like(StringUtils.hasText(normalizedProductName), Product::getProductName, normalizedProductName)
                .eq(Product::getDeleted, 0)
                .list()
                .stream()
                .map(Product::getId)
                .toList();
    }

    private List<Long> resolveKeywordMatchedProductIds(Long currentUserId, String keyword) {
        String normalizedKeyword = normalizeOptionalText(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return null;
        }
        Long tenantId = resolveScopedTenantId(currentUserId);
        return productService.lambdaQuery()
                .eq(tenantId != null, Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .and(wrapper -> wrapper.like(Product::getProductKey, normalizedKeyword)
                        .or()
                        .like(Product::getProductName, normalizedKeyword))
                .list()
                .stream()
                .map(Product::getId)
                .toList();
    }

    private List<DevicePageVO> toPageVOList(Long currentUserId, List<Device> devices) {
        if (CollectionUtils.isEmpty(devices)) {
            return List.of();
        }
        Long tenantId = resolveScopedTenantId(currentUserId);
        Map<Long, Product> productMap = loadProductMap(tenantId, devices.stream().map(Device::getProductId).toList());
        Map<Long, Device> relationDeviceMap = loadRelationDeviceMap(tenantId, devices);
        return devices.stream()
                .map(device -> toPageVO(device, productMap.get(device.getProductId()), relationDeviceMap))
                .toList();
    }

    private DeviceDetailVO toDetailVO(Long currentUserId, Device device) {
        Long tenantId = resolveEffectiveTenantId(currentUserId, device == null ? null : device.getTenantId());
        Product product = loadProductMap(tenantId, List.of(device.getProductId())).get(device.getProductId());
        Map<Long, Device> relationDeviceMap = loadDeviceMap(tenantId, java.util.Arrays.asList(device.getGatewayId(), device.getParentDeviceId()));
        DeviceDetailVO detail = new DeviceDetailVO();
        detail.setId(device.getId());
        detail.setProductId(device.getProductId());
        detail.setOrgId(device.getOrgId());
        detail.setGatewayId(device.getGatewayId());
        detail.setParentDeviceId(device.getParentDeviceId());
        detail.setProductKey(product != null ? product.getProductKey() : null);
        detail.setProductName(product != null ? product.getProductName() : null);
        detail.setOrgName(device.getOrgName());
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
        detail.setRegistrationStatus(REGISTERED_STATUS);
        detail.setAssetSourceType(REGISTERED_SOURCE);
        applyRelationFields(detail, relationDeviceMap);
        return detail;
    }

    private DevicePageVO toPageVO(Device device, Product product, Map<Long, Device> relationDeviceMap) {
        DevicePageVO row = new DevicePageVO();
        row.setId(device.getId());
        row.setProductId(device.getProductId());
        row.setOrgId(device.getOrgId());
        row.setGatewayId(device.getGatewayId());
        row.setParentDeviceId(device.getParentDeviceId());
        row.setProductKey(product != null ? product.getProductKey() : null);
        row.setProductName(product != null ? product.getProductName() : null);
        row.setOrgName(device.getOrgName());
        row.setDeviceName(device.getDeviceName());
        row.setDeviceCode(device.getDeviceCode());
        row.setProtocolCode(device.getProtocolCode());
        row.setNodeType(device.getNodeType());
        row.setOnlineStatus(device.getOnlineStatus());
        row.setActivateStatus(device.getActivateStatus());
        row.setDeviceStatus(device.getDeviceStatus());
        row.setRegistrationStatus(REGISTERED_STATUS);
        row.setAssetSourceType(REGISTERED_SOURCE);
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

    private PageResult<DevicePageVO> pageRegisteredDevices(Long currentUserId,
                                                           Long deviceId,
                                                           String keyword,
                                                           List<Long> filteredProductIds,
                                                           List<Long> keywordMatchedProductIds,
                                                           String deviceCode,
                                                           String deviceName,
                                                           Integer onlineStatus,
                                                           Integer activateStatus,
                                                           Integer deviceStatus,
                                                           long pageNum,
                                                           long pageSize) {
        if (filteredProductIds != null && filteredProductIds.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Page<Device> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<Device> result = page(page, buildDeviceQueryWrapper(
                currentUserId,
                deviceId,
                keyword,
                filteredProductIds,
                keywordMatchedProductIds,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus
        ));
        List<DevicePageVO> records = toPageVOList(currentUserId, result.getRecords());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    private PageResult<DevicePageVO> pageUnregisteredDevices(Long currentUserId,
                                                             Long deviceId,
                                                             String keyword,
                                                             String productKey,
                                                             String productName,
                                                             String deviceCode,
                                                             String deviceName,
                                                             Integer onlineStatus,
                                                             Integer activateStatus,
                                                             Integer deviceStatus,
                                                             long pageNum,
                                                             long pageSize) {
        if (hasOrganizationRestrictedScope(currentUserId)) {
            return PageResult.empty(pageNum, pageSize);
        }
        if (!canMatchUnregisteredDevices(deviceId, deviceName, onlineStatus, activateStatus, deviceStatus)) {
            return PageResult.empty(pageNum, pageSize);
        }

        long offset = Math.max(pageNum - 1, 0L) * pageSize;
        Long tenantId = resolveScopedTenantId(currentUserId);
        long total = tenantId == null
                ? unregisteredDeviceRosterService.countByFilters(
                normalizeOptionalText(keyword),
                normalizeOptionalText(productKey),
                normalizeOptionalText(productName),
                normalizeOptionalText(deviceCode))
                : unregisteredDeviceRosterService.countByFilters(
                tenantId,
                normalizeOptionalText(keyword),
                normalizeOptionalText(productKey),
                normalizeOptionalText(productName),
                normalizeOptionalText(deviceCode));
        if (total <= 0L) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<DevicePageVO> records = tenantId == null
                ? unregisteredDeviceRosterService.listByFilters(
                normalizeOptionalText(keyword),
                normalizeOptionalText(productKey),
                normalizeOptionalText(productName),
                normalizeOptionalText(deviceCode),
                offset,
                pageSize
        )
                : unregisteredDeviceRosterService.listByFilters(
                tenantId,
                normalizeOptionalText(keyword),
                normalizeOptionalText(productKey),
                normalizeOptionalText(productName),
                normalizeOptionalText(deviceCode),
                offset,
                pageSize
        );
        return PageResult.of(total, pageNum, pageSize, records);
    }

    private PageResult<DevicePageVO> pageCombinedDevices(Long currentUserId,
                                                         Long deviceId,
                                                         String keyword,
                                                         List<Long> filteredProductIds,
                                                         List<Long> keywordMatchedProductIds,
                                                         String productKey,
                                                         String productName,
                                                         String deviceCode,
                                                         String deviceName,
                                                         Integer onlineStatus,
                                                         Integer activateStatus,
                                                         Integer deviceStatus,
                                                         long pageNum,
                                                         long pageSize) {
        long registeredTotal = countRegisteredDevices(currentUserId,
                keyword,
                filteredProductIds,
                keywordMatchedProductIds,
                deviceId,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus);
        Long tenantId = resolveScopedTenantId(currentUserId);
        long unregisteredTotal = !hasOrganizationRestrictedScope(currentUserId)
                && canMatchUnregisteredDevices(deviceId, deviceName, onlineStatus, activateStatus, deviceStatus)
                ? (tenantId == null
                ? unregisteredDeviceRosterService.countByFilters(
                normalizeOptionalText(keyword),
                normalizeOptionalText(productKey),
                normalizeOptionalText(productName),
                normalizeOptionalText(deviceCode))
                : unregisteredDeviceRosterService.countByFilters(
                tenantId,
                normalizeOptionalText(keyword),
                normalizeOptionalText(productKey),
                normalizeOptionalText(productName),
                normalizeOptionalText(deviceCode)))
                : 0L;
        long total = registeredTotal + unregisteredTotal;
        if (total <= 0L) {
            return PageResult.empty(pageNum, pageSize);
        }

        long offset = Math.max(pageNum - 1, 0L) * pageSize;
        List<DevicePageVO> records = new ArrayList<>();
        if (offset < registeredTotal) {
            PageResult<DevicePageVO> registeredPage = pageRegisteredDevices(currentUserId,
                    deviceId,
                    keyword,
                    filteredProductIds,
                    keywordMatchedProductIds,
                    deviceCode,
                    deviceName,
                    onlineStatus,
                    activateStatus,
                    deviceStatus,
                    pageNum,
                    pageSize);
            records.addAll(registeredPage.getRecords());
        }

        long remaining = pageSize - records.size();
        if (remaining > 0L && unregisteredTotal > 0L) {
            long unregisteredOffset = Math.max(0L, offset - registeredTotal);
            records.addAll(tenantId == null
                    ? unregisteredDeviceRosterService.listByFilters(
                    normalizeOptionalText(keyword),
                    normalizeOptionalText(productKey),
                    normalizeOptionalText(productName),
                    normalizeOptionalText(deviceCode),
                    unregisteredOffset,
                    remaining
            )
                    : unregisteredDeviceRosterService.listByFilters(
                    tenantId,
                    normalizeOptionalText(keyword),
                    normalizeOptionalText(productKey),
                    normalizeOptionalText(productName),
                    normalizeOptionalText(deviceCode),
                    unregisteredOffset,
                    remaining
            ));
        }
        return PageResult.of(total, pageNum, pageSize, records);
    }

    private long countRegisteredDevices(Long currentUserId,
                                        String keyword,
                                        List<Long> filteredProductIds,
                                        List<Long> keywordMatchedProductIds,
                                        Long deviceId,
                                        String deviceCode,
                                        String deviceName,
                                        Integer onlineStatus,
                                        Integer activateStatus,
                                        Integer deviceStatus) {
        if (filteredProductIds != null && filteredProductIds.isEmpty()) {
            return 0L;
        }
        return count(buildDeviceQueryWrapper(
                currentUserId,
                deviceId,
                keyword,
                filteredProductIds,
                keywordMatchedProductIds,
                deviceCode,
                deviceName,
                onlineStatus,
                activateStatus,
                deviceStatus
        ));
    }

    private boolean canMatchUnregisteredDevices(Long deviceId,
                                                String deviceName,
                                                Integer onlineStatus,
                                                Integer activateStatus,
                                                Integer deviceStatus) {
        return deviceId == null
                && !StringUtils.hasText(deviceName)
                && onlineStatus == null
                && activateStatus == null
                && deviceStatus == null;
    }

    private Map<Long, Product> loadProductMap(Long tenantId, List<Long> productIds) {
        List<Long> filteredIds = productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(filteredIds)) {
            return Collections.emptyMap();
        }
        if (tenantId == null) {
            List<Product> products = productService.listByIds(filteredIds);
            if (CollectionUtils.isEmpty(products)) {
                return Collections.emptyMap();
            }
            return products
                    .stream()
                    .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left));
        }
        List<Product> products = productService.lambdaQuery()
                .in(Product::getId, filteredIds)
                .eq(tenantId != null, Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0)
                .list();
        if (CollectionUtils.isEmpty(products)) {
            return Collections.emptyMap();
        }
        return products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left));
    }

    private Map<Long, Device> loadRelationDeviceMap(Long tenantId, List<Device> devices) {
        return loadDeviceMap(tenantId, devices.stream()
                .flatMap(device -> java.util.stream.Stream.of(device.getGatewayId(), device.getParentDeviceId()))
                .toList());
    }

    private Map<Long, Device> loadDeviceMap(Long tenantId, List<Long> deviceIds) {
        List<Long> filteredIds = deviceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(filteredIds)) {
            return Collections.emptyMap();
        }
        return lambdaQuery()
                .in(Device::getId, filteredIds)
                .eq(tenantId != null, Device::getTenantId, tenantId)
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

    private Device resolveParentDevice(Long currentUserId, DeviceAddDTO dto, Long currentDeviceId) {
        String parentDeviceCode = normalizeOptionalText(dto.getParentDeviceCode());
        if (dto.getParentDeviceId() != null && parentDeviceCode != null) {
            throw new BizException("父设备主键和父设备编码不能同时填写");
        }
        if (dto.getParentDeviceId() == null && parentDeviceCode == null) {
            return null;
        }

        Device parentDevice = dto.getParentDeviceId() != null
                ? getRequiredById(currentUserId, dto.getParentDeviceId())
                : getRequiredByCode(currentUserId, parentDeviceCode);

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

    private DeviceOrganizationAssignment resolveWritableOrganizationAssignment(Long currentUserId, Long tenantId) {
        if (currentUserId == null) {
            return null;
        }
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null) {
            throw new BizException("无法解析当前登录人的机构上下文");
        }
        Long orgId = context.orgId();
        if (orgId == null || orgId <= 0) {
            throw new BizException("当前账号未绑定主机构，禁止维护设备归属");
        }
        if (organizationService == null) {
            throw new BizException("无法解析当前登录人的机构上下文");
        }
        Organization organization = organizationService.getById(orgId);
        if (organization == null
                || Integer.valueOf(1).equals(organization.getDeleted())
                || !Integer.valueOf(1).equals(organization.getStatus())
                || !StringUtils.hasText(organization.getOrgName())) {
            throw new BizException("当前账号主机构不存在或已不可用，禁止维护设备归属");
        }
        if (tenantId != null && organization.getTenantId() != null && !tenantId.equals(organization.getTenantId())) {
            throw new BizException("当前账号主机构不存在或已不可用，禁止维护设备归属");
        }
        return new DeviceOrganizationAssignment(organization.getId(), organization.getOrgName());
    }

    private boolean hasOrganizationRestrictedScope(Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return false;
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        return dataScopeType == DataScopeType.ORG || dataScopeType == DataScopeType.ORG_AND_CHILDREN;
    }

    private void applyDeviceScope(LambdaQueryWrapper<Device> wrapper, Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null) {
            return;
        }
        if (!context.superAdmin() && context.tenantId() != null) {
            wrapper.eq(Device::getTenantId, context.tenantId());
        }
        if (context.superAdmin()) {
            return;
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.TENANT) {
            return;
        }
        if (dataScopeType == DataScopeType.ORG) {
            if (context.orgId() == null || context.orgId() <= 0) {
                wrapper.eq(Device::getId, -1L);
                return;
            }
            wrapper.eq(Device::getOrgId, context.orgId());
            return;
        }
        Set<Long> accessibleOrgIds = listAccessibleOrganizationIds(currentUserId);
        if (CollectionUtils.isEmpty(accessibleOrgIds)) {
            wrapper.eq(Device::getId, -1L);
            return;
        }
        wrapper.in(Device::getOrgId, accessibleOrgIds);
    }

    private Product resolveAccessibleProduct(Long currentUserId, String productKey) {
        String normalizedProductKey = normalizeRequiredText(productKey);
        Long tenantId = resolveScopedTenantId(currentUserId);
        if (tenantId == null) {
            return productService.getRequiredByProductKey(normalizedProductKey);
        }
        Product product = productService.lambdaQuery()
                .eq(Product::getProductKey, normalizedProductKey)
                .eq(Product::getDeleted, 0)
                .eq(tenantId != null, Product::getTenantId, tenantId)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + normalizedProductKey);
        }
        ensureProductAccessible(currentUserId, product);
        return product;
    }

    private void ensureProductAccessible(Long currentUserId, Product product) {
        if (currentUserId == null || product == null) {
            return;
        }
        Long tenantId = resolveScopedTenantId(currentUserId);
        if (tenantId != null && !tenantId.equals(product.getTenantId())) {
            throw new BizException("产品不存在或无权访问");
        }
    }

    private void ensureDeviceAccessible(Long currentUserId, Device device) {
        if (currentUserId == null || device == null) {
            return;
        }
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return;
        }
        if (context.tenantId() != null && !context.tenantId().equals(device.getTenantId())) {
            throw new BizException("设备不存在或无权访问");
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.TENANT) {
            return;
        }
        if (dataScopeType == DataScopeType.ORG) {
            if (!Objects.equals(context.orgId(), device.getOrgId())) {
                throw new BizException("设备不存在或无权访问");
            }
            return;
        }
        Set<Long> accessibleOrgIds = listAccessibleOrganizationIds(currentUserId);
        if (CollectionUtils.isEmpty(accessibleOrgIds) || device.getOrgId() == null || !accessibleOrgIds.contains(device.getOrgId())) {
            throw new BizException("设备不存在或无权访问");
        }
    }

    private Long resolveScopedTenantId(Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return null;
        }
        return context.tenantId();
    }

    private Long resolveEffectiveTenantId(Long currentUserId, Long fallbackTenantId) {
        Long tenantId = resolveScopedTenantId(currentUserId);
        return tenantId != null ? tenantId : fallbackTenantId;
    }

    private DataPermissionContext resolveDataPermissionContext(Long currentUserId) {
        if (currentUserId == null || permissionService == null) {
            return null;
        }
        return permissionService.getDataPermissionContext(currentUserId);
    }

    private DataScopeType normalizeDeviceDataScope(DataScopeType dataScopeType) {
        if (dataScopeType == null) {
            return DataScopeType.TENANT;
        }
        return dataScopeType == DataScopeType.SELF ? DataScopeType.ORG : dataScopeType;
    }

    private Set<Long> listAccessibleOrganizationIds(Long currentUserId) {
        if (currentUserId == null || permissionService == null) {
            return Set.of();
        }
        Set<Long> orgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
        if (CollectionUtils.isEmpty(orgIds)) {
            return Set.of();
        }
        return orgIds;
    }

    private DeviceOptionVO toDeviceOption(Device device, Product product, boolean hasFormalMetrics) {
        DeviceOptionVO option = new DeviceOptionVO();
        DeviceBindingCapabilityType capabilityType = DeviceBindingCapabilitySupport.resolve(
                product == null ? null : product.getProductKey(),
                product == null ? null : product.getProductName(),
                hasFormalMetrics
        );
        option.setId(device.getId());
        option.setProductId(device.getProductId());
        option.setOrgId(device.getOrgId());
        option.setGatewayId(device.getGatewayId());
        option.setParentDeviceId(device.getParentDeviceId());
        option.setProductKey(product != null ? product.getProductKey() : null);
        option.setProductName(product != null ? product.getProductName() : null);
        option.setOrgName(device.getOrgName());
        option.setDeviceCode(device.getDeviceCode());
        option.setDeviceName(device.getDeviceName());
        option.setNodeType(device.getNodeType());
        option.setOnlineStatus(device.getOnlineStatus());
        option.setDeviceStatus(device.getDeviceStatus());
        option.setDeviceCapabilityType(capabilityType.name());
        option.setSupportsMetricBinding(DeviceBindingCapabilitySupport.supportsMetricBinding(capabilityType, hasFormalMetrics));
        option.setAiEventExpandable(DeviceBindingCapabilitySupport.isAiEventExpandable(capabilityType));
        return option;
    }

    private Set<Long> loadProductsWithFormalMetrics(List<Long> productIds) {
        List<Long> normalizedProductIds = productIds == null
                ? List.of()
                : productIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedProductIds.isEmpty()) {
            return Set.of();
        }
        return riskMetricCatalogReadMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalogReadModel>()
                        .in(RiskMetricCatalogReadModel::getProductId, normalizedProductIds)
                        .eq(RiskMetricCatalogReadModel::getEnabled, 1)
                        .eq(RiskMetricCatalogReadModel::getDeleted, 0))
                .stream()
                .map(RiskMetricCatalogReadModel::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private record DeviceOrganizationAssignment(Long orgId, String orgName) {
    }

    private record DeviceArchiveContract(Long productId, String protocolCode, Integer nodeType) {
    }
}
