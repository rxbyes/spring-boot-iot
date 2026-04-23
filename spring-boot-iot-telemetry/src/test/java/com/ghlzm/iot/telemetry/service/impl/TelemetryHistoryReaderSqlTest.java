package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryHistoryReaderSqlTest {

    @Test
    void rawHistorySqlShouldUseTdengineCompatibleNullPredicate() throws Exception {
        TelemetryRawHistoryReader reader = new TelemetryRawHistoryReader(null, null, null);

        String sql = invokeRawBuildSelectSql(
                reader,
                "tb_m_1_1922974195260456962",
                List.of("L4_NW_1"),
                LocalDateTime.of(2026, 4, 11, 21, 0),
                LocalDateTime.of(2026, 4, 12, 21, 0),
                10_000
        );

        assertTrue(sql.contains("ingested_at >= ? AND ingested_at < ?"));
        assertTrue(sql.contains("ingested_at IS NULL AND ts >= ? AND ts < ?"));
        assertFalse(sql.contains("COALESCE("));
    }

    @Test
    void normalizedHistorySqlShouldUseTdengineCompatibleNullPredicate() throws Exception {
        NormalizedTelemetryHistoryReader reader = new NormalizedTelemetryHistoryReader(null, null);

        String sql = invokeNormalizedBuildSelectSql(
                reader,
                LocalDateTime.of(2026, 4, 11, 21, 0),
                LocalDateTime.of(2026, 4, 12, 21, 0),
                10_000
        );

        assertTrue(sql.contains("reported_at >= ? AND reported_at < ?"));
        assertTrue(sql.contains("reported_at IS NULL AND ts >= ? AND ts < ?"));
        assertFalse(sql.contains("COALESCE("));
    }

    @Test
    void normalizedHistoryShouldResolveRawMetricCodeToPublishedCanonicalIdentifier() throws Exception {
        PublishedProductContractSnapshotService snapshotService = mock(PublishedProductContractSnapshotService.class);
        MetricIdentifierResolver metricIdentifierResolver = mock(MetricIdentifierResolver.class);
        NormalizedTelemetryHistoryReader reader =
                new NormalizedTelemetryHistoryReader(null, null, snapshotService, metricIdentifierResolver);
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .canonicalAlias("L1_LF_1.value", "value")
                .build();
        Product product = new Product();
        product.setId(1001L);
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(snapshot);
        when(metricIdentifierResolver.resolveForRead(snapshot, "L1_LF_1.value"))
                .thenReturn(MetricIdentifierResolution.of(
                        "L1_LF_1.value",
                        "value",
                        MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT
                ));

        String canonicalMetricCode = invokeNormalizedResolveMetricCode(reader, product, "L1_LF_1.value");

        assertEquals("value", canonicalMetricCode);
    }

    private String invokeRawBuildSelectSql(TelemetryRawHistoryReader reader,
                                           String childTable,
                                           List<String> identifiers,
                                           LocalDateTime windowStart,
                                           LocalDateTime windowEnd,
                                           int batchSize) throws Exception {
        Method method = TelemetryRawHistoryReader.class.getDeclaredMethod(
                "buildSelectSql",
                String.class,
                List.class,
                LocalDateTime.class,
                LocalDateTime.class,
                int.class
        );
        method.setAccessible(true);
        return (String) method.invoke(reader, childTable, identifiers, windowStart, windowEnd, batchSize);
    }

    private String invokeNormalizedBuildSelectSql(NormalizedTelemetryHistoryReader reader,
                                                  LocalDateTime windowStart,
                                                  LocalDateTime windowEnd,
                                                  int batchSize) throws Exception {
        Method method = NormalizedTelemetryHistoryReader.class.getDeclaredMethod(
                "buildSelectSql",
                LocalDateTime.class,
                LocalDateTime.class,
                int.class
        );
        method.setAccessible(true);
        return (String) method.invoke(reader, windowStart, windowEnd, batchSize);
    }

    private String invokeNormalizedResolveMetricCode(NormalizedTelemetryHistoryReader reader,
                                                     Product product,
                                                     String metricCode) throws Exception {
        Method method = NormalizedTelemetryHistoryReader.class.getDeclaredMethod(
                "resolveMetricCode",
                Product.class,
                String.class
        );
        method.setAccessible(true);
        return (String) method.invoke(reader, product, metricCode);
    }
}
