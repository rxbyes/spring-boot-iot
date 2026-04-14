package com.ghlzm.iot.device.service.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 已发布产品合同快照（读侧聚合视图）。
 */
public final class PublishedProductContractSnapshot {

    private final Long productId;
    private final Long releaseBatchId;
    private final Set<String> publishedIdentifiers;
    private final Set<String> normalizedPublishedIdentifiers;
    private final Map<String, String> canonicalAliases;

    private PublishedProductContractSnapshot(Builder builder) {
        this.productId = builder.productId;
        this.releaseBatchId = builder.releaseBatchId;
        this.publishedIdentifiers = Collections.unmodifiableSet(new LinkedHashSet<>(builder.publishedIdentifiersByNormalized.values()));
        this.normalizedPublishedIdentifiers =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.publishedIdentifiersByNormalized.keySet()));
        this.canonicalAliases = Collections.unmodifiableMap(new LinkedHashMap<>(builder.canonicalAliasesByNormalized));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PublishedProductContractSnapshot empty(Long productId) {
        return builder().productId(productId).build();
    }

    public Long productId() {
        return productId;
    }

    public Long releaseBatchId() {
        return releaseBatchId;
    }

    public Set<String> publishedIdentifiers() {
        return publishedIdentifiers;
    }

    public void forEachCanonicalAlias(BiConsumer<String, String> consumer) {
        if (consumer == null) {
            return;
        }
        canonicalAliases.forEach(consumer);
    }

    public Optional<String> canonicalAliasOf(String identifier) {
        String normalized = normalize(identifier);
        if (normalized == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(canonicalAliases.get(normalized));
    }

    public boolean containsPublishedIdentifier(String identifier) {
        String normalized = normalize(identifier);
        return normalized != null && normalizedPublishedIdentifiers.contains(normalized);
    }

    public static final class Builder {

        private Long productId;
        private Long releaseBatchId;
        private final Map<String, String> publishedIdentifiersByNormalized = new LinkedHashMap<>();
        private final Map<String, String> canonicalAliasesByNormalized = new LinkedHashMap<>();

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder releaseBatchId(Long releaseBatchId) {
            this.releaseBatchId = releaseBatchId;
            return this;
        }

        public Builder publishedIdentifier(String identifier) {
            String canonicalIdentifier = sanitizeCanonicalIdentifier(identifier);
            String normalized = normalize(canonicalIdentifier);
            if (canonicalIdentifier != null && normalized != null) {
                this.publishedIdentifiersByNormalized.putIfAbsent(normalized, canonicalIdentifier);
                this.canonicalAliasesByNormalized.putIfAbsent(normalized, canonicalIdentifier);
            }
            return this;
        }

        public Builder publishedIdentifiers(Collection<String> identifiers) {
            if (identifiers == null) {
                return this;
            }
            for (String identifier : identifiers) {
                publishedIdentifier(identifier);
            }
            return this;
        }

        public Builder canonicalAlias(String alias, String canonicalIdentifier) {
            String normalizedAlias = normalize(alias);
            String normalizedCanonical = normalize(canonicalIdentifier);
            String canonical = sanitizeCanonicalIdentifier(canonicalIdentifier);
            if (normalizedAlias == null || normalizedCanonical == null || canonical == null) {
                return this;
            }
            String publishedCanonical = publishedIdentifiersByNormalized.getOrDefault(normalizedCanonical, canonical);
            this.canonicalAliasesByNormalized.put(normalizedAlias, publishedCanonical);
            return this;
        }

        public PublishedProductContractSnapshot build() {
            return new PublishedProductContractSnapshot(this);
        }
    }

    private static String normalize(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase();
    }

    private static String sanitizeCanonicalIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
