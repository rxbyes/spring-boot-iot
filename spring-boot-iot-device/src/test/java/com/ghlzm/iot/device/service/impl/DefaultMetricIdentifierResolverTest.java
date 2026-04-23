package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultMetricIdentifierResolverTest {

    private final DefaultMetricIdentifierResolver resolver = new DefaultMetricIdentifierResolver();

    @Test
    void shouldPreferPublishedCanonicalIdentifierOverCaseInsensitiveGuessing() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .canonicalAlias("l1_lf_1.value", "value")
                .canonicalAlias("VALUE", "value")
                .build();

        MetricIdentifierResolution resolution = resolver.resolveForRead(snapshot, "VALUE");

        assertEquals("value", resolution.canonicalIdentifier());
        assertEquals(MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT, resolution.source());
    }

    @Test
    void shouldKeepMixedCaseCanonicalIdentifierWhenAliasMatchesCaseInsensitively() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .canonicalAlias("L1_GNSS_1.gpsTotalX", "gpsTotalX")
                .build();

        MetricIdentifierResolution resolution = resolver.resolveForRead(snapshot, "l1_gnss_1.GPSTOTALX");

        assertEquals("gpsTotalX", resolution.canonicalIdentifier());
        assertEquals(MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT, resolution.source());
    }

    @Test
    void shouldPreferFullPathPublishedIdentifierDuringGovernanceNormalization() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("L1_GNSS_1.gpsTotalX")
                .canonicalAlias("L1_GNSS_1.gpsTotalX", "gpsTotalX")
                .canonicalAlias("gpsTotalX", "gpsTotalX")
                .build();

        MetricIdentifierResolution resolution = resolver.resolveForGovernance(snapshot, "l1_gnss_1.GPSTOTALX");

        assertEquals("L1_GNSS_1.gpsTotalX", resolution.canonicalIdentifier());
        assertEquals(MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT, resolution.source());
    }

    @Test
    void shouldPreserveRawIdentifierCaseWhenSnapshotDoesNotMatch() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .build();

        MetricIdentifierResolution resolution = resolver.resolveForRuntime(snapshot, " RawMetricX ");

        assertEquals("RawMetricX", resolution.canonicalIdentifier());
        assertEquals(MetricIdentifierResolution.SOURCE_RAW_IDENTIFIER, resolution.source());
    }
}
