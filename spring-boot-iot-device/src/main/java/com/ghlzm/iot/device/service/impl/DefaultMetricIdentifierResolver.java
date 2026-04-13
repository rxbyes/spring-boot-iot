package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.springframework.stereotype.Component;

/**
 * 默认指标标识符解析器。
 */
@Component
public class DefaultMetricIdentifierResolver implements MetricIdentifierResolver {

    @Override
    public MetricIdentifierResolution resolveForRead(PublishedProductContractSnapshot snapshot, String metricIdentifier) {
        return resolve(snapshot, metricIdentifier);
    }

    @Override
    public MetricIdentifierResolution resolveForRuntime(PublishedProductContractSnapshot snapshot, String metricIdentifier) {
        return resolve(snapshot, metricIdentifier);
    }

    @Override
    public MetricIdentifierResolution resolveForGovernance(PublishedProductContractSnapshot snapshot, String metricIdentifier) {
        return resolve(snapshot, metricIdentifier);
    }

    private MetricIdentifierResolution resolve(PublishedProductContractSnapshot snapshot, String metricIdentifier) {
        String sanitizedInput = sanitize(metricIdentifier);
        if (sanitizedInput == null) {
            return MetricIdentifierResolution.of(metricIdentifier, null, MetricIdentifierResolution.SOURCE_RAW_IDENTIFIER);
        }
        PublishedProductContractSnapshot safeSnapshot = snapshot == null
                ? PublishedProductContractSnapshot.empty(null)
                : snapshot;

        String published = safeSnapshot.canonicalAliasOf(metricIdentifier).orElse(null);
        if (published != null) {
            return MetricIdentifierResolution.of(metricIdentifier, published, MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT);
        }

        for (String publishedIdentifier : safeSnapshot.publishedIdentifiers()) {
            if (publishedIdentifier != null && metricIdentifier != null && publishedIdentifier.equalsIgnoreCase(metricIdentifier)) {
                return MetricIdentifierResolution.of(metricIdentifier, publishedIdentifier,
                        MetricIdentifierResolution.SOURCE_CASE_INSENSITIVE_GUESS);
            }
        }
        return MetricIdentifierResolution.of(metricIdentifier, sanitizedInput, MetricIdentifierResolution.SOURCE_RAW_IDENTIFIER);
    }

    private String sanitize(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
