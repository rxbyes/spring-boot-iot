package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.device.entity.DeviceInvalidReportState;
import com.ghlzm.iot.device.service.DeviceInvalidReportStateService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 无效 MQTT 上报最新态服务实现。
 */
@Service
public class DeviceInvalidReportStateServiceImpl implements DeviceInvalidReportStateService {

    private static final String TABLE_NAME = "iot_device_invalid_report_state";
    private static final Long DEFAULT_TENANT_ID = 1L;

    private final JdbcTemplate jdbcTemplate;
    private final DeviceInvalidReportStateSchemaSupport schemaSupport;

    public DeviceInvalidReportStateServiceImpl(JdbcTemplate jdbcTemplate,
                                               DeviceInvalidReportStateSchemaSupport schemaSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaSupport = schemaSupport;
    }

    @Override
    public void upsertState(DeviceInvalidReportState state) {
        if (state == null || !StringUtils.hasText(state.getGovernanceKey())) {
            return;
        }
        Set<String> columns = schemaSupport.getColumns();
        if (columns.isEmpty() || !columns.contains("governance_key")) {
            return;
        }
        DeviceInvalidReportState normalized = normalizeState(state);
        if (updateExistingState(normalized, columns) > 0) {
            return;
        }
        insertState(normalized, columns);
    }

    @Override
    public void markResolvedByDevice(String productKey, String deviceCode, LocalDateTime resolvedTime) {
        if (!StringUtils.hasText(productKey) || !StringUtils.hasText(deviceCode)) {
            return;
        }
        Set<String> columns = schemaSupport.getColumns();
        if (!columns.containsAll(Set.of("resolved", "resolved_time", "product_key", "device_code"))) {
            return;
        }
        StringBuilder sql = new StringBuilder("""
                UPDATE iot_device_invalid_report_state
                   SET resolved = 1, resolved_time = ?
                 WHERE product_key = ?
                   AND device_code = ?
                """);
        if (columns.contains("deleted")) {
            sql.append(" AND deleted = 0");
        }
        if (columns.contains("resolved")) {
            sql.append(" AND resolved = 0");
        }
        jdbcTemplate.update(
                sql.toString(),
                resolvedTime == null ? LocalDateTime.now() : resolvedTime,
                productKey.trim(),
                deviceCode.trim()
        );
    }

    private int updateExistingState(DeviceInvalidReportState state, Set<String> columns) {
        Map<String, Object> values = new LinkedHashMap<>();
        putValue(values, columns, "tenant_id", state.getTenantId());
        putValue(values, columns, "reason_code", state.getReasonCode());
        putValue(values, columns, "request_method", state.getRequestMethod());
        putValue(values, columns, "failure_stage", state.getFailureStage());
        putValue(values, columns, "device_code", state.getDeviceCode());
        putValue(values, columns, "product_key", state.getProductKey());
        putValue(values, columns, "protocol_code", state.getProtocolCode());
        putValue(values, columns, "topic_route_type", state.getTopicRouteType());
        putValue(values, columns, "topic", state.getTopic());
        putValue(values, columns, "client_id", state.getClientId());
        putValue(values, columns, "payload_size", state.getPayloadSize());
        putValue(values, columns, "payload_encoding", state.getPayloadEncoding());
        putValue(values, columns, "last_payload", state.getLastPayload());
        putValue(values, columns, "last_trace_id", state.getLastTraceId());
        putValue(values, columns, "sample_error_message", state.getSampleErrorMessage());
        putValue(values, columns, "sample_exception_class", state.getSampleExceptionClass());
        putValue(values, columns, "first_seen_time", state.getFirstSeenTime());
        putValue(values, columns, "last_seen_time", state.getLastSeenTime());
        putValue(values, columns, "hit_count", state.getHitCount());
        putValue(values, columns, "sampled_count", state.getSampledCount());
        putValue(values, columns, "suppressed_count", state.getSuppressedCount());
        putValue(values, columns, "suppressed_until", state.getSuppressedUntil());
        putValue(values, columns, "resolved", 0);
        putValue(values, columns, "resolved_time", null);
        if (values.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(TABLE_NAME).append(" SET ");
        List<Object> args = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                sql.append(", ");
            }
            if (isCounterColumn(entry.getKey())) {
                sql.append(entry.getKey()).append(" = COALESCE(").append(entry.getKey()).append(", 0) + ?");
            } else {
                sql.append(entry.getKey()).append(" = ?");
            }
            args.add(entry.getValue());
            first = false;
        }
        sql.append(" WHERE governance_key = ?");
        args.add(state.getGovernanceKey());
        if (columns.contains("deleted")) {
            sql.append(" AND deleted = 0");
        }
        return jdbcTemplate.update(sql.toString(), args.toArray());
    }

    private void insertState(DeviceInvalidReportState state, Set<String> columns) {
        Map<String, Object> values = new LinkedHashMap<>();
        putValue(values, columns, "id", state.getId());
        putValue(values, columns, "tenant_id", state.getTenantId());
        putValue(values, columns, "governance_key", state.getGovernanceKey());
        putValue(values, columns, "reason_code", state.getReasonCode());
        putValue(values, columns, "request_method", state.getRequestMethod());
        putValue(values, columns, "failure_stage", state.getFailureStage());
        putValue(values, columns, "device_code", state.getDeviceCode());
        putValue(values, columns, "product_key", state.getProductKey());
        putValue(values, columns, "protocol_code", state.getProtocolCode());
        putValue(values, columns, "topic_route_type", state.getTopicRouteType());
        putValue(values, columns, "topic", state.getTopic());
        putValue(values, columns, "client_id", state.getClientId());
        putValue(values, columns, "payload_size", state.getPayloadSize());
        putValue(values, columns, "payload_encoding", state.getPayloadEncoding());
        putValue(values, columns, "last_payload", state.getLastPayload());
        putValue(values, columns, "last_trace_id", state.getLastTraceId());
        putValue(values, columns, "sample_error_message", state.getSampleErrorMessage());
        putValue(values, columns, "sample_exception_class", state.getSampleExceptionClass());
        putValue(values, columns, "first_seen_time", state.getFirstSeenTime());
        putValue(values, columns, "last_seen_time", state.getLastSeenTime());
        putValue(values, columns, "hit_count", state.getHitCount());
        putValue(values, columns, "sampled_count", state.getSampledCount());
        putValue(values, columns, "suppressed_count", state.getSuppressedCount());
        putValue(values, columns, "suppressed_until", state.getSuppressedUntil());
        putValue(values, columns, "resolved", state.getResolved());
        putValue(values, columns, "resolved_time", state.getResolvedTime());
        putValue(values, columns, "deleted", state.getDeleted());
        if (values.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO " + TABLE_NAME + " (" + String.join(", ", values.keySet()) + ") VALUES ("
                + String.join(", ", values.keySet().stream().map(item -> "?").toList()) + ")";
        jdbcTemplate.update(sql, values.values().toArray());
    }

    private DeviceInvalidReportState normalizeState(DeviceInvalidReportState state) {
        LocalDateTime now = state.getLastSeenTime() == null ? LocalDateTime.now() : state.getLastSeenTime();
        if (state.getId() == null) {
            state.setId(IdWorker.getId());
        }
        if (state.getTenantId() == null) {
            state.setTenantId(DEFAULT_TENANT_ID);
        }
        state.setGovernanceKey(state.getGovernanceKey().trim());
        state.setReasonCode(trimToNull(state.getReasonCode()));
        state.setRequestMethod(trimToNull(state.getRequestMethod()));
        state.setFailureStage(trimToNull(state.getFailureStage()));
        state.setDeviceCode(trimToNull(state.getDeviceCode()));
        state.setProductKey(trimToNull(state.getProductKey()));
        state.setProtocolCode(trimToNull(state.getProtocolCode()));
        state.setTopicRouteType(trimToNull(state.getTopicRouteType()));
        state.setTopic(trimToNull(state.getTopic()));
        state.setClientId(trimToNull(state.getClientId()));
        state.setPayloadEncoding(trimToNull(state.getPayloadEncoding()));
        state.setLastTraceId(trimToNull(state.getLastTraceId()));
        state.setSampleErrorMessage(trimToNull(state.getSampleErrorMessage()));
        state.setSampleExceptionClass(trimToNull(state.getSampleExceptionClass()));
        state.setFirstSeenTime(state.getFirstSeenTime() == null ? now : state.getFirstSeenTime());
        state.setLastSeenTime(now);
        state.setHitCount(defaultLong(state.getHitCount()));
        state.setSampledCount(defaultLong(state.getSampledCount()));
        state.setSuppressedCount(defaultLong(state.getSuppressedCount()));
        state.setResolved(0);
        state.setResolvedTime(null);
        state.setDeleted(0);
        return state;
    }

    private void putValue(Map<String, Object> values, Set<String> columns, String column, Object value) {
        if (columns.contains(column)) {
            values.put(column, value);
        }
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private boolean isCounterColumn(String column) {
        return "hit_count".equals(column)
                || "sampled_count".equals(column)
                || "suppressed_count".equals(column);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
