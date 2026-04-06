package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 风险治理缺口服务实现。
 */
@Service
public class RiskGovernanceServiceImpl implements RiskGovernanceService {

    private final DeviceMapper deviceMapper;
    private final RiskPointMapper riskPointMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final RiskMetricCatalogMapper riskMetricCatalogMapper;
    private final ProductModelMapper productModelMapper;
    private final ProductMapper productMapper;
    private final ProductContractReleaseBatchMapper productContractReleaseBatchMapper;

    public RiskGovernanceServiceImpl(DeviceMapper deviceMapper,
                                     RiskPointMapper riskPointMapper,
                                     RiskPointDeviceMapper riskPointDeviceMapper,
                                     RuleDefinitionMapper ruleDefinitionMapper,
                                     RiskMetricCatalogMapper riskMetricCatalogMapper,
                                     ProductModelMapper productModelMapper,
                                     ProductMapper productMapper,
                                     ProductContractReleaseBatchMapper productContractReleaseBatchMapper) {
        this.deviceMapper = deviceMapper;
        this.riskPointMapper = riskPointMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.productModelMapper = productModelMapper;
        this.productMapper = productMapper;
        this.productContractReleaseBatchMapper = productContractReleaseBatchMapper;
    }

    @Override
    public PageResult<RiskGovernanceGapItemVO> listMissingBindings(RiskGovernanceGapQuery query) {
        long pageNum = normalizePageNum(query);
        long pageSize = normalizePageSize(query);
        Set<Long> boundDeviceIds = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                        .eq(RiskPointDevice::getDeleted, 0))
                .stream()
                .map(RiskPointDevice::getDeviceId)
                .collect(Collectors.toSet());

        List<RiskGovernanceGapItemVO> items = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeleted, 0)
                        .isNotNull(Device::getLastReportTime)
                        .orderByDesc(Device::getLastReportTime))
                .stream()
                .filter(device -> !boundDeviceIds.contains(device.getId()))
                .filter(device -> matchesDeviceCode(device, query))
                .map(this::toMissingBindingItem)
                .toList();
        return toPage(items, pageNum, pageSize);
    }

    @Override
    public PageResult<RiskGovernanceGapItemVO> listMissingPolicies(RiskGovernanceGapQuery query) {
        long pageNum = normalizePageNum(query);
        long pageSize = normalizePageSize(query);
        List<RiskPointDevice> bindings = listMissingPolicyBindings(query);
        if (bindings.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Set<Long> riskPointIds = bindings.stream().map(RiskPointDevice::getRiskPointId).collect(Collectors.toSet());
        Map<Long, RiskPoint> riskPointMap = riskPointMapper.selectList(new LambdaQueryWrapper<RiskPoint>()
                        .eq(RiskPoint::getDeleted, 0)
                        .in(!riskPointIds.isEmpty(), RiskPoint::getId, riskPointIds))
                .stream()
                .collect(Collectors.toMap(RiskPoint::getId, Function.identity(), (left, right) -> left));
        List<RiskGovernanceGapItemVO> items = bindings.stream()
                .map(binding -> toMissingPolicyItem(binding, riskPointMap.get(binding.getRiskPointId())))
                .toList();
        return toPage(items, pageNum, pageSize);
    }

    @Override
    public List<MissingPolicyAlertSignal> listMissingPolicyAlertSignals() {
        List<RiskPointDevice> bindings = listMissingPolicyBindings(null);
        if (bindings.isEmpty()) {
            return List.of();
        }
        Map<String, MissingPolicyAlertAccumulator> accumulators = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            String dimensionKey = toMissingPolicyDimensionKey(binding);
            if (!StringUtils.hasText(dimensionKey)) {
                continue;
            }
            MissingPolicyAlertAccumulator accumulator = accumulators.computeIfAbsent(
                    dimensionKey,
                    key -> new MissingPolicyAlertAccumulator(
                            key,
                            toMissingPolicyDimensionLabel(binding),
                            binding.getRiskMetricId(),
                            normalizeMetricIdentifier(binding.getMetricIdentifier()),
                            normalizeMetricName(binding.getMetricName())
                    )
            );
            accumulator.add(binding);
        }
        return accumulators.values().stream()
                .map(MissingPolicyAlertAccumulator::toSignal)
                .sorted(Comparator.comparingLong(MissingPolicyAlertSignal::bindingCount)
                        .reversed()
                        .thenComparing(MissingPolicyAlertSignal::dimensionKey))
                .toList();
    }

    @Override
    public PageResult<RiskMetricCatalogItemVO> pageMetricCatalogs(Long productId, Long pageNum, Long pageSize) {
        Page<RiskMetricCatalog> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<RiskMetricCatalog> result = riskMetricCatalogMapper.selectPage(page, new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(productId != null, RiskMetricCatalog::getProductId, productId)
                .orderByDesc(RiskMetricCatalog::getUpdateTime)
                .orderByDesc(RiskMetricCatalog::getCreateTime)
                .orderByDesc(RiskMetricCatalog::getId));
        List<RiskMetricCatalogItemVO> records = result.getRecords().stream()
                .map(this::toMetricCatalogItem)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public RiskMetricCatalogItemVO getMetricCatalog(Long id) {
        RiskMetricCatalog catalog = riskMetricCatalogMapper.selectById(id);
        if (catalog == null || Integer.valueOf(1).equals(catalog.getDeleted())) {
            throw new BizException("风险指标目录不存在: " + id);
        }
        return toMetricCatalogItem(catalog);
    }

    @Override
    public RiskGovernanceCoverageOverviewVO getCoverageOverview(Long productId) {
        List<ProductModel> propertyModels = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, productId)
                .eq(ProductModel::getModelType, "property"));
        Set<String> contractIdentifiers = propertyModels.stream()
                .map(ProductModel::getIdentifier)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<RiskMetricCatalog> catalogs = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .eq(RiskMetricCatalog::getProductId, productId));
        Set<Long> catalogIds = catalogs.stream()
                .map(RiskMetricCatalog::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> catalogIdentifiers = catalogs.stream()
                .map(RiskMetricCatalog::getContractIdentifier)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeleted, 0)
                .eq(Device::getProductId, productId));
        Set<Long> deviceIds = devices.stream()
                .map(Device::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> boundMetricKeys = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                        .eq(RiskPointDevice::getDeleted, 0)
                        .in(!deviceIds.isEmpty(), RiskPointDevice::getDeviceId, deviceIds))
                .stream()
                .map(this::toBindingMetricKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> ruleMetricKeys = ruleDefinitionMapper.selectList(new LambdaQueryWrapper<RuleDefinition>()
                        .eq(RuleDefinition::getDeleted, 0)
                        .eq(RuleDefinition::getStatus, 0))
                .stream()
                .map(this::toRuleMetricKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        long contractPropertyCount = contractIdentifiers.size();
        long publishedRiskMetricCount = catalogIds.size();
        long boundRiskMetricCount = boundMetricKeys.stream()
                .filter(key -> matchesCatalogKey(key, catalogIds, catalogIdentifiers))
                .count();
        long ruleCoveredRiskMetricCount = ruleMetricKeys.stream()
                .filter(boundMetricKeys::contains)
                .count();

        RiskGovernanceCoverageOverviewVO overview = new RiskGovernanceCoverageOverviewVO();
        overview.setProductId(productId);
        overview.setContractPropertyCount(contractPropertyCount);
        overview.setPublishedRiskMetricCount(publishedRiskMetricCount);
        overview.setBoundRiskMetricCount(boundRiskMetricCount);
        overview.setRuleCoveredRiskMetricCount(ruleCoveredRiskMetricCount);
        overview.setContractMetricCoverageRate(calculateRate(publishedRiskMetricCount, contractPropertyCount));
        overview.setBindingCoverageRate(calculateRate(boundRiskMetricCount, publishedRiskMetricCount));
        overview.setRuleCoverageRate(calculateRate(ruleCoveredRiskMetricCount, boundRiskMetricCount));
        return overview;
    }

    @Override
    public RiskGovernanceDashboardOverviewVO getDashboardOverview() {
        Set<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getDeleted, 0))
                .stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long totalProductCount = productIds.size();

        Set<Long> releasedProductIds = productContractReleaseBatchMapper.selectList(new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getDeleted, 0))
                .stream()
                .map(ProductContractReleaseBatch::getProductId)
                .filter(Objects::nonNull)
                .filter(productIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<RiskMetricCatalog> enabledCatalogs = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1));
        Set<Long> catalogIds = enabledCatalogs.stream()
                .map(RiskMetricCatalog::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> catalogIdentifiers = enabledCatalogs.stream()
                .map(RiskMetricCatalog::getContractIdentifier)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> catalogProductIds = enabledCatalogs.stream()
                .map(RiskMetricCatalog::getProductId)
                .filter(Objects::nonNull)
                .filter(productIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> governedProductIds = new LinkedHashSet<>();
        governedProductIds.addAll(releasedProductIds);
        governedProductIds.addAll(catalogProductIds);

        Set<String> boundMetricKeys = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                        .eq(RiskPointDevice::getDeleted, 0))
                .stream()
                .map(this::toBindingMetricKey)
                .filter(StringUtils::hasText)
                .filter(key -> matchesCatalogKey(key, catalogIds, catalogIdentifiers))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> ruleMetricKeys = ruleDefinitionMapper.selectList(new LambdaQueryWrapper<RuleDefinition>()
                        .eq(RuleDefinition::getDeleted, 0)
                        .eq(RuleDefinition::getStatus, 0))
                .stream()
                .map(this::toRuleMetricKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        long publishedRiskMetricCount = catalogIds.size();
        long boundRiskMetricCount = boundMetricKeys.size();
        long ruleCoveredRiskMetricCount = ruleMetricKeys.stream()
                .filter(boundMetricKeys::contains)
                .count();

        long releasedProductCount = releasedProductIds.size();
        long governedProductCount = governedProductIds.size();
        long pendingProductGovernanceCount = Math.max(0L, totalProductCount - governedProductCount);
        long pendingContractReleaseCount = Math.max(0L, totalProductCount - releasedProductCount);
        long pendingRiskBindingCount = listMissingBindings(null).getTotal();
        long pendingPolicyCount = listMissingPolicies(null).getTotal();
        long pendingReplayCount = listMissingPolicyAlertSignals().size();

        RiskGovernanceDashboardOverviewVO overview = new RiskGovernanceDashboardOverviewVO();
        overview.setTotalProductCount(totalProductCount);
        overview.setGovernedProductCount(governedProductCount);
        overview.setPendingProductGovernanceCount(pendingProductGovernanceCount);
        overview.setReleasedProductCount(releasedProductCount);
        overview.setPendingContractReleaseCount(pendingContractReleaseCount);
        overview.setPublishedRiskMetricCount(publishedRiskMetricCount);
        overview.setBoundRiskMetricCount(boundRiskMetricCount);
        overview.setRuleCoveredRiskMetricCount(ruleCoveredRiskMetricCount);
        overview.setPendingRiskBindingCount(pendingRiskBindingCount);
        overview.setPendingPolicyCount(pendingPolicyCount);
        overview.setPendingReplayCount(pendingReplayCount);
        overview.setGovernanceCompletionRate(calculateRate(governedProductCount, totalProductCount));
        overview.setMetricBindingCoverageRate(calculateRate(boundRiskMetricCount, publishedRiskMetricCount));
        overview.setPolicyCoverageRate(calculateRate(ruleCoveredRiskMetricCount, boundRiskMetricCount));
        return overview;
    }

    private boolean matchesDeviceCode(Device device, RiskGovernanceGapQuery query) {
        if (query == null || !StringUtils.hasText(query.getDeviceCode())) {
            return true;
        }
        return StringUtils.hasText(device.getDeviceCode()) && device.getDeviceCode().contains(query.getDeviceCode().trim());
    }

    private boolean matchesDeviceCode(RiskPointDevice binding, RiskGovernanceGapQuery query) {
        if (query == null || !StringUtils.hasText(query.getDeviceCode())) {
            return true;
        }
        return StringUtils.hasText(binding.getDeviceCode()) && binding.getDeviceCode().contains(query.getDeviceCode().trim());
    }

    private boolean matchesPolicy(RiskPointDevice binding,
                                  Map<String, List<RuleDefinition>> enabledRulesByMetric,
                                  Map<Long, List<RuleDefinition>> enabledRulesByRiskMetricId) {
        if (binding == null) {
            return false;
        }
        if (binding.getRiskMetricId() != null && enabledRulesByRiskMetricId.containsKey(binding.getRiskMetricId())) {
            return true;
        }
        return StringUtils.hasText(binding.getMetricIdentifier())
                && enabledRulesByMetric.containsKey(binding.getMetricIdentifier().trim());
    }

    private RiskGovernanceGapItemVO toMissingBindingItem(Device device) {
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_BINDING");
        item.setIssueLabel("待纳入风险对象");
        item.setDeviceId(device.getId());
        item.setDeviceCode(device.getDeviceCode());
        item.setDeviceName(device.getDeviceName());
        item.setLastReportTime(device.getLastReportTime());
        return item;
    }

    private RiskGovernanceGapItemVO toMissingPolicyItem(RiskPointDevice binding, RiskPoint riskPoint) {
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_POLICY");
        item.setIssueLabel("待配置阈值策略");
        item.setDeviceId(binding.getDeviceId());
        item.setDeviceCode(binding.getDeviceCode());
        item.setDeviceName(binding.getDeviceName());
        item.setRiskPointId(binding.getRiskPointId());
        item.setRiskPointName(riskPoint == null ? null : riskPoint.getRiskPointName());
        item.setRiskMetricId(binding.getRiskMetricId());
        item.setMetricIdentifier(binding.getMetricIdentifier());
        item.setMetricName(binding.getMetricName());
        return item;
    }

    private RiskMetricCatalogItemVO toMetricCatalogItem(RiskMetricCatalog catalog) {
        RiskMetricCatalogItemVO item = new RiskMetricCatalogItemVO();
        item.setId(catalog.getId());
        item.setProductId(catalog.getProductId());
        item.setProductModelId(catalog.getProductModelId());
        item.setContractIdentifier(catalog.getContractIdentifier());
        item.setRiskMetricCode(catalog.getRiskMetricCode());
        item.setRiskMetricName(catalog.getRiskMetricName());
        item.setThresholdDirection(catalog.getThresholdDirection());
        item.setTrendEnabled(catalog.getTrendEnabled());
        item.setGisEnabled(catalog.getGisEnabled());
        item.setInsightEnabled(catalog.getInsightEnabled());
        item.setAnalyticsEnabled(catalog.getAnalyticsEnabled());
        item.setEnabled(catalog.getEnabled());
        item.setCreateTime(catalog.getCreateTime());
        item.setUpdateTime(catalog.getUpdateTime());
        return item;
    }

    private String toBindingMetricKey(RiskPointDevice binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getRiskMetricId() != null) {
            return "ID:" + binding.getRiskMetricId();
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            return "IDENT:" + binding.getMetricIdentifier().trim();
        }
        return null;
    }

    private String toRuleMetricKey(RuleDefinition rule) {
        if (rule == null) {
            return null;
        }
        if (rule.getRiskMetricId() != null) {
            return "ID:" + rule.getRiskMetricId();
        }
        if (StringUtils.hasText(rule.getMetricIdentifier())) {
            return "IDENT:" + rule.getMetricIdentifier().trim();
        }
        return null;
    }

    private boolean matchesCatalogKey(String key, Set<Long> catalogIds, Set<String> catalogIdentifiers) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        if (key.startsWith("ID:")) {
            String raw = key.substring(3);
            try {
                return catalogIds.contains(Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (key.startsWith("IDENT:")) {
            return catalogIdentifiers.contains(key.substring(6));
        }
        return false;
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0D;
        }
        return Math.min(100D, (numerator * 100D) / denominator);
    }

    private List<RiskPointDevice> listMissingPolicyBindings(RiskGovernanceGapQuery query) {
        Long riskPointId = query == null ? null : query.getRiskPointId();
        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .eq(riskPointId != null, RiskPointDevice::getRiskPointId, riskPointId));
        if (bindings.isEmpty()) {
            return List.of();
        }
        List<RuleDefinition> enabledRules = ruleDefinitionMapper.selectList(new LambdaQueryWrapper<RuleDefinition>()
                .eq(RuleDefinition::getDeleted, 0)
                .eq(RuleDefinition::getStatus, 0));
        Map<String, List<RuleDefinition>> enabledRulesByMetric = enabledRules.stream()
                .filter(rule -> StringUtils.hasText(rule.getMetricIdentifier()))
                .collect(Collectors.groupingBy(rule -> rule.getMetricIdentifier().trim()));
        Map<Long, List<RuleDefinition>> enabledRulesByRiskMetricId = enabledRules.stream()
                .filter(rule -> rule.getRiskMetricId() != null)
                .collect(Collectors.groupingBy(RuleDefinition::getRiskMetricId));
        return bindings.stream()
                .filter(binding -> !matchesPolicy(binding, enabledRulesByMetric, enabledRulesByRiskMetricId))
                .filter(binding -> matchesDeviceCode(binding, query))
                .toList();
    }

    private String toMissingPolicyDimensionKey(RiskPointDevice binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getRiskMetricId() != null) {
            return "risk_metric_id:" + binding.getRiskMetricId();
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            return "metric_identifier:" + binding.getMetricIdentifier().trim().toLowerCase();
        }
        return null;
    }

    private String toMissingPolicyDimensionLabel(RiskPointDevice binding) {
        if (binding == null) {
            return "unknown";
        }
        if (binding.getRiskMetricId() != null) {
            return "riskMetricId=" + binding.getRiskMetricId();
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            return "metricIdentifier=" + binding.getMetricIdentifier().trim();
        }
        return "unknown";
    }

    private String normalizeMetricIdentifier(String metricIdentifier) {
        return StringUtils.hasText(metricIdentifier) ? metricIdentifier.trim() : null;
    }

    private String normalizeMetricName(String metricName) {
        return StringUtils.hasText(metricName) ? metricName.trim() : null;
    }

    private long normalizePageNum(RiskGovernanceGapQuery query) {
        return query == null || query.getPageNum() == null || query.getPageNum() < 1 ? 1L : query.getPageNum();
    }

    private long normalizePageSize(RiskGovernanceGapQuery query) {
        return query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10L : Math.min(query.getPageSize(), 100L);
    }

    private PageResult<RiskGovernanceGapItemVO> toPage(List<RiskGovernanceGapItemVO> items, long pageNum, long pageSize) {
        if (items == null || items.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, items.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, items.size());
        return PageResult.of((long) items.size(), pageNum, pageSize, items.subList(fromIndex, toIndex));
    }

    private static final class MissingPolicyAlertAccumulator {
        private final String dimensionKey;
        private final String dimensionLabel;
        private final Long riskMetricId;
        private final String metricIdentifier;
        private final String metricName;
        private long bindingCount;
        private final Set<Long> riskPointIds = new LinkedHashSet<>();

        private MissingPolicyAlertAccumulator(String dimensionKey,
                                             String dimensionLabel,
                                             Long riskMetricId,
                                             String metricIdentifier,
                                             String metricName) {
            this.dimensionKey = dimensionKey;
            this.dimensionLabel = dimensionLabel;
            this.riskMetricId = riskMetricId;
            this.metricIdentifier = metricIdentifier;
            this.metricName = metricName;
        }

        private void add(RiskPointDevice binding) {
            bindingCount++;
            if (binding != null && binding.getRiskPointId() != null) {
                riskPointIds.add(binding.getRiskPointId());
            }
        }

        private MissingPolicyAlertSignal toSignal() {
            return new MissingPolicyAlertSignal(
                    dimensionKey,
                    dimensionLabel,
                    riskMetricId,
                    metricIdentifier,
                    metricName,
                    bindingCount,
                    riskPointIds.size()
            );
        }
    }
}
