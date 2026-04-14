package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductMetricResolverSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMetricResolverSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 已发布产品合同快照服务实现。
 */
@Service
public class PublishedProductContractSnapshotServiceImpl implements PublishedProductContractSnapshotService {

    private final ProductModelMapper productModelMapper;
    private final ProductContractReleaseBatchMapper releaseBatchMapper;
    private final ProductMetricResolverSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ConcurrentMap<Long, CachedSnapshot> latestSnapshotCache = new ConcurrentHashMap<>();

    public PublishedProductContractSnapshotServiceImpl(ProductModelMapper productModelMapper,
                                                       ProductContractReleaseBatchMapper releaseBatchMapper) {
        this(productModelMapper, releaseBatchMapper, null);
    }

    @Autowired
    public PublishedProductContractSnapshotServiceImpl(ProductModelMapper productModelMapper,
                                                       ProductContractReleaseBatchMapper releaseBatchMapper,
                                                       @Nullable ProductMetricResolverSnapshotMapper snapshotMapper) {
        this.productModelMapper = productModelMapper;
        this.releaseBatchMapper = releaseBatchMapper;
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public PublishedProductContractSnapshot getRequiredSnapshot(Long productId) {
        if (productId == null) {
            return PublishedProductContractSnapshot.empty(null);
        }
        Long latestReleasedBatchId = resolveLatestReleasedBatchId(productId);
        Map<String, String> currentFormalByLower = loadCurrentFormalIdentifierMap(productId);
        String currentFormalSignature = buildCurrentFormalSignature(currentFormalByLower);
        CachedSnapshot cachedSnapshot = latestSnapshotCache.get(productId);
        if (cachedSnapshot != null
                && Objects.equals(cachedSnapshot.releaseBatchId(), latestReleasedBatchId)
                && Objects.equals(cachedSnapshot.currentFormalSignature(), currentFormalSignature)) {
            return cachedSnapshot.snapshot();
        }
        PersistedSnapshotData persistedSnapshot = loadPersistedSnapshot(productId, latestReleasedBatchId);
        PublishedProductContractSnapshot snapshot =
                mergeSnapshotWithCurrentFormal(productId, latestReleasedBatchId, currentFormalByLower, persistedSnapshot);
        cacheSnapshot(productId, latestReleasedBatchId, currentFormalSignature, snapshot);
        return snapshot;
    }

    private PersistedSnapshotData loadPersistedSnapshot(Long productId, Long releaseBatchId) {
        if (snapshotMapper == null || productId == null || releaseBatchId == null) {
            return null;
        }
        List<ProductMetricResolverSnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<ProductMetricResolverSnapshot>()
                        .eq(ProductMetricResolverSnapshot::getProductId, productId)
                        .eq(ProductMetricResolverSnapshot::getReleaseBatchId, releaseBatchId)
                        .eq(ProductMetricResolverSnapshot::getDeleted, 0)
                        .orderByDesc(ProductMetricResolverSnapshot::getCreateTime)
                        .orderByDesc(ProductMetricResolverSnapshot::getId)
                        .last("limit 1")
        );
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }
        return parseSnapshot(productId, releaseBatchId, snapshots.get(0).getSnapshotJson());
    }

    private Long resolveLatestReleasedBatchId(Long productId) {
        if (releaseBatchMapper == null || productId == null) {
            return null;
        }
        List<ProductContractReleaseBatch> batches = releaseBatchMapper.selectList(
                new LambdaQueryWrapper<ProductContractReleaseBatch>()
                        .eq(ProductContractReleaseBatch::getProductId, productId)
                        .eq(ProductContractReleaseBatch::getReleaseStatus, ProductContractReleaseServiceImpl.RELEASE_STATUS_RELEASED)
                        .isNull(ProductContractReleaseBatch::getRollbackTime)
                        .orderByDesc(ProductContractReleaseBatch::getCreateTime)
                        .orderByDesc(ProductContractReleaseBatch::getId)
                        .last("limit 1")
        );
        if (batches == null || batches.isEmpty()) {
            return null;
        }
        return batches.get(0).getId();
    }

    private void cacheSnapshot(Long productId,
                               Long releaseBatchId,
                               String currentFormalSignature,
                               PublishedProductContractSnapshot snapshot) {
        if (productId == null) {
            return;
        }
        if (releaseBatchId == null || snapshot == null) {
            latestSnapshotCache.remove(productId);
            return;
        }
        latestSnapshotCache.put(productId, new CachedSnapshot(releaseBatchId, currentFormalSignature, snapshot));
    }

    private PersistedSnapshotData parseSnapshot(Long productId, Long releaseBatchId, String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            PublishedProductContractSnapshot.Builder builder = PublishedProductContractSnapshot.builder()
                    .productId(productId)
                    .releaseBatchId(releaseBatchId);
            Map<String, String> canonicalAliases = new LinkedHashMap<>();
            JsonNode publishedIdentifiersNode = root.path("publishedIdentifiers");
            if (publishedIdentifiersNode.isArray()) {
                for (JsonNode identifierNode : publishedIdentifiersNode) {
                    if (identifierNode != null && identifierNode.isTextual()) {
                        builder.publishedIdentifier(identifierNode.asText());
                    }
                }
            }
            JsonNode canonicalAliasesNode = root.path("canonicalAliases");
            if (canonicalAliasesNode.isObject()) {
                canonicalAliasesNode.fields().forEachRemaining(entry -> {
                    if (entry != null && entry.getValue() != null && entry.getValue().isTextual()) {
                        canonicalAliases.put(entry.getKey(), entry.getValue().asText());
                        builder.canonicalAlias(entry.getKey(), entry.getValue().asText());
                    }
                });
            }
            return new PersistedSnapshotData(builder.build(), canonicalAliases);
        } catch (Exception ex) {
            return null;
        }
    }

    private PublishedProductContractSnapshot mergeSnapshotWithCurrentFormal(Long productId,
                                                                            Long releaseBatchId,
                                                                            Map<String, String> currentFormalByLower,
                                                                            PersistedSnapshotData persistedSnapshot) {
        if (currentFormalByLower.isEmpty()) {
            return PublishedProductContractSnapshot.builder()
                    .productId(productId)
                    .releaseBatchId(releaseBatchId)
                    .build();
        }
        PublishedProductContractSnapshot.Builder builder = PublishedProductContractSnapshot.builder()
                .productId(productId)
                .releaseBatchId(releaseBatchId)
                .publishedIdentifiers(currentFormalByLower.values());
        currentFormalByLower.values().forEach(identifier -> builder.canonicalAlias(identifier, identifier));
        if (persistedSnapshot != null) {
            persistedSnapshot.canonicalAliases().forEach((alias, target) -> {
                String normalizedTarget = normalizeIdentifier(target);
                if (!StringUtils.hasText(normalizedTarget)) {
                    return;
                }
                String currentFormalIdentifier = currentFormalByLower.get(normalizedTarget);
                if (StringUtils.hasText(currentFormalIdentifier)) {
                    builder.canonicalAlias(alias, currentFormalIdentifier);
                }
            });
        }
        return builder.build();
    }

    private String buildCurrentFormalSignature(Map<String, String> currentFormalByLower) {
        if (currentFormalByLower == null || currentFormalByLower.isEmpty()) {
            return "";
        }
        return String.join("|", currentFormalByLower.values());
    }

    private Map<String, String> loadCurrentFormalIdentifierMap(Long productId) {
        if (productId == null) {
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
        Map<String, String> currentFormalByLower = new LinkedHashMap<>();
        for (ProductModel productModel : productModels) {
            String canonicalIdentifier = toCanonicalIdentifier(productModel.getIdentifier());
            if (!StringUtils.hasText(canonicalIdentifier)) {
                continue;
            }
            currentFormalByLower.putIfAbsent(canonicalIdentifier.toLowerCase(Locale.ROOT), canonicalIdentifier);
        }
        return currentFormalByLower;
    }

    private String toCanonicalIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        return identifier.trim().toLowerCase(Locale.ROOT);
    }

    private record PersistedSnapshotData(PublishedProductContractSnapshot snapshot,
                                         Map<String, String> canonicalAliases) {
    }

    private record CachedSnapshot(Long releaseBatchId,
                                  String currentFormalSignature,
                                  PublishedProductContractSnapshot snapshot) {
    }
}
