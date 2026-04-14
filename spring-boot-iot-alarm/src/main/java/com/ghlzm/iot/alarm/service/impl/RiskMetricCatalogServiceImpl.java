package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.alarm.service.spi.RiskMetricScenarioResolver;
import com.ghlzm.iot.common.event.governance.RiskMetricCatalogPublishedEvent;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Risk metric catalog service implementation.
 */
@Service
public class RiskMetricCatalogServiceImpl implements RiskMetricCatalogService {

    private final RiskMetricCatalogMapper riskMetricCatalogMapper;
    private final ProductMapper productMapper;
    private final NormativeMetricDefinitionService normativeMetricDefinitionService;
    private final List<RiskMetricScenarioResolver> scenarioResolvers;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PublishedProductContractSnapshotService snapshotService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskMetricCatalogServiceImpl(RiskMetricCatalogMapper riskMetricCatalogMapper,
                                        ProductMapper productMapper,
                                        NormativeMetricDefinitionService normativeMetricDefinitionService,
                                        List<RiskMetricScenarioResolver> scenarioResolvers,
                                      ApplicationEventPublisher applicationEventPublisher) {
        this(
                riskMetricCatalogMapper,
                productMapper,
                normativeMetricDefinitionService,
                scenarioResolvers,
                applicationEventPublisher,
                null
        );
    }

    @Autowired
    public RiskMetricCatalogServiceImpl(RiskMetricCatalogMapper riskMetricCatalogMapper,
                                        ProductMapper productMapper,
                                        NormativeMetricDefinitionService normativeMetricDefinitionService,
                                        List<RiskMetricScenarioResolver> scenarioResolvers,
                                        ApplicationEventPublisher applicationEventPublisher,
                                        @Nullable PublishedProductContractSnapshotService snapshotService) {
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.productMapper = productMapper;
        this.normativeMetricDefinitionService = normativeMetricDefinitionService;
        this.scenarioResolvers = scenarioResolvers == null ? List.of() : List.copyOf(scenarioResolvers);
        this.applicationEventPublisher = applicationEventPublisher;
        this.snapshotService = snapshotService;
    }

    @Override
    public void publishFromReleasedContracts(Long productId,
                                             Long releaseBatchId,
                                             List<ProductModel> releasedContracts,
                                             Set<String> riskEnabledIdentifiers) {
        if (productId == null || releasedContracts == null || releasedContracts.isEmpty()) {
            return;
        }
        PublishedProductContractSnapshot snapshot = loadPublishedSnapshot(productId);
        Map<String, String> collectorParentFormalByLeaf = buildCollectorParentFormalByLeaf(releasedContracts);
        Set<String> enabledIdentifiers = normalizeIdentifiers(riskEnabledIdentifiers, snapshot, collectorParentFormalByLeaf);
        if (enabledIdentifiers.isEmpty()) {
            return;
        }
        Map<String, RiskMetricCatalog> existingByIdentifier = listAllByProduct(productId).stream()
                .filter(row -> StringUtils.hasText(row.getContractIdentifier()))
                .collect(LinkedHashMap::new,
                        (map, row) -> map.put(
                                normalizeExistingCatalogIdentifier(
                                        row.getContractIdentifier(),
                                        snapshot,
                                        collectorParentFormalByLeaf
                                ),
                                row
                        ),
                        Map::putAll);
        Map<String, MetricSemanticProfile> semanticByIdentifier =
                resolveSemanticProfiles(productId, releasedContracts, enabledIdentifiers, snapshot);
        Set<String> publishedIdentifiers = new LinkedHashSet<>();
        List<Long> publishedRiskMetricIds = new ArrayList<>();
        List<Long> retiredRiskMetricIds = new ArrayList<>();
        for (ProductModel contract : releasedContracts) {
            String identifier = normalizeIdentifier(contract == null ? null : contract.getIdentifier(), snapshot);
            if (!StringUtils.hasText(identifier) || !enabledIdentifiers.contains(identifier)) {
                continue;
            }
            RiskMetricCatalog row = existingByIdentifier.remove(identifier);
            MetricSemanticProfile profile = semanticByIdentifier.getOrDefault(identifier, MetricSemanticProfile.empty());
            if (row == null) {
                row = new RiskMetricCatalog();
                row.setProductId(productId);
                row.setContractIdentifier(identifier);
                row.setEnabled(1);
                populateCatalogRow(row, releaseBatchId, contract, profile, identifier);
                riskMetricCatalogMapper.insert(row);
            } else {
                populateCatalogRow(row, releaseBatchId, contract, profile, identifier);
                row.setEnabled(1);
                row.setDeleted(0);
                riskMetricCatalogMapper.updateById(row);
            }
            publishedIdentifiers.add(identifier);
            if (row.getId() != null) {
                publishedRiskMetricIds.add(row.getId());
            }
        }

        for (RiskMetricCatalog stale : existingByIdentifier.values()) {
            if (stale == null || !StringUtils.hasText(stale.getContractIdentifier())) {
                continue;
            }
            if (publishedIdentifiers.contains(stale.getContractIdentifier())) {
                continue;
            }
            stale.setEnabled(0);
            stale.setLifecycleStatus("RETIRED");
            riskMetricCatalogMapper.updateById(stale);
            if (stale.getId() != null) {
                retiredRiskMetricIds.add(stale.getId());
            }
        }
        publishCatalogEvent(productId, releaseBatchId, publishedRiskMetricIds, retiredRiskMetricIds);
    }

    @Override
    public RiskMetricCatalog getRequiredByProductAndIdentifier(Long productId, String contractIdentifier) {
        RiskMetricCatalog catalog = getByProductAndIdentifier(productId, contractIdentifier);
        if (catalog == null) {
            throw new BizException("\u98ce\u9669\u6307\u6807\u76ee\u5f55\u672a\u53d1\u5e03: " + contractIdentifier);
        }
        return catalog;
    }

    @Override
    public RiskMetricCatalog getByProductAndIdentifier(Long productId, String contractIdentifier) {
        String normalizedIdentifier = normalize(contractIdentifier);
        if (productId == null || !StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return riskMetricCatalogMapper.selectOne(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .eq(RiskMetricCatalog::getProductId, productId)
                .eq(RiskMetricCatalog::getContractIdentifier, normalizedIdentifier)
                .last("limit 1"));
    }

    @Override
    public RiskMetricCatalog getById(Long riskMetricId) {
        if (riskMetricId == null) {
            return null;
        }
        RiskMetricCatalog catalog = riskMetricCatalogMapper.selectById(riskMetricId);
        if (catalog == null || Integer.valueOf(1).equals(catalog.getDeleted()) || !Integer.valueOf(1).equals(catalog.getEnabled())) {
            return null;
        }
        return catalog;
    }

    @Override
    public List<RiskMetricCatalog> listEnabledByProduct(Long productId) {
        if (productId == null) {
            return List.of();
        }
        List<RiskMetricCatalog> rows = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .eq(RiskMetricCatalog::getProductId, productId));
        return rows == null ? List.of() : rows;
    }

    private List<RiskMetricCatalog> listAllByProduct(Long productId) {
        if (productId == null) {
            return List.of();
        }
        List<RiskMetricCatalog> rows = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getProductId, productId));
        return rows == null ? List.of() : rows;
    }

    private Map<String, MetricSemanticProfile> resolveSemanticProfiles(Long productId,
                                                                       List<ProductModel> releasedContracts,
                                                                       Set<String> enabledIdentifiers,
                                                                       PublishedProductContractSnapshot snapshot) {
        if (releasedContracts == null || releasedContracts.isEmpty() || enabledIdentifiers == null || enabledIdentifiers.isEmpty()) {
            return Map.of();
        }
        Product product = productMapper.selectById(productId);
        String scenarioCode = resolveScenarioCode(product);
        Map<String, NormativeMetricDefinition> normativeByIdentifier = loadNormativeByIdentifier(scenarioCode);
        Map<String, MetricSemanticProfile> semanticByIdentifier = new LinkedHashMap<>();
        for (ProductModel contract : releasedContracts) {
            String identifier = normalizeIdentifier(contract == null ? null : contract.getIdentifier(), snapshot);
            if (!StringUtils.hasText(identifier) || !enabledIdentifiers.contains(identifier)) {
                continue;
            }
            NormativeMetricDefinition definition = normativeByIdentifier.get(identifier);
            semanticByIdentifier.put(identifier, buildSemanticProfile(contract, definition, scenarioCode, identifier));
        }
        return semanticByIdentifier;
    }

    private Map<String, NormativeMetricDefinition> loadNormativeByIdentifier(String scenarioCode) {
        if (!StringUtils.hasText(scenarioCode)) {
            return Map.of();
        }
        List<NormativeMetricDefinition> definitions = normativeMetricDefinitionService.listByScenario(scenarioCode);
        if (definitions == null || definitions.isEmpty()) {
            return Map.of();
        }
        Map<String, NormativeMetricDefinition> map = new LinkedHashMap<>();
        for (NormativeMetricDefinition definition : definitions) {
            String identifier = normalize(definition == null ? null : definition.getIdentifier());
            if (!StringUtils.hasText(identifier)) {
                continue;
            }
            map.putIfAbsent(identifier, definition);
        }
        return map;
    }

    private MetricSemanticProfile buildSemanticProfile(ProductModel contract,
                                                       NormativeMetricDefinition definition,
                                                       String fallbackScenarioCode,
                                                       String canonicalIdentifier) {
        Map<String, Object> specs = parseJsonObject(contract == null ? null : contract.getSpecsJson());
        Map<String, Object> metadata = parseJsonObject(definition == null ? null : definition.getMetadataJson());

        String scenarioCode = firstNonBlank(
                normalize(definition == null ? null : definition.getScenarioCode()),
                normalize(fallbackScenarioCode)
        );
        String normativeIdentifier = firstNonBlank(
                normalize(definition == null ? null : definition.getIdentifier()),
                normalize(canonicalIdentifier)
        );
        String metricUnit = firstNonBlank(
                readString(specs, "unit"),
                normalize(definition == null ? null : definition.getUnit()),
                readString(metadata, "unit")
        );
        String metricDimension = firstNonBlank(
                readString(specs, "dimension"),
                normalize(definition == null ? null : definition.getMetricDimension()),
                readString(metadata, "dimension"),
                normalize(definition == null ? null : definition.getMonitorTypeCode())
        );
        String thresholdType = firstNonBlank(
                readString(specs, "thresholdType"),
                normalize(definition == null ? null : definition.getThresholdType()),
                readString(metadata, "thresholdType"),
                readString(metadata, "thresholdKind")
        );
        String semanticDirection = firstNonBlank(
                readString(specs, "semanticDirection"),
                normalize(definition == null ? null : definition.getSemanticDirection()),
                readString(metadata, "semanticDirection")
        );
        String thresholdDirection = firstNonBlank(
                readString(specs, "thresholdDirection"),
                readString(metadata, "thresholdDirection"),
                normalize(definition == null ? null : definition.getSemanticDirection()),
                normalize(semanticDirection),
                resolveDefaultThresholdDirection(contract)
        );
        String normalizedSemanticDirection = firstNonBlank(semanticDirection, thresholdDirection);
        String riskCategory = firstNonBlank(
                readString(specs, "riskCategory"),
                readString(metadata, "riskCategory"),
                normalize(definition == null ? null : definition.getMonitorContentCode()),
                scenarioCode
        );
        String metricRole = firstNonBlank(
                readString(specs, "metricRole"),
                readString(metadata, "metricRole"),
                defaultMetricRole(contract)
        );
        String lifecycleStatus = firstNonBlank(
                readString(specs, "lifecycleStatus"),
                readString(metadata, "lifecycleStatus"),
                normalize(definition == null ? null : definition.getStatus()),
                "ACTIVE"
        );

        Integer trendEnabled = firstNonNull(
                readBooleanAsInt(specs, "trendEnabled"),
                normalizeFlag(definition == null ? null : definition.getTrendEnabled()),
                readBooleanAsInt(metadata, "trendEnabled"),
                defaultTrendEnabled(contract)
        );
        Integer gisEnabled = firstNonNull(
                readBooleanAsInt(specs, "gisEnabled", "fitGis"),
                normalizeFlag(definition == null ? null : definition.getGisEnabled()),
                readBooleanAsInt(metadata, "gisEnabled", "fitGis"),
                0
        );
        Integer insightEnabled = firstNonNull(
                readBooleanAsInt(specs, "insightEnabled", "fitInsight"),
                normalizeFlag(definition == null ? null : definition.getInsightEnabled()),
                readBooleanAsInt(metadata, "insightEnabled", "fitInsight"),
                1
        );
        Integer analyticsEnabled = firstNonNull(
                readBooleanAsInt(specs, "analyticsEnabled", "fitAnalytics"),
                normalizeFlag(definition == null ? null : definition.getAnalyticsEnabled()),
                readBooleanAsInt(metadata, "analyticsEnabled", "fitAnalytics"),
                defaultAnalyticsEnabled(contract)
        );

        return new MetricSemanticProfile(
                scenarioCode,
                normativeIdentifier,
                riskCategory,
                metricRole,
                lifecycleStatus,
                metricUnit,
                metricDimension,
                thresholdType,
                normalizedSemanticDirection,
                thresholdDirection,
                trendEnabled,
                gisEnabled,
                insightEnabled,
                analyticsEnabled
        );
    }

    private void populateCatalogRow(RiskMetricCatalog row,
                                    Long releaseBatchId,
                                    ProductModel contract,
                                    MetricSemanticProfile profile,
                                    String identifier) {
        row.setReleaseBatchId(releaseBatchId);
        row.setProductModelId(contract == null ? null : contract.getId());
        row.setNormativeIdentifier(firstNonBlank(profile.normativeIdentifier(), identifier));
        row.setContractIdentifier(identifier);
        row.setRiskMetricCode(buildRiskMetricCode(row.getProductId(), identifier));
        row.setRiskMetricName(resolveRiskMetricName(contract));
        row.setRiskCategory(profile.riskCategory());
        row.setMetricRole(profile.metricRole());
        row.setLifecycleStatus(profile.lifecycleStatus());
        row.setSourceScenarioCode(profile.sourceScenarioCode());
        row.setMetricUnit(profile.metricUnit());
        row.setMetricDimension(profile.metricDimension());
        row.setThresholdType(profile.thresholdType());
        row.setSemanticDirection(profile.semanticDirection());
        row.setThresholdDirection(profile.thresholdDirection());
        row.setTrendEnabled(profile.trendEnabled());
        row.setGisEnabled(profile.gisEnabled());
        row.setInsightEnabled(profile.insightEnabled());
        row.setAnalyticsEnabled(profile.analyticsEnabled());
    }

    private PublishedProductContractSnapshot loadPublishedSnapshot(Long productId) {
        if (snapshotService == null || productId == null) {
            return PublishedProductContractSnapshot.empty(productId);
        }
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(productId);
        return snapshot == null ? PublishedProductContractSnapshot.empty(productId) : snapshot;
    }

    private void publishCatalogEvent(Long productId,
                                     Long releaseBatchId,
                                     List<Long> publishedRiskMetricIds,
                                     List<Long> retiredRiskMetricIds) {
        if (applicationEventPublisher == null || productId == null || releaseBatchId == null) {
            return;
        }
        Product product = productMapper.selectById(productId);
        applicationEventPublisher.publishEvent(new RiskMetricCatalogPublishedEvent(
                defaultTenantId(product == null ? null : product.getTenantId()),
                productId,
                releaseBatchId,
                publishedRiskMetricIds == null ? List.of() : List.copyOf(publishedRiskMetricIds),
                retiredRiskMetricIds == null ? List.of() : List.copyOf(retiredRiskMetricIds),
                null
        ));
    }

    private String resolveScenarioCode(Product product) {
        if (product == null || scenarioResolvers == null || scenarioResolvers.isEmpty()) {
            return null;
        }
        for (RiskMetricScenarioResolver resolver : scenarioResolvers) {
            if (resolver == null) {
                continue;
            }
            String resolved = normalize(resolver.resolveScenarioCode(product));
            if (StringUtils.hasText(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    private String resolveRiskMetricName(ProductModel contract) {
        String modelName = contract == null ? null : normalize(contract.getModelName());
        if (StringUtils.hasText(modelName)) {
            return modelName;
        }
        return normalize(contract == null ? null : contract.getIdentifier());
    }

    private String buildRiskMetricCode(Long productId, String identifier) {
        String normalizedIdentifier = normalize(identifier);
        if (!StringUtils.hasText(normalizedIdentifier)) {
            return null;
        }
        return "RM_" + (productId == null ? "GLOBAL" : productId) + "_" + normalizedIdentifier.toUpperCase(Locale.ROOT);
    }

    private String resolveDefaultThresholdDirection(ProductModel contract) {
        return isNumericDataType(contract == null ? null : contract.getDataType())
                ? "HIGHER_IS_RISKIER"
                : null;
    }

    private Integer defaultTrendEnabled(ProductModel contract) {
        return isNumericDataType(contract == null ? null : contract.getDataType()) ? 1 : 0;
    }

    private Integer defaultAnalyticsEnabled(ProductModel contract) {
        return isNumericDataType(contract == null ? null : contract.getDataType()) ? 1 : 0;
    }

    private String defaultMetricRole(ProductModel contract) {
        if (contract == null) {
            return "PRIMARY";
        }
        return isNumericDataType(contract.getDataType()) ? "PRIMARY" : "STATE";
    }

    private boolean isNumericDataType(String dataType) {
        String normalized = normalize(dataType);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return Set.of("double", "float", "int", "integer", "long", "short", "decimal").contains(lower);
    }

    private Set<String> normalizeIdentifiers(Set<String> identifiers,
                                             PublishedProductContractSnapshot snapshot,
                                             Map<String, String> collectorParentFormalByLeaf) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            String value = normalizeExistingCatalogIdentifier(identifier, snapshot, collectorParentFormalByLeaf);
            if (StringUtils.hasText(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String normalizeExistingCatalogIdentifier(String identifier,
                                                      PublishedProductContractSnapshot snapshot,
                                                      Map<String, String> collectorParentFormalByLeaf) {
        String normalized = normalizeIdentifier(identifier, snapshot);
        if (!StringUtils.hasText(normalized)
                || normalized.contains(".")
                || collectorParentFormalByLeaf == null
                || collectorParentFormalByLeaf.isEmpty()) {
            return normalized;
        }
        return collectorParentFormalByLeaf.getOrDefault(normalized.toLowerCase(Locale.ROOT), normalized);
    }

    private String normalizeIdentifier(String identifier, PublishedProductContractSnapshot snapshot) {
        String normalized = normalize(identifier);
        if (!StringUtils.hasText(normalized) || snapshot == null) {
            return normalized;
        }
        return snapshot.canonicalAliasOf(normalized).orElse(normalized);
    }

    private Map<String, String> buildCollectorParentFormalByLeaf(List<ProductModel> releasedContracts) {
        if (releasedContracts == null || releasedContracts.isEmpty()) {
            return Map.of();
        }
        Map<String, String> collectorParentFormalByLeaf = new LinkedHashMap<>();
        for (ProductModel releasedContract : releasedContracts) {
            String identifier = normalize(releasedContract == null ? null : releasedContract.getIdentifier());
            if (!isCollectorParentFullPathIdentifier(identifier)) {
                continue;
            }
            collectorParentFormalByLeaf.putIfAbsent(
                    lastIdentifierSegment(identifier).toLowerCase(Locale.ROOT),
                    identifier
            );
        }
        return collectorParentFormalByLeaf;
    }

    private boolean isCollectorParentFullPathIdentifier(String identifier) {
        return StringUtils.hasText(identifier)
                && identifier.startsWith("S1_ZT_1.")
                && !identifier.startsWith("S1_ZT_1.sensor_state.");
    }

    private String lastIdentifierSegment(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return identifier;
        }
        int separatorIndex = identifier.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex >= identifier.length() - 1) {
            return identifier;
        }
        return identifier.substring(separatorIndex + 1);
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            var node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                return Map.of();
            }
            Map<String, Object> parsed = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
            });
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String readString(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (String key : keys) {
            if (!StringUtils.hasText(key) || !source.containsKey(key)) {
                continue;
            }
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            String normalized = normalize(String.valueOf(value));
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private Integer readBooleanAsInt(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (String key : keys) {
            if (!StringUtils.hasText(key) || !source.containsKey(key)) {
                continue;
            }
            Object value = source.get(key);
            Integer parsed = toBooleanInt(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Integer toBooleanInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (value instanceof Number number) {
            return number.intValue() > 0 ? 1 : 0;
        }
        String normalized = normalize(String.valueOf(value));
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ("1".equals(normalized) || "true".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return 1;
        }
        if ("0".equals(normalized) || "false".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            return 0;
        }
        return null;
    }

    private Integer normalizeFlag(Integer value) {
        if (value == null) {
            return null;
        }
        return value > 0 ? 1 : 0;
    }

    private String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? 1L : tenantId;
    }

    record MetricSemanticProfile(
            String sourceScenarioCode,
            String normativeIdentifier,
            String riskCategory,
            String metricRole,
            String lifecycleStatus,
            String metricUnit,
            String metricDimension,
            String thresholdType,
            String semanticDirection,
            String thresholdDirection,
            Integer trendEnabled,
            Integer gisEnabled,
            Integer insightEnabled,
            Integer analyticsEnabled
    ) {
        static MetricSemanticProfile empty() {
            return new MetricSemanticProfile(null, null, null, "PRIMARY", "ACTIVE", null, null, null, null, null, 0, 0, 1, 0);
        }
    }
}
