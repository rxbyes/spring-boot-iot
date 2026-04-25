package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.framework.observability.SensitiveLogSanitizer;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC 版可观测证据写入器。
 */
@Slf4j
@Service
public class JdbcObservabilityEvidenceRecorder implements ObservabilityEvidenceRecorder {

    private static final String SPAN_TABLE = "sys_observability_span_log";
    private static final String BUSINESS_EVENT_TABLE = "sys_business_event_log";
    private static final int TEXT_64 = 64;
    private static final int TEXT_128 = 128;
    private static final int TEXT_255 = 255;
    private static final int TEXT_500 = 500;
    private static final int TEXT_32 = 32;
    private static final String TRUNCATED_SUFFIX = "...(truncated)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final Map<String, Set<String>> columnCache = new ConcurrentHashMap<>();

    public JdbcObservabilityEvidenceRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void recordSpan(ObservabilitySpanLogRecord span) {
        try {
            Set<String> columns = getColumns(SPAN_TABLE);
            if (CollectionUtils.isEmpty(columns)) {
                return;
            }
            ObservabilitySpanLogRecord normalized = normalizeSpan(span);
            Map<String, Object> values = new LinkedHashMap<>();
            put(values, columns, "id", IdWorker.getId());
            put(values, columns, "tenant_id", normalized.getTenantId());
            put(values, columns, "trace_id", truncate(normalized.getTraceId(), TEXT_64));
            put(values, columns, "parent_span_id", normalized.getParentSpanId());
            put(values, columns, "span_type", truncate(normalized.getSpanType(), TEXT_64));
            put(values, columns, "span_name", truncate(normalized.getSpanName(), TEXT_128));
            put(values, columns, "domain_code", truncate(normalized.getDomainCode(), TEXT_64));
            put(values, columns, "event_code", truncate(normalized.getEventCode(), TEXT_128));
            put(values, columns, "object_type", truncate(normalized.getObjectType(), TEXT_64));
            put(values, columns, "object_id", truncate(normalized.getObjectId(), TEXT_128));
            put(values, columns, "transport_type", truncate(normalized.getTransportType(), TEXT_32));
            put(values, columns, "status", truncate(normalized.getStatus(), TEXT_32));
            put(values, columns, "duration_ms", normalized.getDurationMs());
            put(values, columns, "started_at", toTimestamp(normalized.getStartedAt()));
            put(values, columns, "finished_at", toTimestamp(normalized.getFinishedAt()));
            put(values, columns, "error_class", truncate(normalized.getErrorClass(), TEXT_255));
            put(values, columns, "error_message", truncate(SensitiveLogSanitizer.sanitize(normalized.getErrorMessage()), TEXT_500));
            put(values, columns, "tags_json", toJson(normalized.getTags()));
            put(values, columns, "create_time", Timestamp.valueOf(LocalDateTime.now()));
            put(values, columns, "deleted", 0);
            insert(SPAN_TABLE, values);
        } catch (Exception ex) {
            log.warn("写入可观测 span 失败, error={}", ex.getMessage());
        }
    }

    @Override
    public void recordBusinessEvent(BusinessEventLogRecord event) {
        try {
            Set<String> columns = getColumns(BUSINESS_EVENT_TABLE);
            if (CollectionUtils.isEmpty(columns)) {
                return;
            }
            BusinessEventLogRecord normalized = normalizeBusinessEvent(event);
            Map<String, Object> values = new LinkedHashMap<>();
            put(values, columns, "id", IdWorker.getId());
            put(values, columns, "tenant_id", normalized.getTenantId());
            put(values, columns, "trace_id", truncate(normalized.getTraceId(), TEXT_64));
            put(values, columns, "event_code", truncate(normalized.getEventCode(), TEXT_128));
            put(values, columns, "event_name", truncate(normalized.getEventName(), TEXT_128));
            put(values, columns, "domain_code", truncate(normalized.getDomainCode(), TEXT_64));
            put(values, columns, "action_code", truncate(normalized.getActionCode(), TEXT_64));
            put(values, columns, "object_type", truncate(normalized.getObjectType(), TEXT_64));
            put(values, columns, "object_id", truncate(normalized.getObjectId(), TEXT_128));
            put(values, columns, "object_name", truncate(normalized.getObjectName(), TEXT_255));
            put(values, columns, "actor_user_id", normalized.getActorUserId());
            put(values, columns, "actor_name", truncate(normalized.getActorName(), TEXT_64));
            put(values, columns, "result_status", truncate(normalized.getResultStatus(), TEXT_32));
            put(values, columns, "source_type", truncate(normalized.getSourceType(), TEXT_32));
            put(values, columns, "evidence_type", truncate(normalized.getEvidenceType(), TEXT_64));
            put(values, columns, "evidence_id", truncate(normalized.getEvidenceId(), TEXT_128));
            put(values, columns, "request_method", truncate(normalized.getRequestMethod(), 16));
            put(values, columns, "request_uri", truncate(normalized.getRequestUri(), TEXT_255));
            put(values, columns, "duration_ms", normalized.getDurationMs());
            put(values, columns, "error_code", truncate(normalized.getErrorCode(), TEXT_64));
            put(values, columns, "error_message", truncate(SensitiveLogSanitizer.sanitize(normalized.getErrorMessage()), TEXT_500));
            put(values, columns, "metadata_json", toJson(normalized.getMetadata()));
            put(values, columns, "occurred_at", toTimestamp(normalized.getOccurredAt()));
            put(values, columns, "create_time", Timestamp.valueOf(LocalDateTime.now()));
            put(values, columns, "deleted", 0);
            insert(BUSINESS_EVENT_TABLE, values);
        } catch (Exception ex) {
            log.warn("写入业务事件证据失败, error={}", ex.getMessage());
        }
    }

    private ObservabilitySpanLogRecord normalizeSpan(ObservabilitySpanLogRecord span) {
        ObservabilitySpanLogRecord target = span == null ? new ObservabilitySpanLogRecord() : span;
        if (target.getTenantId() == null) {
            target.setTenantId(1L);
        }
        if (!StringUtils.hasText(target.getSpanType())) {
            target.setSpanType("UNKNOWN");
        }
        if (!StringUtils.hasText(target.getSpanName())) {
            target.setSpanName(target.getSpanType());
        }
        if (!StringUtils.hasText(target.getStatus())) {
            target.setStatus(ObservabilityEvidenceStatus.SUCCESS);
        }
        LocalDateTime now = LocalDateTime.now();
        if (target.getStartedAt() == null) {
            target.setStartedAt(now);
        }
        if (target.getFinishedAt() == null) {
            target.setFinishedAt(target.getStartedAt());
        }
        return target;
    }

    private BusinessEventLogRecord normalizeBusinessEvent(BusinessEventLogRecord event) {
        BusinessEventLogRecord target = event == null ? new BusinessEventLogRecord() : event;
        if (target.getTenantId() == null) {
            target.setTenantId(1L);
        }
        if (!StringUtils.hasText(target.getEventCode())) {
            target.setEventCode("platform.unknown");
        }
        if (!StringUtils.hasText(target.getEventName())) {
            target.setEventName(target.getEventCode());
        }
        if (!StringUtils.hasText(target.getDomainCode())) {
            target.setDomainCode("platform");
        }
        if (!StringUtils.hasText(target.getActionCode())) {
            target.setActionCode("unknown");
        }
        if (!StringUtils.hasText(target.getResultStatus())) {
            target.setResultStatus(ObservabilityEvidenceStatus.SUCCESS);
        }
        if (!StringUtils.hasText(target.getSourceType())) {
            target.setSourceType("SYSTEM");
        }
        if (target.getOccurredAt() == null) {
            target.setOccurredAt(LocalDateTime.now());
        }
        return target;
    }

    private Set<String> getColumns(String tableName) {
        return columnCache.computeIfAbsent(tableName, table -> {
            try {
                List<String> columns = jdbcTemplate.queryForList(
                        """
                        SELECT COLUMN_NAME
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                        """,
                        String.class,
                        table
                );
                return Set.copyOf(columns);
            } catch (Exception ex) {
                log.warn("读取可观测证据表字段失败, table={}, error={}", table, ex.getMessage());
                return Set.of();
            }
        });
    }

    private void insert(String tableName, Map<String, Object> values) {
        if (values.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", values.keySet()) + ") VALUES ("
                + String.join(", ", values.keySet().stream().map(item -> "?").toList()) + ")";
        jdbcTemplate.update(sql, values.values().toArray());
    }

    private void put(Map<String, Object> values, Set<String> columns, String column, Object value) {
        if (value != null && columns.contains(column)) {
            values.put(column, value);
        }
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= TRUNCATED_SUFFIX.length()) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
    }
}
