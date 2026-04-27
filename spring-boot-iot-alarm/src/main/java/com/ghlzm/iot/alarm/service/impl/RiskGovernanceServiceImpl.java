package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDeviceCapabilityBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceCapabilityBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceMissingPolicyProductMetricSummaryVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReleaseBatchDiffVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductContractReleaseSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
    private final VendorMetricEvidenceMapper vendorMetricEvidenceMapper;
    private final RiskMetricActionBindingBackfillService backfillService;
    private final RiskMetricCatalogPublishRule riskMetricCatalogPublishRule = new DefaultRiskMetricCatalogPublishRule();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    private ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper;
    private RiskPointDeviceCapabilityBindingMapper capabilityBindingMapper;

    RiskGovernanceServiceImpl(DeviceMapper deviceMapper,
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
        this(deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                null,
                backfillService);
    }

    @Autowired
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
                                     VendorMetricEvidenceMapper vendorMetricEvidenceMapper,
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
        this.vendorMetricEvidenceMapper = vendorMetricEvidenceMapper;
        this.backfillService = backfillService;
    }

    @Autowired(required = false)
    void setProductContractReleaseSnapshotMapper(ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper) {
        this.productContractReleaseSnapshotMapper = productContractReleaseSnapshotMapper;
    }

    @Autowired(required = false)
    void setRiskPointDeviceCapabilityBindingMapper(RiskPointDeviceCapabilityBindingMapper capabilityBindingMapper) {
        this.capabilityBindingMapper = capabilityBindingMapper;
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
        if (capabilityBindingMapper != null) {
            capabilityBindingMapper.selectList(new LambdaQueryWrapper<RiskPointDeviceCapabilityBinding>()
                            .eq(RiskPointDeviceCapabilityBinding::getDeleted, 0))
                    .stream()
                    .map(RiskPointDeviceCapabilityBinding::getDeviceId)
                    .filter(Objects::nonNull)
                    .forEach(boundDeviceIds::add);
        }

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
        Map<Long, Long> deviceProductIds = loadDeviceProductIds(bindings);
        Map<Long, Product> productMap = loadProductsById(new LinkedHashSet<>(deviceProductIds.values()));
        List<RiskGovernanceGapItemVO> items = bindings.stream()
                .map(binding -> toMissingPolicyItem(binding, riskPointMap.get(binding.getRiskPointId()),
                        deviceProductIds, productMap))
                .toList();
        return toPage(items, pageNum, pageSize);
    }

    @Override
    public PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> pageMissingPolicyProductMetricSummaries(
            RiskGovernanceGapQuery query) {
        long pageNum = normalizePageNum(query);
        long pageSize = normalizePageSize(query);
        List<RiskPointDevice> bindings = listMissingPolicyBindings(query);
        if (bindings.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        Map<Long, Long> deviceProductIds = loadDeviceProductIds(bindings);
        Map<Long, Product> productMap = loadProductsById(new LinkedHashSet<>(deviceProductIds.values()));
        Map<String, MissingPolicyProductMetricAccumulator> accumulators = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            String metricKey = toMissingPolicyDimensionKey(binding);
            if (!StringUtils.hasText(metricKey)) {
                continue;
            }
            Long productId = binding.getDeviceId() == null ? null : deviceProductIds.get(binding.getDeviceId());
            String groupKey = (productId == null ? "unknown" : productId.toString()) + "#" + metricKey;
            Product product = productId == null ? null : productMap.get(productId);
            MissingPolicyProductMetricAccumulator accumulator = accumulators.computeIfAbsent(
                    groupKey,
                    key -> new MissingPolicyProductMetricAccumulator(productId, product, binding)
            );
            accumulator.add(binding);
        }
        List<RiskGovernanceMissingPolicyProductMetricSummaryVO> summaries = accumulators.values().stream()
                .map(MissingPolicyProductMetricAccumulator::toVO)
                .sorted(Comparator.comparingLong(RiskGovernanceMissingPolicyProductMetricSummaryVO::getBindingCount)
                        .reversed()
                        .thenComparing(item -> normalizeText(item.getProductName()), Comparator.nullsLast(String::compareTo))
                        .thenComparing(item -> normalizeText(item.getMetricIdentifier()), Comparator.nullsLast(String::compareTo)))
                .toList();
        return toPage(summaries, pageNum, pageSize);
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
    public PageResult<RiskMetricCatalogItemVO> pageMetricCatalogs(Long productId,
                                                                  Long releaseBatchId,
                                                                  Long pageNum,
                                                                  Long pageSize) {
        Page<RiskMetricCatalog> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<RiskMetricCatalog> result = riskMetricCatalogMapper.selectPage(page, new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(productId != null, RiskMetricCatalog::getProductId, productId)
                .eq(releaseBatchId != null, RiskMetricCatalog::getReleaseBatchId, releaseBatchId)
                .orderByDesc(RiskMetricCatalog::getUpdateTime)
                .orderByDesc(RiskMetricCatalog::getCreateTime)
                .orderByDesc(RiskMetricCatalog::getId));
        List<RiskMetricCatalogItemVO> records = result.getRecords().stream()
                .map(this::toMetricCatalogItem)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public RiskGovernanceReleaseBatchDiffVO compareReleaseBatches(Long baselineBatchId, Long targetBatchId) {
        if (baselineBatchId == null || targetBatchId == null) {
            throw new BizException("请指定基线批次和目标批次");
        }
        if (Objects.equals(baselineBatchId, targetBatchId)) {
            throw new BizException("基线批次与目标批次不能相同");
        }
        if (productContractReleaseSnapshotMapper == null) {
            throw new BizException("批次快照读侧未启用");
        }

        ProductContractReleaseBatch baselineBatch = requireReleaseBatch(baselineBatchId);
        ProductContractReleaseBatch targetBatch = requireReleaseBatch(targetBatchId);
        if (!Objects.equals(baselineBatch.getProductId(), targetBatch.getProductId())) {
            throw new BizException("仅支持对比同一产品的发布批次");
        }

        Map<String, ReleaseModelSnapshotItem> baselineContracts = toSnapshotMap(parseSnapshotItems(
                loadBatchSnapshot(baselineBatchId, "AFTER_APPLY").getSnapshotJson()
        ));
        Map<String, ReleaseModelSnapshotItem> targetContracts = toSnapshotMap(parseSnapshotItems(
                loadBatchSnapshot(targetBatchId, "AFTER_APPLY").getSnapshotJson()
        ));
        Map<String, RiskMetricCatalog> baselineMetrics = toMetricCatalogMap(selectReleaseBatchCatalogs(baselineBatchId));
        Map<String, RiskMetricCatalog> targetMetrics = toMetricCatalogMap(selectReleaseBatchCatalogs(targetBatchId));

        int addedContractCount = 0;
        int removedContractCount = 0;
        int changedContractCount = 0;
        int unchangedContractCount = 0;
        List<RiskGovernanceReleaseBatchDiffVO.ContractDiffItem> contractDiffItems = new ArrayList<>();
        Set<String> contractKeys = new LinkedHashSet<>();
        contractKeys.addAll(baselineContracts.keySet());
        contractKeys.addAll(targetContracts.keySet());
        for (String contractKey : contractKeys) {
            ReleaseModelSnapshotItem baselineItem = baselineContracts.get(contractKey);
            ReleaseModelSnapshotItem targetItem = targetContracts.get(contractKey);
            if (baselineItem == null && targetItem != null) {
                addedContractCount++;
                contractDiffItems.add(toContractDiffItem("ADDED", targetItem.modelType(), targetItem.identifier(), List.of()));
                continue;
            }
            if (baselineItem != null && targetItem == null) {
                removedContractCount++;
                contractDiffItems.add(toContractDiffItem("REMOVED", baselineItem.modelType(), baselineItem.identifier(), List.of()));
                continue;
            }
            List<String> changedFields = resolveChangedFields(baselineItem, targetItem);
            if (changedFields.isEmpty()) {
                unchangedContractCount++;
            } else {
                changedContractCount++;
                contractDiffItems.add(toContractDiffItem("UPDATED", baselineItem.modelType(), baselineItem.identifier(), changedFields));
            }
        }

        int addedMetricCount = 0;
        int removedMetricCount = 0;
        int changedMetricCount = 0;
        int unchangedMetricCount = 0;
        List<RiskGovernanceReleaseBatchDiffVO.MetricDiffItem> metricDiffItems = new ArrayList<>();
        Set<String> metricKeys = new LinkedHashSet<>();
        metricKeys.addAll(baselineMetrics.keySet());
        metricKeys.addAll(targetMetrics.keySet());
        for (String metricKey : metricKeys) {
            RiskMetricCatalog baselineMetric = baselineMetrics.get(metricKey);
            RiskMetricCatalog targetMetric = targetMetrics.get(metricKey);
            if (baselineMetric == null && targetMetric != null) {
                addedMetricCount++;
                metricDiffItems.add(toMetricDiffItem("ADDED", targetMetric, List.of()));
                continue;
            }
            if (baselineMetric != null && targetMetric == null) {
                removedMetricCount++;
                metricDiffItems.add(toMetricDiffItem("REMOVED", baselineMetric, List.of()));
                continue;
            }
            List<String> changedFields = resolveMetricChangedFields(baselineMetric, targetMetric);
            if (changedFields.isEmpty()) {
                unchangedMetricCount++;
            } else {
                changedMetricCount++;
                metricDiffItems.add(toMetricDiffItem("UPDATED", targetMetric, changedFields));
            }
        }

        RiskGovernanceReleaseBatchDiffVO diff = new RiskGovernanceReleaseBatchDiffVO();
        diff.setProductId(baselineBatch.getProductId());
        diff.setBaselineBatch(toBatchSummary(baselineBatch));
        diff.setTargetBatch(toBatchSummary(targetBatch));
        diff.setBaselineContractFieldCount(baselineContracts.size());
        diff.setTargetContractFieldCount(targetContracts.size());
        diff.setBaselineMetricCount(baselineMetrics.size());
        diff.setTargetMetricCount(targetMetrics.size());
        diff.setAddedContractCount(addedContractCount);
        diff.setRemovedContractCount(removedContractCount);
        diff.setChangedContractCount(changedContractCount);
        diff.setUnchangedContractCount(unchangedContractCount);
        diff.setAddedMetricCount(addedMetricCount);
        diff.setRemovedMetricCount(removedMetricCount);
        diff.setChangedMetricCount(changedMetricCount);
        diff.setUnchangedMetricCount(unchangedMetricCount);
        diff.setComparedAt(LocalDateTime.now());
        diff.setContractDiffItems(contractDiffItems);
        diff.setMetricDiffItems(metricDiffItems);
        return diff;
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
        Product product = productId == null ? null : productMapper.selectById(productId);
        long publishableContractPropertyCount = riskMetricCatalogPublishRule
                .resolveRiskEnabledIdentifiers(product, null, null, propertyModels)
                .size();

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
        overview.setPublishableContractPropertyCount(publishableContractPropertyCount);
        overview.setPublishedRiskMetricCount(publishedRiskMetricCount);
        overview.setBoundRiskMetricCount(boundRiskMetricCount);
        overview.setRuleCoveredRiskMetricCount(ruleCoveredRiskMetricCount);
        overview.setLinkageCoveredRiskMetricCount(linkageCoveredRiskMetricCount);
        overview.setEmergencyPlanCoveredRiskMetricCount(emergencyPlanCoveredRiskMetricCount);
        overview.setLinkagePlanCoveredRiskMetricCount(linkagePlanCoveredRiskMetricCount);
        overview.setContractMetricCoverageRate(
                publishableContractPropertyCount <= 0L
                        ? 100D
                        : calculateRate(publishedRiskMetricCount, publishableContractPropertyCount)
        );
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
        List<RawStageProductAccumulator> rawStageProducts = listRawStageProducts(productMap, governedProductIds);
        long rawStageProductCount = rawStageProducts.size();
        List<String> rawStageProductNames = rawStageProducts.stream()
                .map(RawStageProductAccumulator::productDisplayName)
                .limit(3)
                .toList();
        List<String> rawStageVendorNames = rawStageProducts.stream()
                .map(RawStageProductAccumulator::vendorDisplayName)
                .distinct()
                .limit(3)
                .toList();
        long rawStageVendorCount = rawStageProducts.stream()
                .map(RawStageProductAccumulator::vendorDisplayName)
                .distinct()
                .count();

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
        overview.setRawStageProductCount(rawStageProductCount);
        overview.setRawStageVendorCount(rawStageVendorCount);
        overview.setRawStageProductNames(rawStageProductNames);
        overview.setRawStageVendorNames(rawStageVendorNames);
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
                                  Map<Long, List<RuleDefinition>> enabledRulesByRiskMetricId,
                                  Map<Long, Long> deviceProductIds) {
        if (binding == null) {
            return false;
        }
        List<RuleDefinition> candidates = new ArrayList<>();
        if (binding.getRiskMetricId() != null) {
            candidates.addAll(enabledRulesByRiskMetricId.getOrDefault(binding.getRiskMetricId(), List.of()));
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            candidates.addAll(enabledRulesByMetric.getOrDefault(binding.getMetricIdentifier().trim(), List.of()));
        }
        Long productId = binding.getDeviceId() == null ? null : deviceProductIds.get(binding.getDeviceId());
        return candidates.stream().anyMatch(rule -> matchesPolicyScope(rule, binding, productId));
    }

    private boolean matchesPolicyScope(RuleDefinition rule, RiskPointDevice binding, Long productId) {
        String scope = normalizeRuleScope(rule == null ? null : rule.getRuleScope());
        return switch (scope) {
            case "BINDING" -> binding != null
                    && binding.getId() != null
                    && binding.getId().equals(rule.getRiskPointDeviceId());
            case "DEVICE" -> binding != null
                    && binding.getDeviceId() != null
                    && binding.getDeviceId().equals(rule.getDeviceId());
            case "PRODUCT" -> productId != null && productId.equals(rule.getProductId());
            default -> true;
        };
    }

    private String normalizeRuleScope(String scope) {
        return StringUtils.hasText(scope) ? scope.trim().toUpperCase(Locale.ROOT) : "METRIC";
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

    private RiskGovernanceGapItemVO toMissingPolicyItem(RiskPointDevice binding,
                                                        RiskPoint riskPoint,
                                                        Map<Long, Long> deviceProductIds,
                                                        Map<Long, Product> productMap) {
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_POLICY");
        item.setIssueLabel("待配置阈值策略");
        item.setDeviceId(binding.getDeviceId());
        item.setDeviceCode(binding.getDeviceCode());
        item.setDeviceName(binding.getDeviceName());
        Long productId = binding.getDeviceId() == null || deviceProductIds == null
                ? null
                : deviceProductIds.get(binding.getDeviceId());
        Product product = productId == null || productMap == null ? null : productMap.get(productId);
        item.setProductId(productId);
        item.setProductKey(product == null ? null : product.getProductKey());
        item.setProductName(product == null ? null : product.getProductName());
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

    private ProductContractReleaseBatch requireReleaseBatch(Long batchId) {
        ProductContractReleaseBatch batch = productContractReleaseBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("契约发布批次不存在: " + batchId);
        }
        return batch;
    }

    private ProductContractReleaseSnapshot loadBatchSnapshot(Long batchId, String snapshotStage) {
        List<ProductContractReleaseSnapshot> snapshots = productContractReleaseSnapshotMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseSnapshot>()
                        .eq(ProductContractReleaseSnapshot::getDeleted, 0)
                        .eq(ProductContractReleaseSnapshot::getBatchId, batchId)
                        .eq(ProductContractReleaseSnapshot::getSnapshotStage, snapshotStage)
                        .orderByDesc(ProductContractReleaseSnapshot::getCreateTime)
                        .orderByDesc(ProductContractReleaseSnapshot::getId)
                        .last("limit 1")
        );
        if (snapshots == null || snapshots.isEmpty()) {
            throw new BizException("发布批次缺少快照: " + snapshotStage);
        }
        ProductContractReleaseSnapshot snapshot = snapshots.get(0);
        if (!StringUtils.hasText(snapshot.getSnapshotJson())) {
            throw new BizException("发布批次快照为空: " + snapshotStage);
        }
        return snapshot;
    }

    private List<ReleaseModelSnapshotItem> parseSnapshotItems(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return List.of();
        }
        try {
            List<ReleaseModelSnapshotItem> parsed = objectMapper.readValue(
                    snapshotJson,
                    new TypeReference<List<ReleaseModelSnapshotItem>>() {
                    }
            );
            if (parsed == null || parsed.isEmpty()) {
                return List.of();
            }
            List<ReleaseModelSnapshotItem> result = new ArrayList<>();
            for (ReleaseModelSnapshotItem item : parsed) {
                if (item == null || !StringUtils.hasText(item.modelType()) || !StringUtils.hasText(item.identifier())) {
                    continue;
                }
                result.add(item);
            }
            result.sort(Comparator
                    .comparing(ReleaseModelSnapshotItem::modelType)
                    .thenComparing(ReleaseModelSnapshotItem::identifier));
            return result;
        } catch (Exception ex) {
            throw new BizException("发布批次快照格式无效");
        }
    }

    private Map<String, ReleaseModelSnapshotItem> toSnapshotMap(List<ReleaseModelSnapshotItem> items) {
        Map<String, ReleaseModelSnapshotItem> map = new LinkedHashMap<>();
        if (items == null || items.isEmpty()) {
            return map;
        }
        for (ReleaseModelSnapshotItem item : items) {
            String key = buildModelKey(item.modelType(), item.identifier());
            if (StringUtils.hasText(key) && !map.containsKey(key)) {
                map.put(key, item);
            }
        }
        return map;
    }

    private String buildModelKey(String modelType, String identifier) {
        String normalizedModelType = normalizeText(modelType);
        String normalizedIdentifier = normalizeText(identifier);
        if (!StringUtils.hasText(normalizedModelType) || !StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return normalizedModelType + "#" + normalizedIdentifier;
    }

    private List<RiskMetricCatalog> selectReleaseBatchCatalogs(Long releaseBatchId) {
        if (releaseBatchId == null) {
            return List.of();
        }
        return riskMetricCatalogMapper.selectList(new QueryWrapper<RiskMetricCatalog>()
                .eq("deleted", 0)
                .eq("release_batch_id", releaseBatchId)
                .orderByAsc("contract_identifier")
                .orderByAsc("id"));
    }

    private Map<String, RiskMetricCatalog> toMetricCatalogMap(List<RiskMetricCatalog> catalogs) {
        Map<String, RiskMetricCatalog> map = new LinkedHashMap<>();
        if (catalogs == null || catalogs.isEmpty()) {
            return map;
        }
        for (RiskMetricCatalog catalog : catalogs) {
            String key = toMetricCatalogKey(catalog);
            if (StringUtils.hasText(key) && !map.containsKey(key)) {
                map.put(key, catalog);
            }
        }
        return map;
    }

    private String toMetricCatalogKey(RiskMetricCatalog catalog) {
        if (catalog == null) {
            return null;
        }
        String contractIdentifier = normalizeText(catalog.getContractIdentifier());
        if (StringUtils.hasText(contractIdentifier)) {
            return contractIdentifier;
        }
        String riskMetricCode = normalizeText(catalog.getRiskMetricCode());
        if (StringUtils.hasText(riskMetricCode)) {
            return riskMetricCode;
        }
        return null;
    }

    private RiskGovernanceReleaseBatchDiffVO.BatchSummary toBatchSummary(ProductContractReleaseBatch batch) {
        RiskGovernanceReleaseBatchDiffVO.BatchSummary summary = new RiskGovernanceReleaseBatchDiffVO.BatchSummary();
        summary.setId(batch.getId());
        summary.setProductId(batch.getProductId());
        summary.setScenarioCode(batch.getScenarioCode());
        summary.setReleaseSource(batch.getReleaseSource());
        summary.setReleasedFieldCount(batch.getReleasedFieldCount());
        summary.setApprovalOrderId(batch.getApprovalOrderId());
        summary.setReleaseReason(batch.getReleaseReason());
        summary.setReleaseStatus(batch.getReleaseStatus());
        summary.setCreateTime(batch.getCreateTime());
        summary.setRollbackTime(batch.getRollbackTime());
        return summary;
    }

    private RiskGovernanceReleaseBatchDiffVO.ContractDiffItem toContractDiffItem(String changeType,
                                                                                 String modelType,
                                                                                 String identifier,
                                                                                 List<String> changedFields) {
        RiskGovernanceReleaseBatchDiffVO.ContractDiffItem item = new RiskGovernanceReleaseBatchDiffVO.ContractDiffItem();
        item.setChangeType(changeType);
        item.setModelType(normalizeText(modelType));
        item.setIdentifier(normalizeText(identifier));
        item.setChangedFields(changedFields == null ? List.of() : changedFields);
        return item;
    }

    private RiskGovernanceReleaseBatchDiffVO.MetricDiffItem toMetricDiffItem(String changeType,
                                                                             RiskMetricCatalog catalog,
                                                                             List<String> changedFields) {
        RiskGovernanceReleaseBatchDiffVO.MetricDiffItem item = new RiskGovernanceReleaseBatchDiffVO.MetricDiffItem();
        item.setChangeType(changeType);
        item.setContractIdentifier(catalog == null ? null : normalizeText(catalog.getContractIdentifier()));
        item.setRiskMetricCode(catalog == null ? null : normalizeText(catalog.getRiskMetricCode()));
        item.setRiskMetricName(catalog == null ? null : normalizeText(catalog.getRiskMetricName()));
        item.setMetricRole(catalog == null ? null : normalizeText(catalog.getMetricRole()));
        item.setLifecycleStatus(catalog == null ? null : normalizeText(catalog.getLifecycleStatus()));
        item.setChangedFields(changedFields == null ? List.of() : changedFields);
        return item;
    }

    private List<String> resolveChangedFields(ReleaseModelSnapshotItem baseline, ReleaseModelSnapshotItem target) {
        if (baseline == null || target == null) {
            return List.of();
        }
        List<String> changed = new ArrayList<>();
        addChangedField(changed, "modelName", normalizeText(baseline.modelName()), normalizeText(target.modelName()));
        addChangedField(changed, "dataType", normalizeText(baseline.dataType()), normalizeText(target.dataType()));
        addChangedField(changed, "specsJson", normalizeText(baseline.specsJson()), normalizeText(target.specsJson()));
        addChangedField(changed, "eventType", normalizeText(baseline.eventType()), normalizeText(target.eventType()));
        addChangedField(changed, "serviceInputJson", normalizeText(baseline.serviceInputJson()), normalizeText(target.serviceInputJson()));
        addChangedField(changed, "serviceOutputJson", normalizeText(baseline.serviceOutputJson()), normalizeText(target.serviceOutputJson()));
        addChangedField(changed, "sortNo", baseline.sortNo(), target.sortNo());
        addChangedField(changed, "requiredFlag", baseline.requiredFlag(), target.requiredFlag());
        addChangedField(changed, "description", normalizeText(baseline.description()), normalizeText(target.description()));
        return changed;
    }

    private List<String> resolveMetricChangedFields(RiskMetricCatalog baseline, RiskMetricCatalog target) {
        if (baseline == null || target == null) {
            return List.of();
        }
        List<String> changed = new ArrayList<>();
        addChangedField(changed, "normativeIdentifier", normalizeText(baseline.getNormativeIdentifier()), normalizeText(target.getNormativeIdentifier()));
        addChangedField(changed, "riskMetricCode", normalizeText(baseline.getRiskMetricCode()), normalizeText(target.getRiskMetricCode()));
        addChangedField(changed, "riskMetricName", normalizeText(baseline.getRiskMetricName()), normalizeText(target.getRiskMetricName()));
        addChangedField(changed, "riskCategory", normalizeText(baseline.getRiskCategory()), normalizeText(target.getRiskCategory()));
        addChangedField(changed, "metricRole", normalizeText(baseline.getMetricRole()), normalizeText(target.getMetricRole()));
        addChangedField(changed, "lifecycleStatus", normalizeText(baseline.getLifecycleStatus()), normalizeText(target.getLifecycleStatus()));
        addChangedField(changed, "sourceScenarioCode", normalizeText(baseline.getSourceScenarioCode()), normalizeText(target.getSourceScenarioCode()));
        addChangedField(changed, "metricUnit", normalizeText(baseline.getMetricUnit()), normalizeText(target.getMetricUnit()));
        addChangedField(changed, "metricDimension", normalizeText(baseline.getMetricDimension()), normalizeText(target.getMetricDimension()));
        addChangedField(changed, "thresholdType", normalizeText(baseline.getThresholdType()), normalizeText(target.getThresholdType()));
        addChangedField(changed, "semanticDirection", normalizeText(baseline.getSemanticDirection()), normalizeText(target.getSemanticDirection()));
        addChangedField(changed, "thresholdDirection", normalizeText(baseline.getThresholdDirection()), normalizeText(target.getThresholdDirection()));
        addChangedField(changed, "trendEnabled", baseline.getTrendEnabled(), target.getTrendEnabled());
        addChangedField(changed, "gisEnabled", baseline.getGisEnabled(), target.getGisEnabled());
        addChangedField(changed, "insightEnabled", baseline.getInsightEnabled(), target.getInsightEnabled());
        addChangedField(changed, "analyticsEnabled", baseline.getAnalyticsEnabled(), target.getAnalyticsEnabled());
        addChangedField(changed, "enabled", baseline.getEnabled(), target.getEnabled());
        return changed;
    }

    private void addChangedField(List<String> changedFields, String fieldName, Object baseline, Object target) {
        if (!Objects.equals(baseline, target)) {
            changedFields.add(fieldName);
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private List<RawStageProductAccumulator> listRawStageProducts(Map<Long, Product> productMap,
                                                                  Set<Long> governedProductIds) {
        if (vendorMetricEvidenceMapper == null || productMap == null || productMap.isEmpty()) {
            return List.of();
        }
        QueryWrapper<VendorMetricEvidence> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("product_id", "raw_identifier", "evidence_count", "last_seen_time")
                .eq("deleted", 0)
                .isNotNull("product_id");
        List<VendorMetricEvidence> evidenceList = vendorMetricEvidenceMapper.selectList(queryWrapper);
        if (evidenceList == null || evidenceList.isEmpty()) {
            return List.of();
        }
        Map<Long, RawStageProductAccumulator> rawStageByProduct = new LinkedHashMap<>();
        for (VendorMetricEvidence evidence : evidenceList) {
            Long productId = evidence == null ? null : evidence.getProductId();
            if (productId == null || (governedProductIds != null && governedProductIds.contains(productId))) {
                continue;
            }
            Product product = productMap.get(productId);
            if (product == null) {
                continue;
            }
            rawStageByProduct.computeIfAbsent(productId, ignored -> new RawStageProductAccumulator(product))
                    .add(evidence);
        }
        return rawStageByProduct.values().stream()
                .sorted(Comparator.comparingInt(RawStageProductAccumulator::totalEvidenceCount).reversed()
                        .thenComparing(RawStageProductAccumulator::lastSeenTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RawStageProductAccumulator::productDisplayName))
                .toList();
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
        Long productId = query == null ? null : query.getProductId();
        Map<Long, Long> deviceProductIds = loadDeviceProductIds(bindings);
        return bindings.stream()
                .filter(binding -> !matchesPolicy(binding, enabledRulesByMetric, enabledRulesByRiskMetricId, deviceProductIds))
                .filter(binding -> matchesProduct(binding, productId, deviceProductIds))
                .filter(binding -> matchesDeviceCode(binding, query))
                .toList();
    }

    private Map<Long, Long> loadDeviceProductIds(List<RiskPointDevice> bindings) {
        Set<Long> deviceIds = bindings == null
                ? Set.of()
                : bindings.stream()
                .map(RiskPointDevice::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deviceIds.isEmpty()) {
            return Map.of();
        }
        return deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                        .in(Device::getId, deviceIds))
                .stream()
                .filter(device -> device.getId() != null)
                .filter(device -> device.getProductId() != null)
                .collect(Collectors.toMap(Device::getId, Device::getProductId, (left, right) -> left));
    }

    private Map<Long, Product> loadProductsById(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .in(Product::getId, productIds));
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        return products.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, Function.identity(), (left, right) -> left));
    }

    private boolean matchesProduct(RiskPointDevice binding, Long productId, Map<Long, Long> deviceProductIds) {
        if (productId == null) {
            return true;
        }
        if (binding == null || binding.getDeviceId() == null || deviceProductIds == null) {
            return false;
        }
        return productId.equals(deviceProductIds.get(binding.getDeviceId()));
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

    private static class MissingPolicyProductMetricAccumulator {
        private final Long productId;
        private final String productKey;
        private final String productName;
        private final Long riskMetricId;
        private final String metricIdentifier;
        private final String metricName;
        private long bindingCount;
        private final Set<Long> riskPointIds = new LinkedHashSet<>();
        private final Set<Long> deviceIds = new LinkedHashSet<>();

        MissingPolicyProductMetricAccumulator(Long productId, Product product, RiskPointDevice firstBinding) {
            this.productId = productId;
            this.productKey = product == null ? null : product.getProductKey();
            this.productName = product == null ? null : product.getProductName();
            this.riskMetricId = firstBinding == null ? null : firstBinding.getRiskMetricId();
            this.metricIdentifier = firstBinding == null ? null : firstBinding.getMetricIdentifier();
            this.metricName = firstBinding == null ? null : firstBinding.getMetricName();
        }

        void add(RiskPointDevice binding) {
            if (binding == null) {
                return;
            }
            bindingCount++;
            if (binding.getRiskPointId() != null) {
                riskPointIds.add(binding.getRiskPointId());
            }
            if (binding.getDeviceId() != null) {
                deviceIds.add(binding.getDeviceId());
            }
        }

        RiskGovernanceMissingPolicyProductMetricSummaryVO toVO() {
            RiskGovernanceMissingPolicyProductMetricSummaryVO item = new RiskGovernanceMissingPolicyProductMetricSummaryVO();
            item.setProductId(productId);
            item.setProductKey(productKey);
            item.setProductName(productName);
            item.setRiskMetricId(riskMetricId);
            item.setMetricIdentifier(metricIdentifier);
            item.setMetricName(metricName);
            item.setBindingCount(bindingCount);
            item.setRiskPointCount((long) riskPointIds.size());
            item.setDeviceCount((long) deviceIds.size());
            return item;
        }
    }

    record ReleaseModelSnapshotItem(
            String modelType,
            String identifier,
            String modelName,
            String dataType,
            String specsJson,
            String eventType,
            String serviceInputJson,
            String serviceOutputJson,
            Integer sortNo,
            Integer requiredFlag,
            String description
    ) {
    }

    private long normalizePageNum(RiskGovernanceGapQuery query) {
        return query == null || query.getPageNum() == null || query.getPageNum() < 1 ? 1L : query.getPageNum();
    }

    private long normalizePageSize(RiskGovernanceGapQuery query) {
        return query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10L : Math.min(query.getPageSize(), 100L);
    }

    private <T> PageResult<T> toPage(List<T> items, long pageNum, long pageSize) {
        if (items == null || items.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, items.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, items.size());
        return PageResult.of((long) items.size(), pageNum, pageSize, items.subList(fromIndex, toIndex));
    }

    private record MetricBindingDimension(String dimensionKey, String metricIdentifier, String metricName) {
    }

    private static final class RawStageProductAccumulator {
        private final Product product;
        private int totalEvidenceCount;
        private LocalDateTime lastSeenTime;

        private RawStageProductAccumulator(Product product) {
            this.product = product;
        }

        private void add(VendorMetricEvidence evidence) {
            int currentCount = evidence == null || evidence.getEvidenceCount() == null || evidence.getEvidenceCount() < 1
                    ? 1
                    : evidence.getEvidenceCount();
            totalEvidenceCount += currentCount;
            LocalDateTime candidateTime = evidence == null ? null : evidence.getLastSeenTime();
            if (candidateTime != null && (lastSeenTime == null || candidateTime.isAfter(lastSeenTime))) {
                lastSeenTime = candidateTime;
            }
        }

        private int totalEvidenceCount() {
            return totalEvidenceCount;
        }

        private LocalDateTime lastSeenTime() {
            return lastSeenTime;
        }

        private String vendorDisplayName() {
            if (product == null) {
                return "未标注厂商";
            }
            if (StringUtils.hasText(product.getManufacturer())) {
                return product.getManufacturer().trim();
            }
            return "未标注厂商";
        }

        private String productDisplayName() {
            if (product == null) {
                return "--";
            }
            if (StringUtils.hasText(product.getProductName())) {
                return product.getProductName().trim();
            }
            if (StringUtils.hasText(product.getProductKey())) {
                return product.getProductKey().trim();
            }
            return String.valueOf(product.getId());
        }
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
