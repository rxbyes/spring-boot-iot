package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.telemetry.service.model.TelemetryProjectionTask;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telemetry 聚合投影器。
 * 当前实现只负责 MEASURE 小时聚合。
 */
@Service
public class TelemetryAggregateProjector {

    private static final Logger log = LoggerFactory.getLogger(TelemetryAggregateProjector.class);
    private static final DateTimeFormatter SQL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    private final TelemetryAggregateSchemaSupport schemaSupport;
    private final TelemetryAggregateTableNamingStrategy tableNamingStrategy;

    public TelemetryAggregateProjector(TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider,
                                       TelemetryAggregateSchemaSupport schemaSupport,
                                       TelemetryAggregateTableNamingStrategy tableNamingStrategy) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.schemaSupport = schemaSupport;
        this.tableNamingStrategy = tableNamingStrategy;
    }

    public void project(TelemetryProjectionTask task) {
        if (task == null || task.getPoints() == null || task.getPoints().isEmpty()) {
            return;
        }
        try {
            Map<String, HourlyAggregate> groupedAggregates = groupHourlyAggregates(task.getPoints());
            if (groupedAggregates.isEmpty()) {
                return;
            }
            Map<String, List<HourlyAggregate>> childTableAggregates = new LinkedHashMap<>();
            for (HourlyAggregate aggregate : groupedAggregates.values()) {
                String childTable = tableNamingStrategy.resolveChildTableName(aggregate.samplePoint.getTenantId(),
                        aggregate.samplePoint.getDeviceId());
                childTableAggregates.computeIfAbsent(childTable, key -> new ArrayList<>()).add(aggregate);
            }
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate();
            for (Map.Entry<String, List<HourlyAggregate>> entry : childTableAggregates.entrySet()) {
                List<HourlyAggregate> aggregates = entry.getValue();
                if (aggregates.isEmpty()) {
                    continue;
                }
                schemaSupport.ensureChildTable(aggregates.get(0).samplePoint);
                List<HourlyAggregate> mergedAggregates = new ArrayList<>();
                for (HourlyAggregate aggregate : aggregates) {
                    mergedAggregates.add(mergeWithExisting(entry.getKey(), aggregate));
                }
                jdbcTemplate.batchUpdate(buildInsertSql(entry.getKey()), buildBatchArgs(mergedAggregates));
            }
        } catch (Exception ex) {
            log.warn("写入 telemetry 小时聚合失败, traceId={}, error={}", task.getTraceId(), ex.getMessage());
        }
    }

    private Map<String, HourlyAggregate> groupHourlyAggregates(List<TelemetryV2Point> points) {
        Map<String, HourlyAggregate> groupedAggregates = new LinkedHashMap<>();
        for (TelemetryV2Point point : points) {
            Double numericValue = resolveNumericValue(point);
            if (numericValue == null) {
                continue;
            }
            LocalDateTime pointTime = resolvePointTime(point);
            LocalDateTime windowStart = pointTime.withMinute(0).withSecond(0).withNano(0);
            String key = buildGroupKey(point, windowStart);
            groupedAggregates.compute(key, (ignored, existing) -> {
                if (existing == null) {
                    return HourlyAggregate.fromPoint(point, windowStart, pointTime, numericValue);
                }
                existing.accumulate(point, pointTime, numericValue);
                return existing;
            });
        }
        return groupedAggregates;
    }

    private HourlyAggregate mergeWithExisting(String childTable, HourlyAggregate currentAggregate) {
        ExistingAggregateRow existingRow = loadExistingAggregate(childTable, currentAggregate.windowStart, currentAggregate.metricId);
        if (existingRow == null) {
            return currentAggregate;
        }
        boolean existingKeepsLastValue = existingRow.lastReportedAt() != null
                && (currentAggregate.lastReportedAt == null || existingRow.lastReportedAt().isAfter(currentAggregate.lastReportedAt));
        currentAggregate.firstReportedAt = min(currentAggregate.firstReportedAt, existingRow.firstReportedAt());
        currentAggregate.lastReportedAt = max(currentAggregate.lastReportedAt, existingRow.lastReportedAt());
        currentAggregate.minValueDouble = Math.min(currentAggregate.minValueDouble, existingRow.minValueDouble());
        currentAggregate.maxValueDouble = Math.max(currentAggregate.maxValueDouble, existingRow.maxValueDouble());
        currentAggregate.sumValueDouble += existingRow.sumValueDouble();
        currentAggregate.sampleCount += existingRow.sampleCount();
        if (existingKeepsLastValue) {
            currentAggregate.lastValueDouble = existingRow.lastValueDouble();
            currentAggregate.traceId = existingRow.traceId();
            currentAggregate.sourceMessageType = existingRow.sourceMessageType();
        }
        return currentAggregate;
    }

    private ExistingAggregateRow loadExistingAggregate(String childTable, LocalDateTime windowStart, String metricId) {
        String sql = "SELECT first_reported_at, last_reported_at, min_value_double, max_value_double, "
                + "sum_value_double, last_value_double, sample_count, trace_id, source_message_type "
                + "FROM " + childTable
                + " WHERE ts = '" + SQL_TIME_FORMATTER.format(windowStart) + "'"
                + " AND metric_id = '" + escape(metricId, 128) + "' LIMIT 1";
        return jdbcTemplateProvider.getJdbcTemplate().query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            Timestamp firstReportedAt = rs.getTimestamp("first_reported_at");
            Timestamp lastReportedAt = rs.getTimestamp("last_reported_at");
            return new ExistingAggregateRow(
                    firstReportedAt == null ? null : firstReportedAt.toLocalDateTime(),
                    lastReportedAt == null ? null : lastReportedAt.toLocalDateTime(),
                    rs.getDouble("min_value_double"),
                    rs.getDouble("max_value_double"),
                    rs.getDouble("sum_value_double"),
                    rs.getDouble("last_value_double"),
                    rs.getLong("sample_count"),
                    rs.getString("trace_id"),
                    rs.getString("source_message_type")
            );
        });
    }

    private String buildInsertSql(String childTable) {
        return "INSERT INTO " + childTable + " ("
                + "ts, metric_id, metric_code, metric_name, value_type, first_reported_at, last_reported_at,"
                + " min_value_double, max_value_double, sum_value_double, last_value_double, sample_count,"
                + " trace_id, source_message_type"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private List<Object[]> buildBatchArgs(List<HourlyAggregate> aggregates) {
        List<Object[]> args = new ArrayList<>();
        for (HourlyAggregate aggregate : aggregates) {
            args.add(new Object[]{
                    Timestamp.valueOf(aggregate.windowStart),
                    escape(aggregate.metricId, 128),
                    escape(aggregate.metricCode, 128),
                    truncate(aggregate.metricName, 128),
                    truncate(aggregate.valueType, 32),
                    Timestamp.valueOf(aggregate.firstReportedAt),
                    Timestamp.valueOf(aggregate.lastReportedAt),
                    aggregate.minValueDouble,
                    aggregate.maxValueDouble,
                    aggregate.sumValueDouble,
                    aggregate.lastValueDouble,
                    aggregate.sampleCount,
                    truncate(aggregate.traceId, 64),
                    truncate(aggregate.sourceMessageType, 32)
            });
        }
        return args;
    }

    private String buildGroupKey(TelemetryV2Point point, LocalDateTime windowStart) {
        return safe(point.getTenantId()) + ":" + safe(point.getDeviceId()) + ":" + safe(point.getMetricId()) + ":"
                + SQL_TIME_FORMATTER.format(windowStart);
    }

    private Double resolveNumericValue(TelemetryV2Point point) {
        if (point == null || point.getStreamKind() != TelemetryStreamKind.MEASURE) {
            return null;
        }
        if (point.getValueDouble() != null) {
            return point.getValueDouble();
        }
        if (point.getValueLong() != null) {
            return point.getValueLong().doubleValue();
        }
        return null;
    }

    private LocalDateTime resolvePointTime(TelemetryV2Point point) {
        if (point.getReportedAt() != null) {
            return point.getReportedAt();
        }
        if (point.getIngestedAt() != null) {
            return point.getIngestedAt();
        }
        return LocalDateTime.now();
    }

    private LocalDateTime min(LocalDateTime current, LocalDateTime candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return candidate.isBefore(current) ? candidate : current;
    }

    private LocalDateTime max(LocalDateTime current, LocalDateTime candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return candidate.isAfter(current) ? candidate : current;
    }

    private String safe(Long value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return truncate(value.replace("'", "''"), maxLength);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ExistingAggregateRow(LocalDateTime firstReportedAt,
                                        LocalDateTime lastReportedAt,
                                        double minValueDouble,
                                        double maxValueDouble,
                                        double sumValueDouble,
                                        double lastValueDouble,
                                        long sampleCount,
                                        String traceId,
                                        String sourceMessageType) {
    }

    private static class HourlyAggregate {

        private final TelemetryV2Point samplePoint;
        private final String metricId;
        private final String metricCode;
        private final String metricName;
        private final String valueType;
        private final LocalDateTime windowStart;
        private LocalDateTime firstReportedAt;
        private LocalDateTime lastReportedAt;
        private LocalDateTime lastIngestedAt;
        private double minValueDouble;
        private double maxValueDouble;
        private double sumValueDouble;
        private double lastValueDouble;
        private long sampleCount;
        private String traceId;
        private String sourceMessageType;

        private HourlyAggregate(TelemetryV2Point samplePoint,
                                LocalDateTime windowStart,
                                LocalDateTime pointTime,
                                double value) {
            this.samplePoint = samplePoint;
            this.metricId = samplePoint.getMetricId();
            this.metricCode = samplePoint.getMetricCode();
            this.metricName = samplePoint.getMetricName();
            this.valueType = "double";
            this.windowStart = windowStart;
            this.firstReportedAt = pointTime;
            this.lastReportedAt = pointTime;
            this.lastIngestedAt = samplePoint.getIngestedAt();
            this.minValueDouble = value;
            this.maxValueDouble = value;
            this.sumValueDouble = value;
            this.lastValueDouble = value;
            this.sampleCount = 1L;
            this.traceId = samplePoint.getTraceId();
            this.sourceMessageType = samplePoint.getSourceMessageType();
        }

        private static HourlyAggregate fromPoint(TelemetryV2Point point,
                                                 LocalDateTime windowStart,
                                                 LocalDateTime pointTime,
                                                 double value) {
            return new HourlyAggregate(point, windowStart, pointTime, value);
        }

        private void accumulate(TelemetryV2Point point, LocalDateTime pointTime, double value) {
            firstReportedAt = pointTime.isBefore(firstReportedAt) ? pointTime : firstReportedAt;
            if (isNewer(pointTime, point.getIngestedAt())) {
                lastReportedAt = pointTime;
                lastIngestedAt = point.getIngestedAt();
                lastValueDouble = value;
                traceId = point.getTraceId();
                sourceMessageType = point.getSourceMessageType();
            }
            minValueDouble = Math.min(minValueDouble, value);
            maxValueDouble = Math.max(maxValueDouble, value);
            sumValueDouble += value;
            sampleCount += 1L;
        }

        private boolean isNewer(LocalDateTime candidateReportedAt, LocalDateTime candidateIngestedAt) {
            if (candidateReportedAt == null) {
                return false;
            }
            if (lastReportedAt == null || candidateReportedAt.isAfter(lastReportedAt)) {
                return true;
            }
            return candidateReportedAt.isEqual(lastReportedAt)
                    && candidateIngestedAt != null
                    && (lastIngestedAt == null || candidateIngestedAt.isAfter(lastIngestedAt));
        }
    }
}
