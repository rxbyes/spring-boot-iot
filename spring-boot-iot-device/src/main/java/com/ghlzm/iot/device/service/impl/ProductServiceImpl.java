package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import com.ghlzm.iot.device.vo.ProductOverviewSummaryVO;
import com.ghlzm.iot.device.vo.ProductDeviceStatRow;
import com.ghlzm.iot.device.vo.ProductPageVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 产品服务实现，负责产品台账、库存概览与基础维护。
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private static final int MAX_OBJECT_INSIGHT_CUSTOM_METRIC_COUNT = 20;
    private static final int MAX_OBJECT_INSIGHT_TEMPLATE_LENGTH = 300;

    private final DeviceMapper deviceMapper;
    private final ProductModelMapper productModelMapper;
    private final ProductContractReleaseBatchMapper releaseBatchMapper;
    private final DeviceOnlineSessionService deviceOnlineSessionService;
    private final PublishedProductContractSnapshotService snapshotService;
    private final MetricIdentifierResolver metricIdentifierResolver;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    public ProductServiceImpl(DeviceMapper deviceMapper,
                              ProductModelMapper productModelMapper,
                              ProductContractReleaseBatchMapper releaseBatchMapper,
                              DeviceOnlineSessionService deviceOnlineSessionService,
                              PublishedProductContractSnapshotService snapshotService,
                              MetricIdentifierResolver metricIdentifierResolver) {
        this.deviceMapper = deviceMapper;
        this.productModelMapper = productModelMapper;
        this.releaseBatchMapper = releaseBatchMapper;
        this.deviceOnlineSessionService = deviceOnlineSessionService;
        this.snapshotService = snapshotService;
        this.metricIdentifierResolver = metricIdentifierResolver;
    }

    public ProductServiceImpl(DeviceMapper deviceMapper,
                              ProductModelMapper productModelMapper,
                              ProductContractReleaseBatchMapper releaseBatchMapper,
                              DeviceOnlineSessionService deviceOnlineSessionService) {
        this(
                deviceMapper,
                productModelMapper,
                releaseBatchMapper,
                deviceOnlineSessionService,
                new PublishedProductContractSnapshotServiceImpl(productModelMapper, releaseBatchMapper),
                new DefaultMetricIdentifierResolver()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailVO addProduct(ProductAddDTO dto) {
        String productKey = normalizeRequired(dto.getProductKey(), "产品Key");
        ensureProductKeyUnique(productKey, null);

        Product product = new Product();
        product.setProductKey(productKey);
        applyEditableFields(product, dto);
        save(product);
        return getDetailById(product.getId());
    }

    @Override
    public Product getRequiredById(Long id) {
        Product product = lambdaQuery()
                .eq(Product::getId, id)
                .eq(Product::getDeleted, 0)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + id);
        }
        return product;
    }

    @Override
    public ProductDetailVO getDetailById(Long id) {
        Product product = getRequiredById(id);
        ProductDeviceStatRow stat = loadProductDeviceStatMap(List.of(id)).get(id);
        ProductActivityStatRow activityStat = loadProductActivityStat(id);
        return toDetailVO(product, stat, activityStat);
    }

    @Override
    public ProductOverviewSummaryVO getOverviewSummary(Long id) {
        ProductDetailVO detail = getDetailById(id);
        ProductOverviewSummaryVO summary = new ProductOverviewSummaryVO();
        summary.setProductId(detail.getId());
        summary.setProductKey(detail.getProductKey());
        summary.setProductName(detail.getProductName());
        summary.setProtocolCode(detail.getProtocolCode());
        summary.setNodeType(detail.getNodeType());
        summary.setDataFormat(detail.getDataFormat());
        summary.setManufacturer(detail.getManufacturer());
        summary.setDescription(detail.getDescription());
        summary.setStatus(detail.getStatus());
        summary.setDeviceCount(detail.getDeviceCount());
        summary.setOnlineDeviceCount(detail.getOnlineDeviceCount());
        summary.setLastReportTime(detail.getLastReportTime());
        summary.setFormalFieldCount(resolveFormalFieldCount(id));

        ProductContractReleaseBatch latestBatch = loadLatestReleaseBatch(id);
        if (latestBatch != null) {
            summary.setLatestReleaseBatchId(latestBatch.getId());
            summary.setLatestReleasedFieldCount(latestBatch.getReleasedFieldCount());
            summary.setLatestReleaseStatus(latestBatch.getReleaseStatus());
            summary.setLatestReleaseCreateTime(latestBatch.getCreateTime());
        }
        return summary;
    }

    @Override
    public PageResult<ProductPageVO> pageProducts(String productKey,
                                                  String productName,
                                                  String protocolCode,
                                                  Integer nodeType,
                                                  Integer status,
                                                  Long pageNum,
                                                  Long pageSize) {
        Page<Product> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<Product> result = page(page, buildProductQueryWrapper(
                productKey,
                productName,
                protocolCode,
                nodeType,
                status
        ));
        List<Product> records = result.getRecords();
        Map<Long, ProductDeviceStatRow> statMap = loadProductDeviceStatMap(records.stream().map(Product::getId).toList());
        List<ProductPageVO> rows = records.stream()
                .map(product -> toPageVO(product, statMap.get(product.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDetailVO updateProduct(Long id, ProductAddDTO dto) {
        Product product = getRequiredById(id);
        String productKey = normalizeRequired(dto.getProductKey(), "产品Key");
        if (!productKey.equals(product.getProductKey())) {
            throw new BizException("产品Key创建后不可修改");
        }

        boolean protocolChanged = !product.getProtocolCode().equals(normalizeRequired(dto.getProtocolCode(), "协议编码"));
        boolean nodeTypeChanged = !product.getNodeType().equals(dto.getNodeType());
        Integer targetStatus = resolveProductStatus(dto.getStatus());
        if (shouldDisableProduct(product.getStatus(), targetStatus)) {
            long activeDeviceCount = countActiveRelatedDevices(id);
            if (activeDeviceCount > 0) {
                throw new BizException("产品下仍有 " + activeDeviceCount + " 台启用设备，请先核查库存设备是否仍在使用后再停用");
            }
        }

        applyEditableFields(product, dto);
        updateById(product);

        if (protocolChanged || nodeTypeChanged) {
            syncRelatedDeviceBaseInfo(product);
        }
        return getDetailById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = getRequiredById(id);
        long deviceCount = countRelatedDevices(id);
        if (deviceCount > 0) {
            throw new BizException("产品下仍存在 " + deviceCount + " 台设备，请先处理库存设备后再删除");
        }
        if (!removeById(id)) {
            throw new BizException("产品删除失败，请稍后重试");
        }
    }

    @Override
    public List<Product> listAvailableProducts() {
        return lambdaQuery()
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, ProductStatusEnum.ENABLED.getCode())
                .orderByDesc(Product::getStatus)
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId)
                .list();
    }

    @Override
    public Product getRequiredByProductKey(String productKey) {
        Product product = lambdaQuery()
                .eq(Product::getProductKey, normalizeRequired(productKey, "产品Key"))
                .eq(Product::getDeleted, 0)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + productKey);
        }
        return product;
    }

    private void ensureProductKeyUnique(String productKey, Long excludeId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getProductKey, productKey)
                .eq(Product::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(Product::getId, excludeId);
        }
        Product existing = getBaseMapper().selectOne(wrapper);
        if (existing != null) {
            throw new BizException("产品Key已存在: " + productKey);
        }
    }

    private void applyEditableFields(Product product, ProductAddDTO dto) {
        product.setProductName(normalizeRequired(dto.getProductName(), "产品名称"));
        product.setProtocolCode(normalizeRequired(dto.getProtocolCode(), "协议编码"));
        product.setNodeType(dto.getNodeType());
        product.setDataFormat(resolveOptionalText(dto.getDataFormat(), "JSON"));
        product.setManufacturer(resolveOptionalText(dto.getManufacturer(), null));
        product.setDescription(resolveOptionalText(dto.getDescription(), null));
        product.setMetadataJson(normalizeMetadataJson(product.getId(), dto.getMetadataJson()));
        product.setStatus(resolveProductStatus(dto.getStatus()));
    }

    private LambdaQueryWrapper<Product> buildProductQueryWrapper(String productKey,
                                                                 String productName,
                                                                 String protocolCode,
                                                                 Integer nodeType,
                                                                 Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productKey)) {
            wrapper.like(Product::getProductKey, productKey.trim());
        }
        if (StringUtils.hasText(productName)) {
            String trimmedKeyword = productName.trim();
            wrapper.and(query -> query.like(Product::getProductName, trimmedKeyword)
                    .or()
                    .like(Product::getProductKey, trimmedKeyword)
                    .or()
                    .like(Product::getManufacturer, trimmedKeyword));
        }
        if (StringUtils.hasText(protocolCode)) {
            wrapper.like(Product::getProtocolCode, protocolCode.trim());
        }
        if (nodeType != null) {
            wrapper.eq(Product::getNodeType, nodeType);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.eq(Product::getDeleted, 0)
                .orderByDesc(Product::getUpdateTime)
                .orderByDesc(Product::getId);
        return wrapper;
    }

    private void syncRelatedDeviceBaseInfo(Product product) {
        deviceMapper.update(
                null,
                new LambdaUpdateWrapper<Device>()
                        .eq(Device::getProductId, product.getId())
                        .eq(Device::getDeleted, 0)
                        .set(Device::getProtocolCode, product.getProtocolCode())
                        .set(Device::getNodeType, product.getNodeType())
        );
    }

    private long countRelatedDevices(Long productId) {
        Long count = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getProductId, productId)
                        .eq(Device::getDeleted, 0)
        );
        return count == null ? 0L : count;
    }

    private long countActiveRelatedDevices(Long productId) {
        Long count = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getProductId, productId)
                        .eq(Device::getDeleted, 0)
                        .eq(Device::getDeviceStatus, DeviceStatusEnum.ENABLED.getCode())
        );
        return count == null ? 0L : count;
    }

    private Integer resolveProductStatus(Integer status) {
        return status == null ? ProductStatusEnum.ENABLED.getCode() : status;
    }

    private boolean shouldDisableProduct(Integer currentStatus, Integer targetStatus) {
        return !ProductStatusEnum.DISABLED.getCode().equals(currentStatus)
                && ProductStatusEnum.DISABLED.getCode().equals(targetStatus);
    }

    private Map<Long, ProductDeviceStatRow> loadProductDeviceStatMap(List<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Map.of();
        }
        List<ProductDeviceStatRow> rows = deviceMapper.selectProductStats(productIds);
        if (CollectionUtils.isEmpty(rows)) {
            return Map.of();
        }

        Map<Long, ProductDeviceStatRow> statMap = new LinkedHashMap<>();
        for (ProductDeviceStatRow row : rows) {
            if (row.getProductId() == null) {
                continue;
            }
            statMap.put(row.getProductId(), row);
        }
        return statMap;
    }

    private ProductActivityStatRow loadProductActivityStat(Long productId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime thirtyDaysStart = todayStart.minusDays(30);
        // 活跃设备数按最近上报时间统计，在线时长按在线会话明细聚合。
        ProductActivityStatRow activityStat = deviceMapper.selectProductActivityStat(
                productId,
                todayStart,
                todayStart.minusDays(7),
                thirtyDaysStart
        );
        if (activityStat == null) {
            activityStat = new ProductActivityStatRow();
            activityStat.setProductId(productId);
        }
        ProductActivityStatRow durationStat = deviceOnlineSessionService.loadProductDurationStat(productId, thirtyDaysStart, LocalDateTime.now());
        if (durationStat != null) {
            activityStat.setAvgOnlineDuration(durationStat.getAvgOnlineDuration());
            activityStat.setMaxOnlineDuration(durationStat.getMaxOnlineDuration());
        }
        return activityStat;
    }

    private Integer resolveFormalFieldCount(Long productId) {
        if (productId == null || productModelMapper == null) {
            return 0;
        }
        Long count = productModelMapper.selectCount(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
        );
        return count == null ? 0 : Math.toIntExact(count);
    }

    private ProductContractReleaseBatch loadLatestReleaseBatch(Long productId) {
        if (productId == null || releaseBatchMapper == null) {
            return null;
        }
        List<ProductContractReleaseBatch> batches = releaseBatchMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getProductId, productId)
                        .eq(ProductContractReleaseBatch::getDeleted, 0)
                        .orderByDesc(ProductContractReleaseBatch::getCreateTime)
                        .orderByDesc(ProductContractReleaseBatch::getId)
                        .last("limit 1")
        );
        if (CollectionUtils.isEmpty(batches)) {
            return null;
        }
        return batches.get(0);
    }

    private ProductPageVO toPageVO(Product product, ProductDeviceStatRow stat) {
        ProductPageVO row = new ProductPageVO();
        row.setId(product.getId());
        row.setProductKey(product.getProductKey());
        row.setProductName(product.getProductName());
        row.setProtocolCode(product.getProtocolCode());
        row.setNodeType(product.getNodeType());
        row.setDataFormat(product.getDataFormat());
        row.setManufacturer(product.getManufacturer());
        row.setStatus(product.getStatus());
        row.setDeviceCount(resolveDeviceCount(stat));
        row.setOnlineDeviceCount(resolveOnlineDeviceCount(stat));
        row.setLastReportTime(resolveLastReportTime(stat));
        row.setCreateTime(product.getCreateTime());
        row.setUpdateTime(product.getUpdateTime());
        return row;
    }

    private ProductDetailVO toDetailVO(Product product, ProductDeviceStatRow stat, ProductActivityStatRow activityStat) {
        ProductDetailVO detail = new ProductDetailVO();
        detail.setId(product.getId());
        detail.setProductKey(product.getProductKey());
        detail.setProductName(product.getProductName());
        detail.setProtocolCode(product.getProtocolCode());
        detail.setNodeType(product.getNodeType());
        detail.setDataFormat(product.getDataFormat());
        detail.setManufacturer(product.getManufacturer());
        detail.setDescription(product.getDescription());
        detail.setMetadataJson(product.getMetadataJson());
        detail.setStatus(product.getStatus());
        detail.setDeviceCount(resolveDeviceCount(stat));
        detail.setOnlineDeviceCount(resolveOnlineDeviceCount(stat));
        detail.setLastReportTime(resolveLastReportTime(stat));
        detail.setTodayActiveCount(resolveTodayActiveCount(activityStat));
        detail.setSevenDaysActiveCount(resolveSevenDaysActiveCount(activityStat));
        detail.setThirtyDaysActiveCount(resolveThirtyDaysActiveCount(activityStat));
        detail.setAvgOnlineDuration(resolveAvgOnlineDuration(activityStat));
        detail.setMaxOnlineDuration(resolveMaxOnlineDuration(activityStat));
        detail.setCreateTime(product.getCreateTime());
        detail.setUpdateTime(product.getUpdateTime());
        return detail;
    }

    private long resolveDeviceCount(ProductDeviceStatRow stat) {
        return stat == null || stat.getDeviceCount() == null ? 0L : stat.getDeviceCount();
    }

    private long resolveOnlineDeviceCount(ProductDeviceStatRow stat) {
        return stat == null || stat.getOnlineDeviceCount() == null ? 0L : stat.getOnlineDeviceCount();
    }

    private LocalDateTime resolveLastReportTime(ProductDeviceStatRow stat) {
        return stat == null ? null : stat.getLastReportTime();
    }

    private long resolveTodayActiveCount(ProductActivityStatRow stat) {
        return stat == null || stat.getTodayActiveCount() == null ? 0L : stat.getTodayActiveCount();
    }

    private long resolveSevenDaysActiveCount(ProductActivityStatRow stat) {
        return stat == null || stat.getSevenDaysActiveCount() == null ? 0L : stat.getSevenDaysActiveCount();
    }

    private long resolveThirtyDaysActiveCount(ProductActivityStatRow stat) {
        return stat == null || stat.getThirtyDaysActiveCount() == null ? 0L : stat.getThirtyDaysActiveCount();
    }

    private Long resolveAvgOnlineDuration(ProductActivityStatRow stat) {
        return stat == null ? null : stat.getAvgOnlineDuration();
    }

    private Long resolveMaxOnlineDuration(ProductActivityStatRow stat) {
        return stat == null ? null : stat.getMaxOnlineDuration();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String resolveOptionalText(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private String normalizeMetadataJson(Long productId, String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(metadataJson.trim());
            if (root == null || !root.isObject()) {
                throw new BizException("产品扩展元数据必须是合法JSON对象");
            }
            PublishedProductContractSnapshot snapshot = loadObjectInsightSnapshot(productId);
            Map<String, String> formalIdentifierMap = loadFormalPropertyIdentifierMap(productId);
            normalizeObjectInsightIdentifiers(root.path("objectInsight"), snapshot, formalIdentifierMap);
            deduplicateObjectInsightMetrics(root.path("objectInsight"));
            validateObjectInsightConfig(root.path("objectInsight"));
            return objectMapper.writeValueAsString(root);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("产品扩展元数据必须是合法JSON对象");
        }
    }

    private Map<String, String> loadFormalPropertyIdentifierMap(Long productId) {
        if (productId == null || productModelMapper == null) {
            return Map.of();
        }
        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
                        .orderByAsc(ProductModel::getSortNo)
                        .orderByAsc(ProductModel::getIdentifier)
        );
        if (CollectionUtils.isEmpty(productModels)) {
            return Map.of();
        }
        Map<String, String> formalIdentifierMap = new LinkedHashMap<>();
        for (ProductModel productModel : productModels) {
            if (productModel == null || !StringUtils.hasText(productModel.getIdentifier())) {
                continue;
            }
            String identifier = productModel.getIdentifier().trim();
            formalIdentifierMap.putIfAbsent(identifier.toLowerCase(Locale.ROOT), identifier);
        }
        return formalIdentifierMap;
    }

    private void normalizeObjectInsightIdentifiers(JsonNode objectInsightNode,
                                                   PublishedProductContractSnapshot snapshot,
                                                   Map<String, String> formalIdentifierMap) {
        if (objectInsightNode == null
                || objectInsightNode.isMissingNode()
                || objectInsightNode.isNull()) {
            return;
        }
        JsonNode customMetricsNode = objectInsightNode.path("customMetrics");
        if (!customMetricsNode.isArray()) {
            return;
        }
        for (JsonNode metricNode : customMetricsNode) {
            if (!(metricNode instanceof ObjectNode metricObject)) {
                continue;
            }
            String identifier = resolveMetricText(metricNode, "identifier");
            if (!StringUtils.hasText(identifier)) {
                continue;
            }
            metricObject.put("identifier", normalizeObjectInsightIdentifier(identifier, snapshot, formalIdentifierMap));
        }
    }

    private void validateObjectInsightConfig(JsonNode objectInsightNode) {
        if (objectInsightNode == null || objectInsightNode.isMissingNode() || objectInsightNode.isNull()) {
            return;
        }
        if (!objectInsightNode.isObject()) {
            throw new BizException("对象洞察配置必须是JSON对象");
        }
        JsonNode customMetricsNode = objectInsightNode.path("customMetrics");
        if (customMetricsNode.isMissingNode() || customMetricsNode.isNull()) {
            return;
        }
        if (!customMetricsNode.isArray()) {
            throw new BizException("对象洞察自定义指标必须是数组");
        }
        if (customMetricsNode.size() > MAX_OBJECT_INSIGHT_CUSTOM_METRIC_COUNT) {
            throw new BizException("对象洞察自定义指标最多允许20项");
        }

        Set<String> identifiers = new LinkedHashSet<>();
        for (JsonNode metricNode : customMetricsNode) {
            if (!metricNode.isObject()) {
                throw new BizException("对象洞察自定义指标必须是JSON对象");
            }
            String identifier = requireMetricText(metricNode, "identifier", "对象洞察指标标识不能为空");
            requireMetricText(metricNode, "displayName", "对象洞察指标中文名称不能为空");
            String group = requireMetricText(metricNode, "group", "对象洞察指标分组不能为空");
            if (!"measure".equals(group) && !"status".equals(group) && !"runtime".equals(group)) {
                throw new BizException("对象洞察指标分组仅支持 measure、status 或 runtime");
            }
            identifiers.add(identifier);
            String analysisTemplate = resolveMetricText(metricNode, "analysisTemplate");
            if (analysisTemplate != null && analysisTemplate.length() > MAX_OBJECT_INSIGHT_TEMPLATE_LENGTH) {
                throw new BizException("对象洞察分析描述模板长度不能超过300");
            }
        }
    }

    private void deduplicateObjectInsightMetrics(JsonNode objectInsightNode) {
        if (objectInsightNode == null || objectInsightNode.isMissingNode() || objectInsightNode.isNull()) {
            return;
        }
        if (!(objectInsightNode instanceof ObjectNode objectInsightObject)) {
            return;
        }
        JsonNode customMetricsNode = objectInsightNode.path("customMetrics");
        if (!customMetricsNode.isArray()) {
            return;
        }
        LinkedHashMap<String, JsonNode> lastByIdentifier = new LinkedHashMap<>();
        for (JsonNode metricNode : customMetricsNode) {
            String identifier = resolveMetricText(metricNode, "identifier");
            if (StringUtils.hasText(identifier)) {
                lastByIdentifier.put(identifier, metricNode);
            } else {
                lastByIdentifier.put("@@empty_" + lastByIdentifier.size(), metricNode);
            }
        }
        if (lastByIdentifier.size() == customMetricsNode.size()) {
            return;
        }
        ArrayNode deduped = objectMapper.createArrayNode();
        for (JsonNode metricNode : lastByIdentifier.values()) {
            deduped.add(metricNode);
        }
        objectInsightObject.set("customMetrics", deduped);
    }

    private String normalizeObjectInsightIdentifier(String identifier,
                                                    PublishedProductContractSnapshot snapshot,
                                                    Map<String, String> formalIdentifierMap) {
        if (!StringUtils.hasText(identifier)) {
            return identifier;
        }
        String normalizedIdentifier = identifier.trim();
        String formalIdentifier = CollectionUtils.isEmpty(formalIdentifierMap)
                ? normalizedIdentifier
                : formalIdentifierMap.getOrDefault(normalizedIdentifier.toLowerCase(Locale.ROOT), normalizedIdentifier);
        if (snapshot != null
                && !CollectionUtils.isEmpty(snapshot.publishedIdentifiers())
                && metricIdentifierResolver != null) {
            MetricIdentifierResolution resolution = metricIdentifierResolver.resolveForGovernance(snapshot, identifier);
            if (resolution != null && StringUtils.hasText(resolution.canonicalIdentifier())) {
                if (snapshot.containsPublishedIdentifier(identifier)) {
                    return resolution.canonicalIdentifier();
                }
                if (StringUtils.hasText(formalIdentifier)
                        && !formalIdentifier.equalsIgnoreCase(resolution.canonicalIdentifier())
                        && snapshot.containsPublishedIdentifier(resolution.canonicalIdentifier())) {
                    return formalIdentifier;
                }
            }
            String uniqueTailMatchedIdentifier = resolveUniquePublishedIdentifierByTail(snapshot, normalizedIdentifier);
            if (StringUtils.hasText(uniqueTailMatchedIdentifier)) {
                return uniqueTailMatchedIdentifier;
            }
            throw new BizException("对象洞察指标必须使用已发布合同标识符: " + identifier);
        }
        if (CollectionUtils.isEmpty(formalIdentifierMap)) {
            return normalizedIdentifier;
        }
        return formalIdentifier;
    }

    private String resolveUniquePublishedIdentifierByTail(PublishedProductContractSnapshot snapshot,
                                                          String identifier) {
        if (snapshot == null
                || CollectionUtils.isEmpty(snapshot.publishedIdentifiers())
                || !StringUtils.hasText(identifier)) {
            return null;
        }
        String normalizedInput = identifier.trim();
        if (normalizedInput.contains(".")) {
            return null;
        }
        String inputTail = normalizeIdentifierTail(normalizedInput);
        if (!StringUtils.hasText(inputTail)) {
            return null;
        }
        String matchedIdentifier = null;
        for (String publishedIdentifier : snapshot.publishedIdentifiers()) {
            String publishedTail = normalizeIdentifierTail(publishedIdentifier);
            if (!StringUtils.hasText(publishedTail) || !publishedTail.equalsIgnoreCase(inputTail)) {
                continue;
            }
            if (matchedIdentifier != null && !matchedIdentifier.equalsIgnoreCase(publishedIdentifier)) {
                return null;
            }
            matchedIdentifier = publishedIdentifier;
        }
        return matchedIdentifier;
    }

    private String normalizeIdentifierTail(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        String normalizedIdentifier = identifier.trim();
        int tailSeparator = normalizedIdentifier.lastIndexOf('.');
        if (tailSeparator < 0 || tailSeparator >= normalizedIdentifier.length() - 1) {
            return normalizedIdentifier;
        }
        String tail = normalizedIdentifier.substring(tailSeparator + 1).trim();
        return StringUtils.hasText(tail) ? tail : null;
    }

    private PublishedProductContractSnapshot loadObjectInsightSnapshot(Long productId) {
        if (productId == null || snapshotService == null) {
            return PublishedProductContractSnapshot.empty(productId);
        }
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(productId);
        return snapshot == null ? PublishedProductContractSnapshot.empty(productId) : snapshot;
    }

    private String requireMetricText(JsonNode metricNode, String fieldName, String message) {
        String value = resolveMetricText(metricNode, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        return value;
    }

    private String resolveMetricText(JsonNode metricNode, String fieldName) {
        JsonNode fieldNode = metricNode.path(fieldName);
        if (!fieldNode.isTextual()) {
            return null;
        }
        String value = fieldNode.asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
