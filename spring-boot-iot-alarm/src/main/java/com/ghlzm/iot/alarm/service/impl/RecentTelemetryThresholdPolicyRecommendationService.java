package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.alarm.service.ThresholdPolicyRecommendationService;
import com.ghlzm.iot.common.device.DeviceBindingCapabilitySupport;
import com.ghlzm.iot.common.device.DeviceBindingCapabilityType;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetryJdbcTemplateProvider;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetrySchemaSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecentTelemetryThresholdPolicyRecommendationService implements ThresholdPolicyRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecentTelemetryThresholdPolicyRecommendationService.class);
    private static final int PREFERRED_WINDOW_DAYS = 15;
    private static final int FALLBACK_WINDOW_DAYS = 30;
    private static final long MIN_SAMPLE_COUNT = 5L;
    private static final int DEVICE_CHUNK_SIZE = 200;
    private static final int LATEST_SAMPLE_LIMIT = 500;
    private static final String DEFAULT_TEMPLATE_CONFIG_PATH = "config/automation/threshold-policy-defaults.confirmed.json";

    private final ObjectProvider<TdengineTelemetryJdbcTemplateProvider> jdbcTemplateProvider;
    private final ObjectProvider<TdengineTelemetrySchemaSupport> schemaSupportProvider;
    private final ObjectProvider<DevicePropertyMapper> devicePropertyMapperProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String templateConfigPath;

    public RecentTelemetryThresholdPolicyRecommendationService(
            ObjectProvider<TdengineTelemetryJdbcTemplateProvider> jdbcTemplateProvider,
            ObjectProvider<TdengineTelemetrySchemaSupport> schemaSupportProvider,
            ObjectProvider<DevicePropertyMapper> devicePropertyMapperProvider,
            @Value("${iot.threshold.policy-template.path:" + DEFAULT_TEMPLATE_CONFIG_PATH + "}") String templateConfigPath) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaSupportProvider = schemaSupportProvider;
        this.devicePropertyMapperProvider = devicePropertyMapperProvider;
        this.templateConfigPath = StringUtils.hasText(templateConfigPath)
                ? templateConfigPath.trim()
                : DEFAULT_TEMPLATE_CONFIG_PATH;
    }

    @Override
    public ThresholdPolicyRecommendation recommend(Product product, String metricIdentifier, Set<Long> deviceIds) {
        if (product == null || !StringUtils.hasText(metricIdentifier) || deviceIds == null || deviceIds.isEmpty()) {
            return unavailable("missing product, metric or device scope");
        }
        DeviceBindingCapabilityType productType = DeviceBindingCapabilitySupport.resolve(
                product.getProductKey(),
                product.getProductName()
        );
        if (productType != DeviceBindingCapabilityType.MONITORING) {
            return new ThresholdPolicyRecommendation(
                    null,
                    0L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "UNSUPPORTED_PRODUCT_TYPE",
                    productType.name(),
                    "only monitoring products are eligible for automatic threshold recommendation"
            );
        }
        try {
            TdengineTelemetryJdbcTemplateProvider provider = jdbcTemplateProvider.getIfAvailable();
            TdengineTelemetrySchemaSupport schemaSupport = schemaSupportProvider.getIfAvailable();
            if (provider == null || schemaSupport == null) {
                return unavailable("tdengine telemetry reader is unavailable");
            }
            schemaSupport.ensureTable();
            JdbcTemplate jdbcTemplate = provider.getJdbcTemplate();
            List<String> metricCandidates = metricCandidates(metricIdentifier.trim());
            ValueStats stats = queryStats(jdbcTemplate, metricCandidates, deviceIds, PREFERRED_WINDOW_DAYS);
            if (stats.sampleCount() < MIN_SAMPLE_COUNT) {
                stats = queryStats(jdbcTemplate, metricCandidates, deviceIds, FALLBACK_WINDOW_DAYS);
            }
            if (stats.sampleCount() <= 0L) {
                ValueStats latestStats = queryLatestStats(jdbcTemplate, metricCandidates, deviceIds);
                if (latestStats.sampleCount() >= MIN_SAMPLE_COUNT) {
                    ThresholdPolicyRecommendation staleRecommendation = buildStaleRecommendation(latestStats);
                    return templateOrCalculated(product, metricCandidates, latestStats, staleRecommendation);
                }
                ValueStats snapshotStats = queryLatestPropertySnapshotStats(metricCandidates, deviceIds);
                if (snapshotStats.sampleCount() > 0L) {
                    ThresholdPolicyRecommendation snapshotRecommendation = buildPropertySnapshotRecommendation(snapshotStats);
                    return templateOrCalculated(product, metricCandidates, snapshotStats, snapshotRecommendation);
                }
            }
            ThresholdPolicyRecommendation recommendation = buildRecommendation(stats);
            return templateOrCalculated(product, metricCandidates, stats, recommendation);
        } catch (RuntimeException ex) {
            log.warn("Build threshold recommendation failed, productId={}, metricIdentifier={}, error={}",
                    product.getId(), metricIdentifier, ex.getMessage());
            return unavailable("telemetry query failed");
        }
    }

    private ThresholdPolicyRecommendation templateOrCalculated(Product product,
                                                               List<String> metricCandidates,
                                                               ValueStats stats,
                                                               ThresholdPolicyRecommendation calculated) {
        if (calculated == null || hasExpression(calculated)) {
            return calculated;
        }
        ThresholdPolicyRecommendation templateRecommendation = buildTemplateRecommendation(product, metricCandidates, stats);
        return templateRecommendation == null ? calculated : templateRecommendation;
    }

    private boolean hasExpression(ThresholdPolicyRecommendation recommendation) {
        return recommendation != null
                && (StringUtils.hasText(recommendation.recommendedExpression())
                || StringUtils.hasText(recommendation.recommendedLowerExpression())
                || StringUtils.hasText(recommendation.recommendedUpperExpression()));
    }

    private ThresholdPolicyRecommendation unavailable(String reason) {
        return new ThresholdPolicyRecommendation(
                null,
                0L,
                null,
                null,
                null,
                null,
                null,
                null,
                "UNAVAILABLE",
                "UNAVAILABLE",
                reason
        );
    }

    private ThresholdPolicyRecommendation buildStaleRecommendation(ValueStats stats) {
        ThresholdPolicyRecommendation recommendation = buildRecommendation(stats);
        if (!StringUtils.hasText(recommendation.recommendedExpression())
                && !StringUtils.hasText(recommendation.recommendedLowerExpression())
                && !StringUtils.hasText(recommendation.recommendedUpperExpression())) {
            return recommendation;
        }
        return new ThresholdPolicyRecommendation(
                FALLBACK_WINDOW_DAYS,
                recommendation.sampleCount(),
                recommendation.minValue(),
                recommendation.maxValue(),
                recommendation.avgValue(),
                recommendation.recommendedExpression(),
                recommendation.recommendedLowerExpression(),
                recommendation.recommendedUpperExpression(),
                "STALE_SUGGESTED",
                recommendation.direction(),
                "no numeric sample in recent 30 days; suggested from latest available telemetry and requires review"
        );
    }

    private ThresholdPolicyRecommendation buildPropertySnapshotRecommendation(ValueStats stats) {
        if (stats.sampleCount() <= 0L || stats.minValue() == null || stats.maxValue() == null) {
            return buildRecommendation(stats);
        }
        if (isZero(stats.minValue()) && isZero(stats.maxValue())) {
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    null,
                    null,
                    null,
                    "FLAT_ZERO_REVIEW",
                    "FLAT_ZERO",
                    "latest property snapshot values are all zero; business review is required"
            );
        }
        if (stats.minValue().signum() < 0 && stats.maxValue().signum() > 0) {
            String lower = "value <= " + format(stats.minValue().multiply(BigDecimal.valueOf(1.2)));
            String upper = "value >= " + format(stats.maxValue().multiply(BigDecimal.valueOf(1.2)));
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    null,
                    lower,
                    upper,
                    "REQUIRES_MANUAL_REVIEW",
                    "BIDIRECTIONAL_REVIEW",
                    "latest property snapshot includes negative and positive values"
            );
        }
        if (stats.maxValue().signum() <= 0) {
            String expression = "value <= " + format(stats.minValue().multiply(BigDecimal.valueOf(1.2)));
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    expression,
                    expression,
                    null,
                    "LATEST_PROPERTY_SUGGESTED",
                    "LOWER_ONLY",
                    "no numeric telemetry sample available; suggested from latest property snapshot and requires review"
            );
        }
        String expression = "value >= " + format(stats.maxValue().multiply(BigDecimal.valueOf(1.2)));
        return new ThresholdPolicyRecommendation(
                stats.windowDays(),
                stats.sampleCount(),
                stats.minValue(),
                stats.maxValue(),
                stats.avgValue(),
                expression,
                null,
                expression,
                "LATEST_PROPERTY_SUGGESTED",
                "UPPER_ONLY",
                "no numeric telemetry sample available; suggested from latest property snapshot and requires review"
        );
    }

    private ThresholdPolicyRecommendation buildRecommendation(ValueStats stats) {
        if (stats.sampleCount() <= 0L || stats.minValue() == null || stats.maxValue() == null) {
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    null,
                    null,
                    null,
                    "NO_NUMERIC_SAMPLE",
                    "NO_NUMERIC_SAMPLE",
                    "no numeric sample available in recent telemetry"
            );
        }
        if (stats.sampleCount() < MIN_SAMPLE_COUNT) {
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    null,
                    null,
                    null,
                    "INSUFFICIENT_SAMPLE",
                    "INSUFFICIENT_SAMPLE",
                    "numeric sample count is below 5"
            );
        }
        if (isZero(stats.minValue()) && isZero(stats.maxValue())) {
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    null,
                    null,
                    null,
                    "FLAT_ZERO_REVIEW",
                    "FLAT_ZERO",
                    "recent values are all zero; business review is required"
            );
        }
        if (stats.minValue().signum() < 0 && stats.maxValue().signum() > 0) {
            String lower = "value <= " + format(stats.minValue().multiply(BigDecimal.valueOf(1.2)));
            String upper = "value >= " + format(stats.maxValue().multiply(BigDecimal.valueOf(1.2)));
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    null,
                    lower,
                    upper,
                    "REQUIRES_MANUAL_REVIEW",
                    "BIDIRECTIONAL_REVIEW",
                    "recent samples include negative and positive values"
            );
        }
        if (stats.maxValue().signum() <= 0) {
            String expression = "value <= " + format(stats.minValue().multiply(BigDecimal.valueOf(1.2)));
            return new ThresholdPolicyRecommendation(
                    stats.windowDays(),
                    stats.sampleCount(),
                    stats.minValue(),
                    stats.maxValue(),
                    stats.avgValue(),
                    expression,
                    expression,
                    null,
                    "SUGGESTED",
                    "LOWER_ONLY",
                    "suggested from recent observed minimum with 20% review buffer"
            );
        }
        String expression = "value >= " + format(stats.maxValue().multiply(BigDecimal.valueOf(1.2)));
        return new ThresholdPolicyRecommendation(
                stats.windowDays(),
                stats.sampleCount(),
                stats.minValue(),
                stats.maxValue(),
                stats.avgValue(),
                expression,
                null,
                expression,
                "SUGGESTED",
                "UPPER_ONLY",
                "suggested from recent observed maximum with 20% review buffer"
        );
    }

    private ThresholdPolicyRecommendation buildTemplateRecommendation(Product product,
                                                                      List<String> metricCandidates,
                                                                      ValueStats stats) {
        ConfirmedThresholdTemplate template = resolveConfirmedTemplate(product, metricCandidates);
        if (template == null) {
            return null;
        }
        String expression = normalizeExpression(template.expression());
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        boolean lower = expression.contains("<");
        return new ThresholdPolicyRecommendation(
                stats == null ? null : stats.windowDays(),
                stats == null ? 0L : stats.sampleCount(),
                stats == null ? null : stats.minValue(),
                stats == null ? null : stats.maxValue(),
                stats == null ? null : stats.avgValue(),
                expression,
                lower ? expression : null,
                lower ? null : expression,
                "TEMPLATE_SUGGESTED",
                lower ? "LOWER_ONLY" : "UPPER_ONLY",
                "confirmed threshold template: " + template.semanticTemplateKey()
        );
    }

    private List<String> metricCandidates(String metricIdentifier) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (StringUtils.hasText(metricIdentifier)) {
            candidates.add(metricIdentifier.trim());
            int lastDotIndex = metricIdentifier.lastIndexOf('.');
            if (lastDotIndex >= 0 && lastDotIndex + 1 < metricIdentifier.length()) {
                candidates.add(metricIdentifier.substring(lastDotIndex + 1).trim());
            }
        }
        return candidates.stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private ConfirmedThresholdTemplate resolveConfirmedTemplate(Product product, List<String> metricCandidates) {
        if (product == null || metricCandidates == null || metricCandidates.isEmpty()) {
            return null;
        }
        Path path = Path.of(templateConfigPath);
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            return null;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
            Object rawTemplates = config.get("productTypeTemplates");
            if (!(rawTemplates instanceof List<?> templates)) {
                return null;
            }
            String productType = DeviceBindingCapabilitySupport.resolve(
                    product.getProductKey(),
                    product.getProductName()
            ).name();
            Set<String> candidateSet = metricCandidates.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (Object rawTemplate : templates) {
                if (!(rawTemplate instanceof Map<?, ?> template)) {
                    continue;
                }
                ConfirmedThresholdTemplate parsed = parseConfirmedTemplate(template);
                if (parsed == null) {
                    continue;
                }
                if (!productType.equalsIgnoreCase(parsed.productType())) {
                    continue;
                }
                if (!matchesProductKey(parsed, product)) {
                    continue;
                }
                if (parsed.metricAliases().stream().noneMatch(candidateSet::contains)) {
                    continue;
                }
                return parsed;
            }
        } catch (RuntimeException | java.io.IOException ex) {
            log.warn("Load threshold template config failed, path={}, error={}", path, ex.getMessage());
        }
        return null;
    }

    private ConfirmedThresholdTemplate parseConfirmedTemplate(Map<?, ?> template) {
        String confirmationStatus = stringValue(template.get("confirmationStatus"));
        if (!"CONFIRMED".equalsIgnoreCase(confirmationStatus)) {
            return null;
        }
        String expression = normalizeExpression(stringValue(template.get("expression")));
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        addAlias(aliases, stringValue(template.get("metricIdentifier")));
        addAlias(aliases, stringValue(template.get("normativeIdentifier")));
        addAlias(aliases, stringValue(template.get("contractIdentifier")));
        Object rawMetricAliases = template.get("metricAliases");
        if (rawMetricAliases instanceof Collection<?> values) {
            values.forEach(value -> addAlias(aliases, stringValue(value)));
        }
        Object rawMatch = template.get("match");
        List<String> productKeys = List.of();
        if (rawMatch instanceof Map<?, ?> match) {
            Object rawIdentifiers = match.get("rawIdentifiers");
            if (rawIdentifiers instanceof Collection<?> values) {
                values.forEach(value -> addAlias(aliases, stringValue(value)));
            }
            Object rawProductKeys = match.get("productKeys");
            if (rawProductKeys instanceof Collection<?> values) {
                productKeys = values.stream()
                        .map(this::stringValue)
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .toList();
            }
        }
        if (aliases.isEmpty()) {
            return null;
        }
        String key = stringValue(template.get("semanticTemplateKey"));
        if (!StringUtils.hasText(key)) {
            key = aliases.iterator().next();
        }
        return new ConfirmedThresholdTemplate(
                stringValue(template.get("productType")),
                aliases,
                productKeys,
                key,
                expression
        );
    }

    private boolean matchesProductKey(ConfirmedThresholdTemplate template, Product product) {
        if (template.productKeys().isEmpty()) {
            return true;
        }
        String productKey = product == null ? null : product.getProductKey();
        return StringUtils.hasText(productKey) && template.productKeys().contains(productKey.trim());
    }

    private void addAlias(Set<String> aliases, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalized = value.trim();
        aliases.add(normalized);
        int lastDotIndex = normalized.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex + 1 < normalized.length()) {
            aliases.add(normalized.substring(lastDotIndex + 1));
        }
    }

    private ValueStats queryStats(JdbcTemplate jdbcTemplate,
                                  List<String> metricCandidates,
                                  Collection<Long> deviceIds,
                                  int windowDays) {
        LocalDateTime windowEnd = LocalDateTime.now();
        LocalDateTime windowStart = windowEnd.minusDays(windowDays);
        ValueStats combined = new ValueStats(windowDays, 0L, null, null, null);
        List<Long> distinctDeviceIds = deviceIds.stream()
                .filter(id -> id != null && id > 0L)
                .distinct()
                .toList();
        for (int start = 0; start < distinctDeviceIds.size(); start += DEVICE_CHUNK_SIZE) {
            List<Long> chunk = distinctDeviceIds.subList(start, Math.min(start + DEVICE_CHUNK_SIZE, distinctDeviceIds.size()));
            combined = combined.merge(queryColumnStats(jdbcTemplate, metricCandidates, chunk, windowStart, windowEnd, "value_double"));
            combined = combined.merge(queryColumnStats(jdbcTemplate, metricCandidates, chunk, windowStart, windowEnd, "value_long"));
        }
        return combined;
    }

    private ValueStats queryLatestStats(JdbcTemplate jdbcTemplate,
                                        List<String> metricCandidates,
                                        Collection<Long> deviceIds) {
        ValueStats combined = new ValueStats(FALLBACK_WINDOW_DAYS, 0L, null, null, null);
        List<Long> distinctDeviceIds = deviceIds.stream()
                .filter(id -> id != null && id > 0L)
                .distinct()
                .toList();
        for (int start = 0; start < distinctDeviceIds.size(); start += DEVICE_CHUNK_SIZE) {
            List<Long> chunk = distinctDeviceIds.subList(start, Math.min(start + DEVICE_CHUNK_SIZE, distinctDeviceIds.size()));
            combined = combined.merge(queryLatestColumnStats(jdbcTemplate, metricCandidates, chunk, "value_double"));
            combined = combined.merge(queryLatestColumnStats(jdbcTemplate, metricCandidates, chunk, "value_long"));
        }
        return combined;
    }

    private ValueStats queryLatestPropertySnapshotStats(List<String> metricCandidates,
                                                        Collection<Long> deviceIds) {
        DevicePropertyMapper mapper = devicePropertyMapperProvider.getIfAvailable();
        if (mapper == null || metricCandidates == null || metricCandidates.isEmpty()) {
            return new ValueStats(null, 0L, null, null, null);
        }
        ValueStats combined = new ValueStats(null, 0L, null, null, null);
        List<Long> distinctDeviceIds = deviceIds.stream()
                .filter(id -> id != null && id > 0L)
                .distinct()
                .toList();
        for (int start = 0; start < distinctDeviceIds.size(); start += DEVICE_CHUNK_SIZE) {
            List<Long> chunk = distinctDeviceIds.subList(start, Math.min(start + DEVICE_CHUNK_SIZE, distinctDeviceIds.size()));
            LambdaQueryWrapper<DeviceProperty> wrapper = new LambdaQueryWrapper<DeviceProperty>()
                    .in(DeviceProperty::getDeviceId, chunk)
                    .and(condition -> {
                        condition.in(DeviceProperty::getIdentifier, metricCandidates);
                        metricCandidates.forEach(metricIdentifier ->
                                condition.or().likeRight(DeviceProperty::getIdentifier, metricIdentifier + "."));
                        metricCandidates.forEach(metricIdentifier ->
                                condition.or().like(DeviceProperty::getIdentifier, "." + metricIdentifier));
                    })
                    .orderByDesc(DeviceProperty::getReportTime)
                    .last("LIMIT " + LATEST_SAMPLE_LIMIT);
            List<Map<String, Object>> rows = mapper.selectList(wrapper).stream()
                    .map(property -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("metric_value", property.getPropertyValue());
                        return row;
                    })
                    .toList();
            combined = combined.merge(valueStatsFromRows(rows, null));
        }
        return combined;
    }

    private ValueStats queryColumnStats(JdbcTemplate jdbcTemplate,
                                        List<String> metricCandidates,
                                        List<Long> deviceIds,
                                        LocalDateTime windowStart,
                                        LocalDateTime windowEnd,
                                        String valueColumn) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new ValueStats(daysBetween(windowStart, windowEnd), 0L, null, null, null);
        }
        String placeholders = String.join(", ", deviceIds.stream().map(id -> "?").toList());
        String metricPlaceholders = String.join(", ", metricCandidates.stream().map(value -> "?").toList());
        String metricLikeConditions = metricCandidates.stream()
                .map(value -> "metric_code LIKE ?")
                .collect(Collectors.joining(" OR "));
        String sql = """
                SELECT COUNT(1) AS sample_count,
                       MIN(%s) AS min_value,
                       MAX(%s) AS max_value,
                       AVG(%s) AS avg_value
                FROM %s
                WHERE device_id IN (%s)
                  AND (metric_code IN (%s) OR %s)
                  AND ((reported_at >= ? AND reported_at < ?) OR (reported_at IS NULL AND ts >= ? AND ts < ?))
                  AND %s IS NOT NULL
                """.formatted(valueColumn, valueColumn, valueColumn, TdengineTelemetrySchemaSupport.TABLE_NAME,
                placeholders, metricPlaceholders, metricLikeConditions, valueColumn);
        List<Object> args = new ArrayList<>(deviceIds);
        args.addAll(metricCandidates);
        metricCandidates.forEach(metricIdentifier -> args.add("%." + metricIdentifier));
        args.add(Timestamp.valueOf(windowStart));
        args.add(Timestamp.valueOf(windowEnd));
        args.add(Timestamp.valueOf(windowStart));
        args.add(Timestamp.valueOf(windowEnd));
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, args.toArray());
        return new ValueStats(
                daysBetween(windowStart, windowEnd),
                longValue(row.get("sample_count")),
                decimalValue(row.get("min_value")),
                decimalValue(row.get("max_value")),
                decimalValue(row.get("avg_value"))
        );
    }

    private ValueStats queryLatestColumnStats(JdbcTemplate jdbcTemplate,
                                              List<String> metricCandidates,
                                              List<Long> deviceIds,
                                              String valueColumn) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new ValueStats(FALLBACK_WINDOW_DAYS, 0L, null, null, null);
        }
        String placeholders = String.join(", ", deviceIds.stream().map(id -> "?").toList());
        String metricPlaceholders = String.join(", ", metricCandidates.stream().map(value -> "?").toList());
        String metricLikeConditions = metricCandidates.stream()
                .map(value -> "metric_code LIKE ?")
                .collect(Collectors.joining(" OR "));
        String sql = """
                SELECT %s AS metric_value
                FROM %s
                WHERE device_id IN (%s)
                  AND (metric_code IN (%s) OR %s)
                  AND %s IS NOT NULL
                ORDER BY ts DESC
                LIMIT %d
                """.formatted(valueColumn, TdengineTelemetrySchemaSupport.TABLE_NAME,
                placeholders, metricPlaceholders, metricLikeConditions, valueColumn, LATEST_SAMPLE_LIMIT);
        List<Object> args = new ArrayList<>(deviceIds);
        args.addAll(metricCandidates);
        metricCandidates.forEach(metricIdentifier -> args.add("%." + metricIdentifier));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args.toArray());
        return valueStatsFromRows(rows, FALLBACK_WINDOW_DAYS);
    }

    private ValueStats valueStatsFromRows(List<Map<String, Object>> rows, Integer windowDays) {
        if (rows == null || rows.isEmpty()) {
            return new ValueStats(windowDays, 0L, null, null, null);
        }
        BigDecimal min = null;
        BigDecimal max = null;
        BigDecimal sum = BigDecimal.ZERO;
        long count = 0L;
        for (Map<String, Object> row : rows) {
            BigDecimal value = decimalValue(row.get("metric_value"));
            if (value == null) {
                continue;
            }
            min = min == null || value.compareTo(min) < 0 ? value : min;
            max = max == null || value.compareTo(max) > 0 ? value : max;
            sum = sum.add(value);
            count++;
        }
        BigDecimal avg = count == 0L ? null : sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
        return new ValueStats(windowDays, count, min, max, avg);
    }

    private int daysBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        return Math.max(1, (int) java.time.Duration.between(windowStart, windowEnd).toDays());
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return normalize(decimal);
        }
        if (value instanceof Number number) {
            return normalize(BigDecimal.valueOf(number.doubleValue()));
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return normalize(new BigDecimal(text.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeExpression(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String raw = value.trim().replaceAll("\\s+", " ");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(?:value\\s*)?(>=|<=|>|<|==|=)\\s*(-?\\d+(?:\\.\\d+)?)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        if (!matcher.matches()) {
            return null;
        }
        return "value " + matcher.group(1) + " " + matcher.group(2);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    private String format(BigDecimal value) {
        return normalize(value).toPlainString();
    }

    private record ValueStats(
            Integer windowDays,
            Long sampleCount,
            BigDecimal minValue,
            BigDecimal maxValue,
            BigDecimal avgValue
    ) {
        ValueStats merge(ValueStats other) {
            if (other == null || other.sampleCount() == null || other.sampleCount() <= 0L) {
                return this;
            }
            if (sampleCount == null || sampleCount <= 0L) {
                return other;
            }
            long totalCount = sampleCount + other.sampleCount();
            BigDecimal totalSum = avgValue.multiply(BigDecimal.valueOf(sampleCount))
                    .add(other.avgValue().multiply(BigDecimal.valueOf(other.sampleCount())));
            return new ValueStats(
                    windowDays,
                    totalCount,
                    min(minValue, other.minValue()),
                    max(maxValue, other.maxValue()),
                    totalSum.divide(BigDecimal.valueOf(totalCount), 6, RoundingMode.HALF_UP).stripTrailingZeros()
            );
        }

        private BigDecimal min(BigDecimal left, BigDecimal right) {
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            return left.compareTo(right) <= 0 ? left : right;
        }

        private BigDecimal max(BigDecimal left, BigDecimal right) {
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            return left.compareTo(right) >= 0 ? left : right;
        }
    }

    private record ConfirmedThresholdTemplate(
            String productType,
            Set<String> metricAliases,
            List<String> productKeys,
            String semanticTemplateKey,
            String expression
    ) {
    }
}
