package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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
    private final LinkageRuleMapper linkageRuleMapper;
    private final EmergencyPlanMapper emergencyPlanMapper;
    private final RiskMetricLinkageBindingMapper linkageBindingMapper;
    private final RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;
    private final RiskMetricActionBindingBackfillService backfillService;

    public RiskGovernanceServiceImpl(DeviceMapper deviceMapper,
                                     RiskPointMapper riskPointMapper,
                                     RiskPointDeviceMapper riskPointDeviceMapper,
                                     RuleDefinitionMapper ruleDefinitionMapper,
                                     RiskMetricCatalogMapper riskMetricCatalogMapper,
                                     ProductModelMapper productModelMapper,
                                     ProductMapper productMapper,
                                     ProductContractReleaseBatchMapper productContractReleaseBatchMapper,
                                     LinkageRuleMapper linkageRuleMapper,
                                     EmergencyPlanMapper emergencyPlanMapper,
                                     RiskMetricLinkageBindingMapper linkageBindingMapper,
                                     RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper,
                                     RiskMetricActionBindingBackfillService backfillService) {
        this.deviceMapper = deviceMapper;
        this.riskPointMapper = riskPointMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.productModelMapper = productModelMapper;
        this.productMapper = productMapper;
        this.productContractReleaseBatchMapper = productContractReleaseBatchMapper;
        this.linkageRuleMapper = linkageRuleMapper;
        this.emergencyPlanMapper = emergencyPlanMapper;
        this.linkageBindingMapper = linkageBindingMapper;
        this.emergencyPlanBindingMapper = emergencyPlanBindingMapper;
        this.backfillService = backfillService;
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
        backfillService.ensureBindingsReadyForRead();
        List<ProductModel> propertyModels = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, productId)
                .eq(ProductModel::getModelType, "property"));
        Set<String> contractIdentifiers = propertyModels.stream()
                .map(ProductModel::getIdentifier)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<RiskMetricCatalog> catalogs = selectEnabledCatalogs(productId);
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

        List<RiskPointDevice> boundBindings = deviceIds.isEmpty()
                ? List.of()
                : riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .in(RiskPointDevice::getDeviceId, deviceIds));
        Set<String> boundMetricKeys = boundBindings.stream()
                .map(this::toBindingMetricKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MetricBindingDimension> boundMetricDimensions = buildBoundMetricDimensions(boundBindings, catalogIds, catalogIdentifiers);

        Set<String> ruleMetricKeys = ruleDefinitionMapper.selectList(new LambdaQueryWrapper<RuleDefinition>()
                        .eq(RuleDefinition::getDeleted, 0)
                        .eq(RuleDefinition::getStatus, 0))
                .stream()
                .map(this::toRuleMetricKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<RiskMetricLinkageBinding> linkageBindings = selectActiveLinkageBindings();
        List<RiskMetricEmergencyPlanBinding> emergencyPlanBindings = selectActiveEmergencyPlanBindings();

        long contractPropertyCount = contractIdentifiers.size();
        long publishedRiskMetricCount = catalogIds.size();
        long boundRiskMetricCount = boundMetricKeys.stream()
                .filter(key -> matchesCatalogKey(key, catalogIds, catalogIdentifiers))
                .count();
        long boundMetricDimensionCount = boundMetricDimensions.size();
        long ruleCoveredRiskMetricCount = ruleMetricKeys.stream()
                .filter(boundMetricKeys::contains)
                .count();
        long linkageCoveredRiskMetricCount = countCoveredDimensionsFromLinkageBindings(boundMetricDimensions, linkageBindings);
        long emergencyPlanCoveredRiskMetricCount = countCoveredDimensionsFromPlanBindings(boundMetricDimensions, emergencyPlanBindings);
        long linkagePlanCoveredRiskMetricCount = countCoveredDimensionsWithBoth(linkageBindings, emergencyPlanBindings, boundMetricDimensions);

        RiskGovernanceCoverageOverviewVO overview = new RiskGovernanceCoverageOverviewVO();
        overview.setProductId(productId);
        overview.setContractPropertyCount(contractPropertyCount);
        overview.setPublishedRiskMetricCount(publishedRiskMetricCount);
        overview.setBoundRiskMetricCount(boundRiskMetricCount);
        overview.setRuleCoveredRiskMetricCount(ruleCoveredRiskMetricCount);
        overview.setLinkageCoveredRiskMetricCount(linkageCoveredRiskMetricCount);
        overview.setEmergencyPlanCoveredRiskMetricCount(emergencyPlanCoveredRiskMetricCount);
        overview.setLinkagePlanCoveredRiskMetricCount(linkagePlanCoveredRiskMetricCount);
        overview.setContractMetricCoverageRate(calculateRate(publishedRiskMetricCount, contractPropertyCount));
        overview.setBindingCoverageRate(calculateRate(boundRiskMetricCount, publishedRiskMetricCount));
        overview.setRuleCoverageRate(calculateRate(ruleCoveredRiskMetricCount, boundRiskMetricCount));
        overview.setLinkageCoverageRate(calculateRate(linkageCoveredRiskMetricCount, boundMetricDimensionCount));
        overview.setEmergencyPlanCoverageRate(calculateRate(emergencyPlanCoveredRiskMetricCount, boundMetricDimensionCount));
        overview.setLinkagePlanCoverageRate(calculateRate(linkagePlanCoveredRiskMetricCount, boundMetricDimensionCount));
        return overview;
    }

    @Override
    public RiskGovernanceDashboardOverviewVO getDashboardOverview() {
        backfillService.ensureBindingsReadyForRead();
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getDeleted, 0));
        Set<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long totalProductCount = productIds.size();

        Map<Long, Product> productMap = products.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<ProductContractReleaseBatch> releaseBatches = productContractReleaseBatchMapper.selectList(new LambdaQueryWrapper<ProductContractReleaseBatch>()
                .eq(ProductContractReleaseBatch::getDeleted, 0));
        Set<Long> releasedProductIds = releaseBatches.stream()
                .map(ProductContractReleaseBatch::getProductId)
                .filter(Objects::nonNull)
                .filter(productIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, LocalDateTime> firstReleaseTimeByProduct = releaseBatches.stream()
                .filter(batch -> batch.getProductId() != null)
                .filter(batch -> batch.getCreateTime() != null)
                .filter(batch -> productIds.contains(batch.getProductId()))
                .collect(Collectors.toMap(
                        ProductContractReleaseBatch::getProductId,
                        ProductContractReleaseBatch::getCreateTime,
                        (left, right) -> left.isBefore(right) ? left : right,
                        LinkedHashMap::new
                ));

        List<RiskMetricCatalog> enabledCatalogs = selectEnabledCatalogs(null);
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

        List<RiskPointDevice> boundBindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0));
        Set<String> boundMetricKeys = boundBindings.stream()
                .map(this::toBindingMetricKey)
                .filter(StringUtils::hasText)
                .filter(key -> matchesCatalogKey(key, catalogIds, catalogIdentifiers))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MetricBindingDimension> boundMetricDimensions = buildBoundMetricDimensions(boundBindings, catalogIds, catalogIdentifiers);

        Set<String> ruleMetricKeys = ruleDefinitionMapper.selectList(new LambdaQueryWrapper<RuleDefinition>()
                        .eq(RuleDefinition::getDeleted, 0)
                        .eq(RuleDefinition::getStatus, 0))
                .stream()
                .map(this::toRuleMetricKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<RiskMetricLinkageBinding> linkageBindings = selectActiveLinkageBindings();
        List<RiskMetricEmergencyPlanBinding> emergencyPlanBindings = selectActiveEmergencyPlanBindings();

        long publishedRiskMetricCount = catalogIds.size();
        long boundRiskMetricCount = boundMetricKeys.size();
        long boundMetricDimensionCount = boundMetricDimensions.size();
        long ruleCoveredRiskMetricCount = ruleMetricKeys.stream()
                .filter(boundMetricKeys::contains)
                .count();
        long linkageCoveredMetricCount = countCoveredDimensionsFromLinkageBindings(boundMetricDimensions, linkageBindings);
        long emergencyPlanCoveredMetricCount = countCoveredDimensionsFromPlanBindings(boundMetricDimensions, emergencyPlanBindings);

        long releasedProductCount = releasedProductIds.size();
        long governedProductCount = governedProductIds.size();
        long pendingProductGovernanceCount = Math.max(0L, totalProductCount - governedProductCount);
        long pendingContractReleaseCount = Math.max(0L, totalProductCount - releasedProductCount);
        long pendingRiskBindingCount = listMissingBindings(null).getTotal();
        long pendingThresholdPolicyCount = listMissingPolicies(null).getTotal();
        long pendingLinkageCount = Math.max(0L, boundMetricDimensionCount - linkageCoveredMetricCount);
        long pendingEmergencyPlanCount = Math.max(0L, boundMetricDimensionCount - emergencyPlanCoveredMetricCount);
        long pendingLinkagePlanCount = pendingLinkageCount + pendingEmergencyPlanCount;
        long pendingReplayCount = listMissingPolicyAlertSignals().size();
        long totalBacklogCount = pendingProductGovernanceCount
                + pendingContractReleaseCount
                + pendingRiskBindingCount
                + pendingThresholdPolicyCount
                + pendingLinkagePlanCount
                + pendingReplayCount;
        double policyCoverageRate = calculateRate(ruleCoveredRiskMetricCount, boundRiskMetricCount);
        double linkageCoverageRate = calculateRate(linkageCoveredMetricCount, boundMetricDimensionCount);
        double emergencyPlanCoverageRate = calculateRate(emergencyPlanCoveredMetricCount, boundMetricDimensionCount);
        double linkagePlanCoverageRate = calculateRate(
                linkageCoveredMetricCount + emergencyPlanCoveredMetricCount,
                boundMetricDimensionCount * 2L
        );
        double averageOnboardingDurationHours = calculateAverageOnboardingDurationHours(
                productMap,
                firstReleaseTimeByProduct,
                releasedProductIds
        );

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
        overview.setPendingPolicyCount(pendingThresholdPolicyCount);
        overview.setPendingThresholdPolicyCount(pendingThresholdPolicyCount);
        overview.setPendingLinkageCount(pendingLinkageCount);
        overview.setPendingEmergencyPlanCount(pendingEmergencyPlanCount);
        overview.setPendingLinkagePlanCount(pendingLinkagePlanCount);
        overview.setPendingReplayCount(pendingReplayCount);
        overview.setGovernanceCompletionRate(calculateRate(governedProductCount, totalProductCount));
        overview.setMetricBindingCoverageRate(calculateRate(boundRiskMetricCount, publishedRiskMetricCount));
        overview.setPolicyCoverageRate(policyCoverageRate);
        overview.setThresholdPolicyCoverageRate(policyCoverageRate);
        overview.setLinkageCoverageRate(linkageCoverageRate);
        overview.setEmergencyPlanCoverageRate(emergencyPlanCoverageRate);
        overview.setLinkagePlanCoverageRate(linkagePlanCoverageRate);
        overview.setAverageOnboardingDurationHours(averageOnboardingDurationHours);
        overview.setBottleneckPendingProductGovernanceRate(calculateRate(pendingProductGovernanceCount, totalBacklogCount));
        overview.setBottleneckPendingContractReleaseRate(calculateRate(pendingContractReleaseCount, totalBacklogCount));
        overview.setBottleneckPendingRiskBindingRate(calculateRate(pendingRiskBindingCount, totalBacklogCount));
        overview.setBottleneckPendingThresholdPolicyRate(calculateRate(pendingThresholdPolicyCount, totalBacklogCount));
        overview.setBottleneckPendingLinkagePlanRate(calculateRate(pendingLinkagePlanCount, totalBacklogCount));
        overview.setBottleneckPendingReplayRate(calculateRate(pendingReplayCount, totalBacklogCount));
        return overview;
    }

    private List<RiskMetricCatalog> selectEnabledCatalogs(Long productId) {
        QueryWrapper<RiskMetricCatalog> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "product_id", "contract_identifier")
                .eq("deleted", 0)
                .eq("enabled", 1)
                .eq(productId != null, "product_id", productId);
        return riskMetricCatalogMapper.selectList(queryWrapper);
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
        item.setReleaseBatchId(catalog.getReleaseBatchId());
        item.setProductModelId(catalog.getProductModelId());
        item.setNormativeIdentifier(catalog.getNormativeIdentifier());
        item.setContractIdentifier(catalog.getContractIdentifier());
        item.setRiskMetricCode(catalog.getRiskMetricCode());
        item.setRiskMetricName(catalog.getRiskMetricName());
        item.setRiskCategory(catalog.getRiskCategory());
        item.setMetricRole(catalog.getMetricRole());
        item.setLifecycleStatus(catalog.getLifecycleStatus());
        item.setSourceScenarioCode(catalog.getSourceScenarioCode());
        item.setMetricUnit(catalog.getMetricUnit());
        item.setMetricDimension(catalog.getMetricDimension());
        item.setThresholdType(catalog.getThresholdType());
        item.setSemanticDirection(catalog.getSemanticDirection());
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

    private List<MetricBindingDimension> buildBoundMetricDimensions(List<RiskPointDevice> bindings,
                                                                    Set<Long> catalogIds,
                                                                    Set<String> catalogIdentifiers) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        Map<String, MetricBindingDimension> dimensions = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            String key = toBindingMetricKey(binding);
            if (!matchesCatalogKey(key, catalogIds, catalogIdentifiers)) {
                continue;
            }
            MetricBindingDimension dimension = toMetricBindingDimension(binding);
            if (dimension == null) {
                continue;
            }
            dimensions.putIfAbsent(dimension.dimensionKey(), dimension);
        }
        return new ArrayList<>(dimensions.values());
    }

    private MetricBindingDimension toMetricBindingDimension(RiskPointDevice binding) {
        if (binding == null) {
            return null;
        }
        String metricIdentifier = normalizeLower(binding.getMetricIdentifier());
        String metricName = normalizeLower(binding.getMetricName());
        String dimensionKey = binding.getRiskMetricId() != null
                ? "ID:" + binding.getRiskMetricId()
                : (StringUtils.hasText(metricIdentifier) ? "IDENT:" + metricIdentifier : null);
        if (!StringUtils.hasText(dimensionKey)) {
            return null;
        }
        return new MetricBindingDimension(dimensionKey, metricIdentifier, metricName);
    }

    private List<RiskMetricLinkageBinding> selectActiveLinkageBindings() {
        return linkageBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricLinkageBinding>()
                .eq(RiskMetricLinkageBinding::getDeleted, 0)
                .eq(RiskMetricLinkageBinding::getBindingStatus, "ACTIVE"));
    }

    private List<RiskMetricEmergencyPlanBinding> selectActiveEmergencyPlanBindings() {
        return emergencyPlanBindingMapper.selectList(new LambdaQueryWrapper<RiskMetricEmergencyPlanBinding>()
                .eq(RiskMetricEmergencyPlanBinding::getDeleted, 0)
                .eq(RiskMetricEmergencyPlanBinding::getBindingStatus, "ACTIVE"));
    }

    private long countCoveredDimensionsFromLinkageBindings(List<MetricBindingDimension> dimensions,
                                                           List<RiskMetricLinkageBinding> linkageBindings) {
        return countCoveredDimensions(dimensions, toLinkageDimensionKeys(linkageBindings));
    }

    private long countCoveredDimensionsFromPlanBindings(List<MetricBindingDimension> dimensions,
                                                        List<RiskMetricEmergencyPlanBinding> emergencyPlanBindings) {
        return countCoveredDimensions(dimensions, toEmergencyPlanDimensionKeys(emergencyPlanBindings));
    }

    private long countCoveredDimensionsWithBoth(List<RiskMetricLinkageBinding> linkageBindings,
                                                List<RiskMetricEmergencyPlanBinding> emergencyPlanBindings,
                                                List<MetricBindingDimension> dimensions) {
        Set<String> linkageKeys = toLinkageDimensionKeys(linkageBindings);
        if (linkageKeys.isEmpty()) {
            return 0L;
        }
        Set<String> planKeys = toEmergencyPlanDimensionKeys(emergencyPlanBindings);
        if (planKeys.isEmpty()) {
            return 0L;
        }
        return dimensions.stream()
                .map(MetricBindingDimension::dimensionKey)
                .filter(linkageKeys::contains)
                .filter(planKeys::contains)
                .count();
    }

    private long countCoveredDimensions(List<MetricBindingDimension> dimensions, Set<String> coveredDimensionKeys) {
        if (dimensions == null || dimensions.isEmpty() || coveredDimensionKeys == null || coveredDimensionKeys.isEmpty()) {
            return 0L;
        }
        return dimensions.stream()
                .map(MetricBindingDimension::dimensionKey)
                .filter(coveredDimensionKeys::contains)
                .count();
    }

    private Set<String> toLinkageDimensionKeys(List<RiskMetricLinkageBinding> linkageBindings) {
        if (linkageBindings == null || linkageBindings.isEmpty()) {
            return Set.of();
        }
        return linkageBindings.stream()
                .map(RiskMetricLinkageBinding::getRiskMetricId)
                .filter(Objects::nonNull)
                .map(riskMetricId -> "ID:" + riskMetricId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> toEmergencyPlanDimensionKeys(List<RiskMetricEmergencyPlanBinding> emergencyPlanBindings) {
        if (emergencyPlanBindings == null || emergencyPlanBindings.isEmpty()) {
            return Set.of();
        }
        return emergencyPlanBindings.stream()
                .map(RiskMetricEmergencyPlanBinding::getRiskMetricId)
                .filter(Objects::nonNull)
                .map(riskMetricId -> "ID:" + riskMetricId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private double calculateAverageOnboardingDurationHours(Map<Long, Product> productMap,
                                                           Map<Long, LocalDateTime> firstReleaseTimeByProduct,
                                                           Set<Long> releasedProductIds) {
        if (releasedProductIds == null || releasedProductIds.isEmpty()) {
            return 0D;
        }
        double totalHours = 0D;
        long validSampleCount = 0L;
        for (Long productId : releasedProductIds) {
            if (productId == null) {
                continue;
            }
            Product product = productMap.get(productId);
            LocalDateTime productCreateTime = product == null ? null : product.getCreateTime();
            LocalDateTime releaseTime = firstReleaseTimeByProduct.get(productId);
            if (productCreateTime == null || releaseTime == null || releaseTime.isBefore(productCreateTime)) {
                continue;
            }
            totalHours += Duration.between(productCreateTime, releaseTime).toMinutes() / 60D;
            validSampleCount++;
        }
        if (validSampleCount <= 0L) {
            return 0D;
        }
        return totalHours / validSampleCount;
    }

    private String normalizeLower(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
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

    private record MetricBindingDimension(String dimensionKey, String metricIdentifier, String metricName) {
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
