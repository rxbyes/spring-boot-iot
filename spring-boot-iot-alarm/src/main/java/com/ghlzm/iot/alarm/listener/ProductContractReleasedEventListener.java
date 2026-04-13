package com.ghlzm.iot.alarm.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.device.entity.ProductMetricResolverSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMetricResolverSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 正式合同发布后同步生成风险指标目录。
 */
@Component
public class ProductContractReleasedEventListener {

    private final ProductModelMapper productModelMapper;
    private final RiskMetricCatalogPublishRule riskMetricCatalogPublishRule;
    private final RiskMetricCatalogService riskMetricCatalogService;
    private final ProductMetricResolverSnapshotMapper resolverSnapshotMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public ProductContractReleasedEventListener(ProductModelMapper productModelMapper,
                                                RiskMetricCatalogPublishRule riskMetricCatalogPublishRule,
                                                RiskMetricCatalogService riskMetricCatalogService) {
        this(productModelMapper, riskMetricCatalogPublishRule, riskMetricCatalogService, null);
    }

    public ProductContractReleasedEventListener(ProductModelMapper productModelMapper,
                                                RiskMetricCatalogPublishRule riskMetricCatalogPublishRule,
                                                RiskMetricCatalogService riskMetricCatalogService,
                                                ProductMetricResolverSnapshotMapper resolverSnapshotMapper) {
        this.productModelMapper = productModelMapper;
        this.riskMetricCatalogPublishRule = riskMetricCatalogPublishRule;
        this.riskMetricCatalogService = riskMetricCatalogService;
        this.resolverSnapshotMapper = resolverSnapshotMapper;
    }

    @EventListener
    public void onProductContractReleased(ProductContractReleasedEvent event) {
        if (event == null || event.productId() == null || event.releaseBatchId() == null) {
            return;
        }
        Set<String> releasedIdentifiers = normalizeIdentifiers(event.releasedIdentifiers());
        if (releasedIdentifiers.isEmpty()) {
            return;
        }
        List<ProductModel> releasedContracts = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, event.productId())
                .eq(ProductModel::getModelType, "property")
                .in(ProductModel::getIdentifier, releasedIdentifiers));
        if (releasedContracts == null || releasedContracts.isEmpty()) {
            return;
        }
        ResolverSnapshotPayload snapshotPayload = buildResolverSnapshotPayload(event, releasedContracts);
        persistResolverSnapshot(event, snapshotPayload);
        List<ProductModel> canonicalContracts = canonicalizeContractsForPublishRule(releasedContracts, snapshotPayload.snapshot());
        Set<String> riskEnabledIdentifiers = riskMetricCatalogPublishRule == null
                ? Set.of()
                : riskMetricCatalogPublishRule.resolveRiskEnabledIdentifiers(null, canonicalContracts);
        riskMetricCatalogService.publishFromReleasedContracts(
                event.productId(),
                event.releaseBatchId(),
                releasedContracts,
                riskEnabledIdentifiers
        );
    }

    private Set<String> normalizeIdentifiers(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            if (!StringUtils.hasText(identifier)) {
                continue;
            }
            normalized.add(identifier.trim());
        }
        return normalized;
    }

    private ResolverSnapshotPayload buildResolverSnapshotPayload(ProductContractReleasedEvent event,
                                                                List<ProductModel> releasedContracts) {
        PublishedProductContractSnapshot.Builder builder = PublishedProductContractSnapshot.builder()
                .productId(event.productId())
                .releaseBatchId(event.releaseBatchId());
        Set<String> publishedIdentifiers = new LinkedHashSet<>();
        Map<String, String> canonicalAliases = new LinkedHashMap<>();
        for (ProductModel contract : releasedContracts) {
            String rawIdentifier = normalize(contract == null ? null : contract.getIdentifier());
            String canonicalIdentifier = toCanonicalIdentifier(rawIdentifier);
            if (!StringUtils.hasText(canonicalIdentifier)) {
                continue;
            }
            publishedIdentifiers.add(canonicalIdentifier);
            canonicalAliases.put(rawIdentifier, canonicalIdentifier);
            canonicalAliases.put(canonicalIdentifier, canonicalIdentifier);
            builder.publishedIdentifier(canonicalIdentifier);
            builder.canonicalAlias(rawIdentifier, canonicalIdentifier);
            builder.canonicalAlias(canonicalIdentifier, canonicalIdentifier);
        }
        return new ResolverSnapshotPayload(
                builder.build(),
                writeSnapshotJson(publishedIdentifiers, canonicalAliases)
        );
    }

    private void persistResolverSnapshot(ProductContractReleasedEvent event, ResolverSnapshotPayload payload) {
        if (resolverSnapshotMapper == null || event == null || payload == null || !StringUtils.hasText(payload.snapshotJson())) {
            return;
        }
        List<ProductMetricResolverSnapshot> existingRows = resolverSnapshotMapper.selectList(
                new LambdaQueryWrapper<ProductMetricResolverSnapshot>()
                        .eq(ProductMetricResolverSnapshot::getProductId, event.productId())
                        .eq(ProductMetricResolverSnapshot::getReleaseBatchId, event.releaseBatchId())
                        .eq(ProductMetricResolverSnapshot::getDeleted, 0)
                        .orderByDesc(ProductMetricResolverSnapshot::getCreateTime)
                        .orderByDesc(ProductMetricResolverSnapshot::getId)
                        .last("limit 1")
        );
        ProductMetricResolverSnapshot row = existingRows == null || existingRows.isEmpty() ? null : existingRows.get(0);
        if (row == null) {
            row = new ProductMetricResolverSnapshot();
            row.setTenantId(event.tenantId());
            row.setProductId(event.productId());
            row.setReleaseBatchId(event.releaseBatchId());
            row.setCreateBy(event.operatorUserId());
            row.setSnapshotJson(payload.snapshotJson());
            resolverSnapshotMapper.insert(row);
            return;
        }
        row.setSnapshotJson(payload.snapshotJson());
        resolverSnapshotMapper.updateById(row);
    }

    private List<ProductModel> canonicalizeContractsForPublishRule(List<ProductModel> releasedContracts,
                                                                   PublishedProductContractSnapshot snapshot) {
        if (releasedContracts == null || releasedContracts.isEmpty() || snapshot == null) {
            return releasedContracts == null ? List.of() : releasedContracts;
        }
        List<ProductModel> canonicalized = new ArrayList<>();
        for (ProductModel contract : releasedContracts) {
            if (contract == null) {
                continue;
            }
            ProductModel copy = new ProductModel();
            BeanUtils.copyProperties(contract, copy);
            copy.setIdentifier(snapshot.canonicalAliasOf(contract.getIdentifier()).orElse(contract.getIdentifier()));
            canonicalized.add(copy);
        }
        return canonicalized;
    }

    private String writeSnapshotJson(Set<String> publishedIdentifiers, Map<String, String> canonicalAliases) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("publishedIdentifiers", publishedIdentifiers == null ? List.of() : List.copyOf(publishedIdentifiers));
            payload.put("canonicalAliases", canonicalAliases == null ? Map.of() : canonicalAliases);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toCanonicalIdentifier(String identifier) {
        String normalized = normalize(identifier);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        int idx = normalized.lastIndexOf('.');
        if (idx >= 0 && idx < normalized.length() - 1) {
            return normalized.substring(idx + 1);
        }
        return normalized;
    }

    private String normalize(String identifier) {
        return StringUtils.hasText(identifier) ? identifier.trim() : null;
    }

    private record ResolverSnapshotPayload(PublishedProductContractSnapshot snapshot, String snapshotJson) {
    }
}
