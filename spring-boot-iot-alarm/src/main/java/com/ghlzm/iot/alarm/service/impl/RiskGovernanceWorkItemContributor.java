package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.system.service.GovernanceWorkItemContributor;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RiskGovernanceWorkItemContributor implements GovernanceWorkItemContributor {

    private static final Long SYSTEM_OPERATOR_ID = 1L;

    private final ProductMapper productMapper;
    private final ProductContractReleaseBatchMapper productContractReleaseBatchMapper;
    private final RiskMetricCatalogMapper riskMetricCatalogMapper;
    private final DeviceMapper deviceMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final RiskMetricLinkageBindingMapper linkageBindingMapper;
    private final RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;
    private final RiskMetricActionBindingBackfillService backfillService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskGovernanceWorkItemContributor(ProductMapper productMapper,
                                             ProductContractReleaseBatchMapper productContractReleaseBatchMapper,
                                             RiskMetricCatalogMapper riskMetricCatalogMapper,
                                             DeviceMapper deviceMapper,
                                             RiskPointDeviceMapper riskPointDeviceMapper,
                                             RuleDefinitionMapper ruleDefinitionMapper,
                                             RiskMetricLinkageBindingMapper linkageBindingMapper,
                                             RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper,
                                             RiskMetricActionBindingBackfillService backfillService) {
        this.productMapper = productMapper;
        this.productContractReleaseBatchMapper = productContractReleaseBatchMapper;
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.deviceMapper = deviceMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.linkageBindingMapper = linkageBindingMapper;
        this.emergencyPlanBindingMapper = emergencyPlanBindingMapper;
        this.backfillService = backfillService;
    }

    @Override
    public List<GovernanceWorkItemCommand> collectWorkItems() {
        backfillService.ensureBindingsReadyForRead();

        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0));
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> productMap = products.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
        Set<Long> productIds = productMap.keySet();

        List<ProductContractReleaseBatch> releaseBatches = productContractReleaseBatchMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getDeleted, 0)
        );
        Map<Long, ProductContractReleaseBatch> latestReleaseBatchByProductId = releaseBatches.stream()
                .filter(batch -> batch.getProductId() != null && productIds.contains(batch.getProductId()))
                .collect(Collectors.toMap(
                        ProductContractReleaseBatch::getProductId,
                        batch -> batch,
                        this::chooseLatestReleaseBatch,
                        LinkedHashMap::new
                ));
        Set<Long> releasedProductIds = latestReleaseBatchByProductId.keySet();

        List<RiskMetricCatalog> enabledCatalogs = selectEnabledCatalogs();
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
                .filter(productIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> catalogProductByRiskMetricId = enabledCatalogs.stream()
                .filter(catalog -> catalog.getId() != null && catalog.getProductId() != null)
                .collect(Collectors.toMap(RiskMetricCatalog::getId, RiskMetricCatalog::getProductId, (left, right) -> left, LinkedHashMap::new));
        Map<String, Long> catalogProductByIdentifier = enabledCatalogs.stream()
                .filter(catalog -> catalog.getProductId() != null)
                .filter(catalog -> StringUtils.hasText(catalog.getContractIdentifier()))
                .collect(Collectors.toMap(
                        catalog -> normalizeLower(catalog.getContractIdentifier()),
                        RiskMetricCatalog::getProductId,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<Long> governedProductIds = new LinkedHashSet<>(releasedProductIds);
        governedProductIds.addAll(catalogProductIds);

        List<Device> reportedDevices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeleted, 0)
                .isNotNull(Device::getLastReportTime)
                .orderByDesc(Device::getLastReportTime));
        Map<Long, Device> reportedDeviceMap = reportedDevices.stream()
                .filter(device -> device.getId() != null)
                .collect(Collectors.toMap(Device::getId, device -> device, (left, right) -> left, LinkedHashMap::new));
        Set<Long> reportedDeviceIds = reportedDeviceMap.keySet();

        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0));
        Set<Long> boundDeviceIds = bindings.stream()
                .map(RiskPointDevice::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Long> reportedDeviceCountByProductId = reportedDevices.stream()
                .filter(device -> device.getProductId() != null)
                .collect(Collectors.groupingBy(Device::getProductId, LinkedHashMap::new, Collectors.counting()));
        Map<Long, Long> unboundReportedDeviceCountByProductId = reportedDevices.stream()
                .filter(device -> device.getId() != null)
                .filter(device -> device.getProductId() != null)
                .filter(device -> !boundDeviceIds.contains(device.getId()))
                .collect(Collectors.groupingBy(Device::getProductId, LinkedHashMap::new, Collectors.counting()));
        Set<String> boundMetricKeys = bindings.stream()
                .map(this::toBindingMetricKey)
                .filter(StringUtils::hasText)
                .filter(key -> matchesCatalogKey(key, catalogIds, catalogIdentifiers))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MetricBindingDimension> boundMetricDimensions = buildBoundMetricDimensions(bindings, reportedDeviceMap, catalogIds, catalogIdentifiers);
        Map<Long, Long> catalogCountByProductId = enabledCatalogs.stream()
                .filter(catalog -> catalog.getProductId() != null)
                .collect(Collectors.groupingBy(RiskMetricCatalog::getProductId, LinkedHashMap::new, Collectors.counting()));
        Map<Long, Long> bindingCountByProductId = bindings.stream()
                .map(binding -> binding.getDeviceId() == null ? null : reportedDeviceMap.get(binding.getDeviceId()))
                .filter(Objects::nonNull)
                .filter(device -> device.getProductId() != null)
                .collect(Collectors.groupingBy(Device::getProductId, LinkedHashMap::new, Collectors.counting()));

        List<RuleDefinition> enabledRules = ruleDefinitionMapper.selectList(new LambdaQueryWrapper<RuleDefinition>()
                .eq(RuleDefinition::getDeleted, 0)
                .eq(RuleDefinition::getStatus, 0));
        Map<String, List<RuleDefinition>> enabledRulesByMetric = enabledRules.stream()
                .filter(rule -> StringUtils.hasText(rule.getMetricIdentifier()))
                .collect(Collectors.groupingBy(rule -> rule.getMetricIdentifier().trim()));
        Map<Long, List<RuleDefinition>> enabledRulesByRiskMetricId = enabledRules.stream()
                .filter(rule -> rule.getRiskMetricId() != null)
                .collect(Collectors.groupingBy(RuleDefinition::getRiskMetricId));
        List<RiskPointDevice> missingPolicyBindings = bindings.stream()
                .filter(binding -> !matchesPolicy(binding, enabledRulesByMetric, enabledRulesByRiskMetricId))
                .sorted(Comparator.comparing(RiskPointDevice::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        Map<Long, Long> missingPolicyCountByProductId = missingPolicyBindings.stream()
                .map(binding -> binding.getDeviceId() == null ? null : reportedDeviceMap.get(binding.getDeviceId()))
                .filter(Objects::nonNull)
                .filter(device -> device.getProductId() != null)
                .collect(Collectors.groupingBy(Device::getProductId, LinkedHashMap::new, Collectors.counting()));

        Set<String> linkageCoveredKeys = toLinkageDimensionKeys(selectActiveLinkageBindings());
        Set<String> emergencyCoveredKeys = toEmergencyPlanDimensionKeys(selectActiveEmergencyPlanBindings());

        List<GovernanceWorkItemCommand> commands = new ArrayList<>();
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            if (!governedProductIds.contains(product.getId())) {
                commands.add(new GovernanceWorkItemCommand(
                        "PENDING_PRODUCT_GOVERNANCE",
                        "PRODUCT",
                        product.getId(),
                        product.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        trimToNull(product.getProductKey()),
                        null,
                        "MODEL_GOVERNANCE",
                        "产品尚未进入治理主链路",
                        snapshotOf(snapshotMap(
                                "productId", product.getId(),
                                "productKey", safeText(product.getProductKey()),
                                "productName", safeText(product.getProductName())
                        )),
                        "P2",
                        SYSTEM_OPERATOR_ID
                ));
            }
            if (!releasedProductIds.contains(product.getId())) {
                long catalogCount = catalogCountByProductId.getOrDefault(product.getId(), 0L);
                long reportedDeviceCount = reportedDeviceCountByProductId.getOrDefault(product.getId(), 0L);
                long missingBindingCount = unboundReportedDeviceCountByProductId.getOrDefault(product.getId(), 0L);
                long missingPolicyCount = missingPolicyCountByProductId.getOrDefault(product.getId(), 0L);
                long bindingCount = bindingCountByProductId.getOrDefault(product.getId(), 0L);
                long affectedCount = Math.max(catalogCount + missingBindingCount + missingPolicyCount, Math.max(bindingCount, reportedDeviceCount));
                commands.add(new GovernanceWorkItemCommand(
                        "PENDING_CONTRACT_RELEASE",
                        "PRODUCT",
                        product.getId(),
                        product.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        trimToNull(product.getProductKey()),
                        null,
                        "CONTRACT_RELEASE",
                        "产品尚未形成正式合同发布批次",
                        snapshotOf(snapshotMap(
                                "productId", product.getId(),
                                "productKey", safeText(product.getProductKey()),
                                "productName", safeText(product.getProductName()),
                                "catalogCount", catalogCount,
                                "reportedDeviceCount", reportedDeviceCount,
                                "bindingCount", bindingCount,
                                "missingBindingCount", missingBindingCount,
                                "missingPolicyCount", missingPolicyCount,
                                "affectedCount", affectedCount
                        )),
                        "P1",
                        SYSTEM_OPERATOR_ID
                ));
            }
        }

        for (Device device : reportedDevices) {
            if (device == null || device.getId() == null || boundDeviceIds.contains(device.getId())) {
                continue;
            }
            Product product = device.getProductId() == null ? null : productMap.get(device.getProductId());
            commands.add(new GovernanceWorkItemCommand(
                    "PENDING_RISK_BINDING",
                    "DEVICE",
                    device.getId(),
                    device.getProductId(),
                    null,
                    null,
                    null,
                    null,
                    trimToNull(device.getDeviceCode()),
                    product == null ? null : trimToNull(product.getProductKey()),
                    null,
                    "RISK_BINDING",
                    "设备已上报，待绑定风险点",
                    snapshotOf(snapshotMap(
                            "deviceId", device.getId(),
                            "deviceCode", safeText(device.getDeviceCode()),
                            "deviceName", safeText(device.getDeviceName()),
                            "productId", device.getProductId(),
                            "missingBindingCount", 1,
                            "affectedCount", 1
                    )),
                    "P1",
                    SYSTEM_OPERATOR_ID
            ));
        }

        for (RiskPointDevice binding : missingPolicyBindings) {
            Long productId = productIdOfBinding(binding, reportedDeviceMap, catalogProductByRiskMetricId, catalogProductByIdentifier);
            Product product = productId == null ? null : productMap.get(productId);
            long missingPolicyCount = productId == null ? 1L : missingPolicyCountByProductId.getOrDefault(productId, 1L);
            commands.add(new GovernanceWorkItemCommand(
                    "PENDING_THRESHOLD_POLICY",
                    "RISK_POINT_DEVICE",
                    subjectIdOfBinding(binding),
                    productId,
                    binding.getRiskMetricId(),
                    null,
                    null,
                    null,
                    deviceCodeOfBinding(binding, reportedDeviceMap),
                    product == null ? null : trimToNull(product.getProductKey()),
                    null,
                    "RULE_DEFINITION",
                    "风险点已绑定，待补阈值策略",
                    snapshotOf(snapshotMap(
                            "riskPointDeviceId", subjectIdOfBinding(binding),
                            "riskPointId", binding.getRiskPointId(),
                            "deviceId", binding.getDeviceId(),
                            "deviceCode", safeText(binding.getDeviceCode()),
                            "metricIdentifier", safeText(binding.getMetricIdentifier()),
                            "metricName", safeText(binding.getMetricName()),
                            "missingPolicyCount", missingPolicyCount,
                            "affectedCount", missingPolicyCount
                    )),
                    "P1",
                    SYSTEM_OPERATOR_ID
            ));
        }

        for (MetricBindingDimension dimension : boundMetricDimensions) {
            if (!linkageCoveredKeys.contains(dimension.dimensionKey())) {
                Product product = dimension.productId() == null ? null : productMap.get(dimension.productId());
                commands.add(new GovernanceWorkItemCommand(
                        "PENDING_LINKAGE_PLAN",
                        "LINKAGE_DIMENSION",
                        stableSubjectId("LINKAGE:" + dimension.dimensionKey()),
                        dimension.productId(),
                        dimension.riskMetricId(),
                        null,
                        null,
                        null,
                        null,
                        product == null ? null : trimToNull(product.getProductKey()),
                        null,
                        "LINKAGE_RULE",
                        "已纳管指标待补联动规则",
                        snapshotOf(snapshotMap(
                                "coverageType", "LINKAGE",
                                "dimensionKey", dimension.dimensionKey(),
                                "metricIdentifier", safeText(dimension.metricIdentifier()),
                                "metricName", safeText(dimension.metricName())
                        )),
                        "P2",
                        SYSTEM_OPERATOR_ID
                ));
            }
            if (!emergencyCoveredKeys.contains(dimension.dimensionKey())) {
                Product product = dimension.productId() == null ? null : productMap.get(dimension.productId());
                commands.add(new GovernanceWorkItemCommand(
                        "PENDING_LINKAGE_PLAN",
                        "EMERGENCY_DIMENSION",
                        stableSubjectId("EMERGENCY:" + dimension.dimensionKey()),
                        dimension.productId(),
                        dimension.riskMetricId(),
                        null,
                        null,
                        null,
                        null,
                        product == null ? null : trimToNull(product.getProductKey()),
                        null,
                        "EMERGENCY_PLAN",
                        "已纳管指标待补应急预案",
                        snapshotOf(snapshotMap(
                                "coverageType", "EMERGENCY_PLAN",
                                "dimensionKey", dimension.dimensionKey(),
                                "metricIdentifier", safeText(dimension.metricIdentifier()),
                                "metricName", safeText(dimension.metricName())
                        )),
                        "P2",
                        SYSTEM_OPERATOR_ID
                ));
            }
        }

        List<MissingPolicySignal> replaySignals = buildMissingPolicySignals(
                missingPolicyBindings,
                reportedDeviceMap,
                catalogProductByRiskMetricId,
                catalogProductByIdentifier
        );
        for (MissingPolicySignal signal : replaySignals) {
            Long productId = signal.productId() != null
                    ? signal.productId()
                    : catalogProductByRiskMetricId.get(signal.riskMetricId());
            if (productId == null && StringUtils.hasText(signal.metricIdentifier())) {
                productId = catalogProductByIdentifier.get(normalizeLower(signal.metricIdentifier()));
            }
            Product product = productId == null ? null : productMap.get(productId);
            ProductContractReleaseBatch replayBatch = productId == null ? null : latestReleaseBatchByProductId.get(productId);
            commands.add(new GovernanceWorkItemCommand(
                    "PENDING_REPLAY",
                    "REPLAY_CASE",
                    stableSubjectId("REPLAY:" + signal.dimensionKey()),
                    productId,
                    signal.riskMetricId(),
                    replayBatch == null ? null : replayBatch.getId(),
                    null,
                    null,
                    signal.deviceCode(),
                    product == null ? null : trimToNull(product.getProductKey()),
                    null,
                    "REPLAY",
                    "风险指标缺阈值策略，待运营复盘",
                    snapshotOf(snapshotMap(
                            "dimensionKey", signal.dimensionKey(),
                            "dimensionLabel", signal.dimensionLabel(),
                            "productId", productId,
                            "productKey", product == null ? null : trimToNull(product.getProductKey()),
                            "deviceCode", signal.deviceCode(),
                            "releaseBatchId", replayBatch == null ? null : replayBatch.getId(),
                            "riskMetricId", signal.riskMetricId(),
                            "metricIdentifier", safeText(signal.metricIdentifier()),
                            "metricName", safeText(signal.metricName()),
                            "bindingCount", signal.bindingCount(),
                            "riskPointCount", signal.riskPointCount(),
                            "missingPolicyCount", signal.bindingCount(),
                            "affectedCount", Math.max(signal.bindingCount(), signal.riskPointCount())
                    )),
                    "P2",
                    SYSTEM_OPERATOR_ID
            ));
        }

        return commands;
    }

    private List<RiskMetricCatalog> selectEnabledCatalogs() {
        QueryWrapper<RiskMetricCatalog> wrapper = new QueryWrapper<>();
        wrapper.select("id", "product_id", "contract_identifier")
                .eq("deleted", 0)
                .eq("enabled", 1);
        return riskMetricCatalogMapper.selectList(wrapper);
    }

    private List<MetricBindingDimension> buildBoundMetricDimensions(List<RiskPointDevice> bindings,
                                                                    Map<Long, Device> deviceMap,
                                                                    Set<Long> catalogIds,
                                                                    Set<String> catalogIdentifiers) {
        Map<String, MetricBindingDimension> dimensions = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            String key = toBindingMetricKey(binding);
            if (!matchesCatalogKey(key, catalogIds, catalogIdentifiers)) {
                continue;
            }
            MetricBindingDimension dimension = toMetricBindingDimension(binding, deviceMap);
            if (dimension != null) {
                dimensions.putIfAbsent(dimension.dimensionKey(), dimension);
            }
        }
        return new ArrayList<>(dimensions.values());
    }

    private MetricBindingDimension toMetricBindingDimension(RiskPointDevice binding, Map<Long, Device> deviceMap) {
        if (binding == null) {
            return null;
        }
        String metricIdentifier = normalizeLower(binding.getMetricIdentifier());
        String dimensionKey = binding.getRiskMetricId() != null
                ? "ID:" + binding.getRiskMetricId()
                : (StringUtils.hasText(metricIdentifier) ? "IDENT:" + metricIdentifier : null);
        if (!StringUtils.hasText(dimensionKey)) {
            return null;
        }
        Device device = binding.getDeviceId() == null ? null : deviceMap.get(binding.getDeviceId());
        Long productId = device == null ? null : device.getProductId();
        String metricName = StringUtils.hasText(binding.getMetricName()) ? binding.getMetricName().trim() : null;
        return new MetricBindingDimension(dimensionKey, productId, binding.getRiskMetricId(), metricIdentifier, metricName);
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

    private Set<String> toLinkageDimensionKeys(List<RiskMetricLinkageBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return Set.of();
        }
        return bindings.stream()
                .map(RiskMetricLinkageBinding::getRiskMetricId)
                .filter(Objects::nonNull)
                .map(value -> "ID:" + value)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> toEmergencyPlanDimensionKeys(List<RiskMetricEmergencyPlanBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return Set.of();
        }
        return bindings.stream()
                .map(RiskMetricEmergencyPlanBinding::getRiskMetricId)
                .filter(Objects::nonNull)
                .map(value -> "ID:" + value)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MissingPolicySignal> buildMissingPolicySignals(List<RiskPointDevice> bindings,
                                                                Map<Long, Device> deviceMap,
                                                                Map<Long, Long> catalogProductByRiskMetricId,
                                                                Map<String, Long> catalogProductByIdentifier) {
        Map<String, MissingPolicySignalAccumulator> accumulators = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            Long productId = productIdOfBinding(binding, deviceMap, catalogProductByRiskMetricId, catalogProductByIdentifier);
            String dimensionKey = toMissingPolicyDimensionKey(binding, productId);
            if (!StringUtils.hasText(dimensionKey)) {
                continue;
            }
            MissingPolicySignalAccumulator accumulator = accumulators.computeIfAbsent(
                    dimensionKey,
                    key -> new MissingPolicySignalAccumulator(
                            key,
                            toMissingPolicyDimensionLabel(binding, productId),
                            binding.getRiskMetricId(),
                            safeText(binding.getMetricIdentifier()),
                            safeText(binding.getMetricName())
                    )
            );
            accumulator.add(binding, productId, deviceCodeOfBinding(binding, deviceMap));
        }
        return accumulators.values().stream()
                .map(MissingPolicySignalAccumulator::toSignal)
                .sorted(Comparator.comparingLong(MissingPolicySignal::bindingCount).reversed())
                .toList();
    }

    private String toMissingPolicyDimensionKey(RiskPointDevice binding, Long productId) {
        if (binding == null) {
            return null;
        }
        if (binding.getRiskMetricId() != null) {
            return "risk_metric_id:" + binding.getRiskMetricId();
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            if (productId != null) {
                return "product_metric_identifier:" + productId + ":" + normalizeLower(binding.getMetricIdentifier());
            }
            return "metric_identifier:" + normalizeLower(binding.getMetricIdentifier());
        }
        return null;
    }

    private String toMissingPolicyDimensionLabel(RiskPointDevice binding, Long productId) {
        if (binding == null) {
            return "unknown";
        }
        if (binding.getRiskMetricId() != null) {
            return "riskMetricId=" + binding.getRiskMetricId();
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            if (productId != null) {
                return "productId=" + productId + ", metricIdentifier=" + binding.getMetricIdentifier().trim();
            }
            return "metricIdentifier=" + binding.getMetricIdentifier().trim();
        }
        return "unknown";
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

    private boolean matchesCatalogKey(String key, Set<Long> catalogIds, Set<String> catalogIdentifiers) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        if (key.startsWith("ID:")) {
            try {
                return catalogIds.contains(Long.parseLong(key.substring(3)));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (key.startsWith("IDENT:")) {
            return catalogIdentifiers.contains(key.substring(6));
        }
        return false;
    }

    private Long productIdOfBinding(RiskPointDevice binding,
                                    Map<Long, Device> deviceMap,
                                    Map<Long, Long> catalogProductByRiskMetricId,
                                    Map<String, Long> catalogProductByIdentifier) {
        if (binding == null) {
            return null;
        }
        if (binding.getDeviceId() != null) {
            Device device = deviceMap.get(binding.getDeviceId());
            if (device != null && device.getProductId() != null) {
                return device.getProductId();
            }
        }
        if (binding.getRiskMetricId() != null && catalogProductByRiskMetricId.containsKey(binding.getRiskMetricId())) {
            return catalogProductByRiskMetricId.get(binding.getRiskMetricId());
        }
        if (StringUtils.hasText(binding.getMetricIdentifier())) {
            return catalogProductByIdentifier.get(normalizeLower(binding.getMetricIdentifier()));
        }
        return null;
    }

    private String deviceCodeOfBinding(RiskPointDevice binding, Map<Long, Device> deviceMap) {
        if (binding == null) {
            return null;
        }
        if (StringUtils.hasText(binding.getDeviceCode())) {
            return binding.getDeviceCode().trim();
        }
        if (binding.getDeviceId() == null) {
            return null;
        }
        Device device = deviceMap.get(binding.getDeviceId());
        return device == null ? null : trimToNull(device.getDeviceCode());
    }

    private Long subjectIdOfBinding(RiskPointDevice binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getId() != null) {
            return binding.getId();
        }
        String raw = "BINDING:" + binding.getRiskPointId() + ":" + binding.getDeviceId() + ":" + safeText(binding.getMetricIdentifier());
        return stableSubjectId(raw);
    }

    private Long stableSubjectId(String raw) {
        CRC32 crc32 = new CRC32();
        crc32.update(raw.getBytes(StandardCharsets.UTF_8));
        long value = crc32.getValue();
        return value <= 0L ? 1L : value;
    }

    private String normalizeLower(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ProductContractReleaseBatch chooseLatestReleaseBatch(ProductContractReleaseBatch left,
                                                                 ProductContractReleaseBatch right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        LocalDateTime leftTime = left.getCreateTime();
        LocalDateTime rightTime = right.getCreateTime();
        if (leftTime == null && rightTime != null) {
            return right;
        }
        if (leftTime != null && rightTime == null) {
            return left;
        }
        if (leftTime != null && rightTime != null) {
            int timeCompare = leftTime.compareTo(rightTime);
            if (timeCompare != 0) {
                return timeCompare >= 0 ? left : right;
            }
        }
        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId == null) {
            return right;
        }
        if (rightId == null) {
            return left;
        }
        return leftId >= rightId ? left : right;
    }

    private Map<String, Object> snapshotMap(Object... keyValues) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            snapshot.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return snapshot;
    }

    private String snapshotOf(Map<String, ?> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            return null;
        }
    }

    private record MetricBindingDimension(String dimensionKey,
                                          Long productId,
                                          Long riskMetricId,
                                          String metricIdentifier,
                                          String metricName) {
    }

    private record MissingPolicySignal(String dimensionKey,
                                       String dimensionLabel,
                                       Long riskMetricId,
                                       String metricIdentifier,
                                       String metricName,
                                       long bindingCount,
                                       long riskPointCount,
                                       Long productId,
                                       String deviceCode) {
    }

    private static final class MissingPolicySignalAccumulator {

        private final String dimensionKey;
        private final String dimensionLabel;
        private final Long riskMetricId;
        private final String metricIdentifier;
        private final String metricName;
        private long bindingCount;
        private final Set<Long> riskPointIds = new LinkedHashSet<>();
        private final Set<Long> productIds = new LinkedHashSet<>();
        private final Set<String> deviceCodes = new LinkedHashSet<>();

        private MissingPolicySignalAccumulator(String dimensionKey,
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

        private void add(RiskPointDevice binding, Long productId, String deviceCode) {
            bindingCount++;
            if (binding != null && binding.getRiskPointId() != null) {
                riskPointIds.add(binding.getRiskPointId());
            }
            if (productId != null) {
                productIds.add(productId);
            }
            if (StringUtils.hasText(deviceCode)) {
                deviceCodes.add(deviceCode.trim());
            }
        }

        private MissingPolicySignal toSignal() {
            Long productId = productIds.size() == 1 ? productIds.iterator().next() : null;
            String deviceCode = deviceCodes.size() == 1 ? deviceCodes.iterator().next() : null;
            return new MissingPolicySignal(
                    dimensionKey,
                    dimensionLabel,
                    riskMetricId,
                    metricIdentifier,
                    metricName,
                    bindingCount,
                    riskPointIds.size(),
                    productId,
                    deviceCode
            );
        }
    }
}
