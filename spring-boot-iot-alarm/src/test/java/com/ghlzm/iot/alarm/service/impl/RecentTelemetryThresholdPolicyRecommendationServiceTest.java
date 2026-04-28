package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.service.ThresholdPolicyRecommendationService;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetryJdbcTemplateProvider;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetrySchemaSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecentTelemetryThresholdPolicyRecommendationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void recommendShouldUseFifteenDayStatsWhenSamplesAreEnough() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(6L, 1.0, 10.0, 4.0),
                stats(0L, null, null, null)
        );
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate);
        Product product = monitoringProduct();

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(product, "value", Set.of(8001L, 8002L));

        assertEquals(15, recommendation.windowDays());
        assertEquals(6L, recommendation.sampleCount());
        assertEquals("SUGGESTED", recommendation.status());
        assertEquals("UPPER_ONLY", recommendation.direction());
        assertEquals("value >= 12", recommendation.recommendedExpression());
        assertEquals(2, jdbcTemplate.sqls.size());
        assertTrue(jdbcTemplate.sqls.get(0).contains("value_double"));
        assertTrue(jdbcTemplate.sqls.get(1).contains("value_long"));
    }

    @Test
    void recommendShouldFallbackToThirtyDaysWhenFifteenDaySamplesAreInsufficient() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(2L, 1.0, 3.0, 2.0),
                stats(0L, null, null, null),
                stats(5L, 1.0, 6.0, 3.0),
                stats(0L, null, null, null)
        );
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate);

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(monitoringProduct(), "value", Set.of(8001L));

        assertEquals(30, recommendation.windowDays());
        assertEquals(5L, recommendation.sampleCount());
        assertEquals("value >= 7.2", recommendation.recommendedExpression());
        assertEquals(4, jdbcTemplate.sqls.size());
    }

    @Test
    void recommendShouldMatchLeafIdentifierWhenMetricUsesFullPath() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(8L, 1.0, 10.0, 4.0),
                stats(0L, null, null, null)
        );
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate);

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(monitoringProduct(), "L1_GP_1.gpsTotalX", Set.of(8001L));

        assertEquals("value >= 12", recommendation.recommendedExpression());
        assertTrue(jdbcTemplate.sqls.get(0).contains("metric_code IN (?, ?)"));
        assertTrue(jdbcTemplate.argsList.get(0).contains("L1_GP_1.gpsTotalX"));
        assertTrue(jdbcTemplate.argsList.get(0).contains("gpsTotalX"));
        assertTrue(jdbcTemplate.argsList.get(0).contains("%.gpsTotalX"));
    }

    @Test
    void recommendShouldUseLatestAvailableSamplesWhenRecentWindowIsEmpty() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(0L, null, null, null),
                stats(0L, null, null, null),
                stats(0L, null, null, null),
                stats(0L, null, null, null)
        );
        jdbcTemplate.latestRows.addAll(List.of(
                valueRow(1.0),
                valueRow(2.0),
                valueRow(3.0),
                valueRow(4.0),
                valueRow(10.0)
        ));
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate);

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(monitoringProduct(), "value", Set.of(8001L));

        assertEquals("STALE_SUGGESTED", recommendation.status());
        assertEquals(5L, recommendation.sampleCount());
        assertEquals("value >= 12", recommendation.recommendedExpression());
        assertTrue(recommendation.reason().contains("latest available telemetry"));
    }

    @Test
    void recommendShouldUseLatestPropertySnapshotWhenTelemetryHasNoNumericSample() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(0L, null, null, null),
                stats(0L, null, null, null),
                stats(0L, null, null, null),
                stats(0L, null, null, null)
        );
        DevicePropertyMapper propertyMapper = mock(DevicePropertyMapper.class);
        when(propertyMapper.selectList(any())).thenReturn(List.of(
                property("L3_DB_1.dispsX", "6.5"),
                property("dispsX", "8")
        ));
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate, propertyMapper);

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(monitoringProduct(), "dispsX", Set.of(8001L, 8002L));

        assertEquals("LATEST_PROPERTY_SUGGESTED", recommendation.status());
        assertEquals(2L, recommendation.sampleCount());
        assertEquals("value >= 9.6", recommendation.recommendedExpression());
        assertTrue(recommendation.reason().contains("latest property snapshot"));
    }

    @Test
    void recommendShouldKeepManualReviewForFlatZeroSamples() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(12L, 0.0, 0.0, 0.0),
                stats(0L, null, null, null)
        );
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate);

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(monitoringProduct(), "value", Set.of(8001L));

        assertEquals("FLAT_ZERO_REVIEW", recommendation.status());
        assertEquals(null, recommendation.recommendedExpression());
    }

    @Test
    void recommendShouldUseConfirmedTemplateWhenFlatZeroSamplesNeedBusinessThreshold() throws Exception {
        Path configPath = tempDir.resolve("threshold-policy-defaults.confirmed.json");
        Files.writeString(configPath, """
                {
                  "productTypeTemplates": [
                    {
                      "productType": "MONITORING",
                      "metricIdentifier": "value",
                      "metricAliases": ["L4_NW_1", "value"],
                      "semanticTemplateKey": "mud-level:value",
                      "match": {"productKeys": ["monitoring-mud-level-v1"], "rawIdentifiers": ["L4_NW_1", "value"]},
                      "expression": ">= 1.5",
                      "confirmationStatus": "CONFIRMED"
                    }
                  ]
                }
                """);
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate(
                stats(12L, 0.0, 0.0, 0.0),
                stats(0L, null, null, null)
        );
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate, null, configPath.toString());
        Product product = monitoringProduct();
        product.setProductKey("monitoring-mud-level-v1");

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(product, "L4_NW_1", Set.of(8001L));

        assertEquals("TEMPLATE_SUGGESTED", recommendation.status());
        assertEquals("value >= 1.5", recommendation.recommendedExpression());
        assertEquals("UPPER_ONLY", recommendation.direction());
        assertTrue(recommendation.reason().contains("mud-level:value"));
    }

    @Test
    void recommendShouldSkipNonMonitoringProducts() {
        StubJdbcTemplate jdbcTemplate = new StubJdbcTemplate();
        RecentTelemetryThresholdPolicyRecommendationService service = service(jdbcTemplate);
        Product product = new Product();
        product.setId(1002L);
        product.setProductKey("camera-v1");
        product.setProductName("Camera");

        ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation recommendation =
                service.recommend(product, "value", Set.of(8001L));

        assertEquals("UNSUPPORTED_PRODUCT_TYPE", recommendation.status());
        assertTrue(jdbcTemplate.sqls.isEmpty());
    }

    private RecentTelemetryThresholdPolicyRecommendationService service(StubJdbcTemplate jdbcTemplate) {
        return service(jdbcTemplate, null);
    }

    private RecentTelemetryThresholdPolicyRecommendationService service(StubJdbcTemplate jdbcTemplate,
                                                                        DevicePropertyMapper propertyMapper) {
        return service(jdbcTemplate, propertyMapper, tempDir.resolve("missing-template.json").toString());
    }

    private RecentTelemetryThresholdPolicyRecommendationService service(StubJdbcTemplate jdbcTemplate,
                                                                        DevicePropertyMapper propertyMapper,
                                                                        String templateConfigPath) {
        TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider = mock(TdengineTelemetryJdbcTemplateProvider.class);
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        TdengineTelemetrySchemaSupport schemaSupport = mock(TdengineTelemetrySchemaSupport.class);
        ObjectProvider<TdengineTelemetryJdbcTemplateProvider> jdbcProvider = mock(ObjectProvider.class);
        ObjectProvider<TdengineTelemetrySchemaSupport> schemaProvider = mock(ObjectProvider.class);
        ObjectProvider<DevicePropertyMapper> propertyMapperProvider = mock(ObjectProvider.class);
        when(jdbcProvider.getIfAvailable()).thenReturn(jdbcTemplateProvider);
        when(schemaProvider.getIfAvailable()).thenReturn(schemaSupport);
        when(propertyMapperProvider.getIfAvailable()).thenReturn(propertyMapper);
        return new RecentTelemetryThresholdPolicyRecommendationService(
                jdbcProvider,
                schemaProvider,
                propertyMapperProvider,
                templateConfigPath
        );
    }

    private Product monitoringProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("monitoring-crack-v1");
        product.setProductName("Monitoring Crack");
        return product;
    }

    private static Map<String, Object> stats(Long count, Double min, Double max, Double avg) {
        Map<String, Object> row = new HashMap<>();
        row.put("sample_count", count);
        row.put("min_value", min);
        row.put("max_value", max);
        row.put("avg_value", avg);
        return row;
    }

    private static Map<String, Object> valueRow(Double value) {
        Map<String, Object> row = new HashMap<>();
        row.put("metric_value", value);
        return row;
    }

    private static DeviceProperty property(String identifier, String value) {
        DeviceProperty property = new DeviceProperty();
        property.setIdentifier(identifier);
        property.setPropertyValue(value);
        return property;
    }

    private static class StubJdbcTemplate extends JdbcTemplate {
        private final ArrayDeque<Map<String, Object>> rows = new ArrayDeque<>();
        private final List<String> sqls = new ArrayList<>();
        private final List<List<Object>> argsList = new ArrayList<>();
        private final List<Map<String, Object>> latestRows = new ArrayList<>();

        StubJdbcTemplate(Map<String, Object>... rows) {
            this.rows.addAll(List.of(rows));
        }

        @Override
        public Map<String, Object> queryForMap(String sql, Object... args) {
            sqls.add(sql);
            argsList.add(List.of(args));
            if (rows.isEmpty()) {
                return stats(0L, null, null, null);
            }
            return rows.removeFirst();
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            sqls.add(sql);
            argsList.add(List.of(args));
            return sql.contains("value_double") ? latestRows : List.of();
        }
    }
}
