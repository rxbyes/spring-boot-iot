package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.service.UnregisteredDeviceRosterService;
import com.ghlzm.iot.device.vo.DevicePageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 未登记设备名单服务实现。
 */
@Slf4j
@Service
public class UnregisteredDeviceRosterServiceImpl implements UnregisteredDeviceRosterService {

    private static final String INVALID_REPORT_STATE_TABLE = "iot_device_invalid_report_state";
    private static final String ACCESS_ERROR_TABLE = "iot_device_access_error_log";
    private static final String MESSAGE_LOG_TABLE = "iot_device_message_log";
    private static final String DEVICE_TABLE = "iot_device";
    private static final String PRODUCT_TABLE = "iot_product";
    private static final Set<String> REQUIRED_INVALID_REPORT_STATE_COLUMNS = Set.of(
            "id",
            "device_code",
            "product_key",
            "protocol_code",
            "failure_stage",
            "sample_error_message",
            "topic",
            "last_trace_id",
            "last_payload",
            "last_seen_time",
            "reason_code",
            "resolved",
            "deleted"
    );
    private static final Set<String> REQUIRED_ACCESS_ERROR_COLUMNS = Set.of(
            "id",
            "device_code",
            "product_key",
            "protocol_code",
            "failure_stage",
            "error_message",
            "topic",
            "trace_id",
            "raw_payload",
            "create_time",
            "deleted"
    );
    private static final String UNREGISTERED_DEVICE_NAME = "未登记设备";
    private static final String INVALID_REPORT_STATE_SOURCE = "invalid_report_state";
    private static final String ACCESS_ERROR_SOURCE = "access_error";
    private static final String DISPATCH_FAILED_SOURCE = "dispatch_failed";
    private static final String DEVICE_NOT_FOUND_REASON = "DEVICE_NOT_FOUND";
    private static final String DISPATCH_FAILED_MESSAGE_TYPE = "dispatch_failed";
    private static final String MESSAGE_DISPATCH_FAILURE_STAGE = "message_dispatch";
    private static final String DISPATCH_FAILED_MESSAGE = "未登记设备最近一次上报已记录到失败轨迹。";
    private static final Long UNKNOWN_DEVICE_ID = 0L;

    private final JdbcTemplate jdbcTemplate;
    private final DeviceInvalidReportStateSchemaSupport invalidReportStateSchemaSupport;
    private final DeviceAccessErrorLogSchemaSupport accessErrorLogSchemaSupport;

    public UnregisteredDeviceRosterServiceImpl(JdbcTemplate jdbcTemplate,
                                               DeviceAccessErrorLogSchemaSupport accessErrorLogSchemaSupport,
                                               DeviceInvalidReportStateSchemaSupport invalidReportStateSchemaSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.invalidReportStateSchemaSupport = invalidReportStateSchemaSupport;
        this.accessErrorLogSchemaSupport = accessErrorLogSchemaSupport;
    }

    @Override
    public long countByFilters(String keyword, String productKey, String productName, String deviceCode) {
        return countByFilters(null, keyword, productKey, productName, deviceCode);
    }

    @Override
    public long countByFilters(Long tenantId, String keyword, String productKey, String productName, String deviceCode) {
        if (supportsInvalidReportStateSource()) {
            try {
                return countFromInvalidStateMergedSources(tenantId, keyword, productKey, productName, deviceCode);
            } catch (Exception ex) {
                log.warn("统计未登记设备最新态失败，回退失败样本来源, error={}", ex.getMessage());
            }
        }
        if (supportsAccessErrorSource()) {
            try {
                return countFromMergedSources(tenantId, keyword, productKey, productName, deviceCode);
            } catch (Exception ex) {
                log.warn("统计未登记设备失败，回退失败轨迹来源, error={}", ex.getMessage());
            }
        }
        return countFromDispatchFailure(tenantId, keyword, productKey, productName, deviceCode);
    }

    @Override
    public List<DevicePageVO> listByFilters(String keyword, String productKey, String productName, String deviceCode, long offset, long limit) {
        return listByFilters(null, keyword, productKey, productName, deviceCode, offset, limit);
    }

    @Override
    public List<DevicePageVO> listByFilters(Long tenantId, String keyword, String productKey, String productName, String deviceCode, long offset, long limit) {
        long safeOffset = Math.max(offset, 0L);
        long safeLimit = Math.max(limit, 0L);
        if (safeLimit == 0L) {
            return List.of();
        }

        if (supportsInvalidReportStateSource()) {
            try {
                return listFromInvalidStateMergedSources(tenantId, keyword, productKey, productName, deviceCode, safeOffset, safeLimit);
            } catch (Exception ex) {
                log.warn("查询未登记设备最新态失败，回退失败样本来源, error={}", ex.getMessage());
            }
        }
        if (supportsAccessErrorSource()) {
            try {
                return listFromMergedSources(tenantId, keyword, productKey, productName, deviceCode, safeOffset, safeLimit);
            } catch (Exception ex) {
                log.warn("查询未登记设备失败，回退失败轨迹来源, error={}", ex.getMessage());
            }
        }
        return listFromDispatchFailure(tenantId, keyword, productKey, productName, deviceCode, safeOffset, safeLimit);
    }

    private boolean supportsAccessErrorSource() {
        return accessErrorLogSchemaSupport.getColumns().containsAll(REQUIRED_ACCESS_ERROR_COLUMNS);
    }

    private boolean supportsInvalidReportStateSource() {
        return invalidReportStateSchemaSupport.getColumns().containsAll(REQUIRED_INVALID_REPORT_STATE_COLUMNS);
    }

    private long countFromInvalidStateMergedSources(Long tenantId, String keyword, String productKey, String productName, String deviceCode) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM (
                """);
        appendInvalidStateSelection(sql, params, tenantId, keyword, productKey, productName, deviceCode);
        sql.append("""
                    UNION ALL
                """);
        appendDispatchFailureLatestSelectionExcludingInvalidState(sql, params, tenantId, keyword, productKey, productName, deviceCode);
        sql.append("""
                ) merged_devices
                """);
        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result == null ? 0L : result;
    }

    private long countFromAccessError(Long tenantId, String keyword, String productKey, String productName, String deviceCode) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM (
                    SELECT e.device_code
                    FROM iot_device_access_error_log e
                    LEFT JOIN iot_device d
                      ON d.device_code = e.device_code
                     AND d.deleted = 0
                    WHERE e.deleted = 0
                      AND e.device_code IS NOT NULL
                      AND TRIM(e.device_code) <> ''
                      AND d.id IS NULL
                """);
        appendTenantEquals(sql, params, "e.tenant_id", tenantId);
        appendTextLike(sql, params, "e.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "e.product_key", productName);
        appendTextLike(sql, params, "e.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "e.product_key", "e.device_code", keyword);
        sql.append("""
                    GROUP BY e.device_code
                ) latest_devices
                """);
        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result == null ? 0L : result;
    }

    private List<DevicePageVO> listFromInvalidStateMergedSources(Long tenantId, String keyword, String productKey, String productName, String deviceCode, long offset, long limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  merged.source_record_id,
                  merged.device_code,
                  merged.product_key,
                  merged.protocol_code,
                  merged.asset_source_type,
                  merged.failure_stage,
                  merged.error_message,
                  merged.topic,
                  merged.trace_id,
                  merged.payload,
                  merged.report_time
                FROM (
                """);
        appendInvalidStateSelection(sql, params, tenantId, keyword, productKey, productName, deviceCode);
        sql.append("""
                    UNION ALL
                """);
        appendDispatchFailureLatestSelectionExcludingInvalidState(sql, params, tenantId, keyword, productKey, productName, deviceCode);
        sql.append("""
                ) merged
                ORDER BY merged.report_time DESC, merged.source_record_id DESC
                LIMIT ? OFFSET ?
                """);
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapMergedRow(rs), params.toArray());
    }

    private long countFromMergedSources(Long tenantId, String keyword, String productKey, String productName, String deviceCode) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM (
                """);
        appendAccessErrorLatestSelection(sql, params, tenantId, keyword, productKey, productName, deviceCode);
        sql.append("""
                    UNION ALL
                """);
        appendDispatchFailureLatestSelection(sql, params, tenantId, keyword, productKey, productName, deviceCode, true);
        sql.append("""
                ) merged_devices
                """);
        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result == null ? 0L : result;
    }

    private List<DevicePageVO> listFromAccessError(Long tenantId, String keyword, String productKey, String productName, String deviceCode, long offset, long limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  e.id AS source_record_id,
                  e.device_code,
                  e.product_key,
                  e.protocol_code,
                  e.failure_stage,
                  e.error_message,
                  e.topic,
                  e.trace_id,
                  e.raw_payload,
                  e.create_time
                FROM iot_device_access_error_log e
                INNER JOIN (
                    SELECT MAX(e2.id) AS latest_id
                    FROM iot_device_access_error_log e2
                    LEFT JOIN iot_device d2
                      ON d2.device_code = e2.device_code
                     AND d2.deleted = 0
                    WHERE e2.deleted = 0
                      AND e2.device_code IS NOT NULL
                      AND TRIM(e2.device_code) <> ''
                      AND d2.id IS NULL
                """);
        appendTenantEquals(sql, params, "e2.tenant_id", tenantId);
        appendTextLike(sql, params, "e2.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "e2.product_key", productName);
        appendTextLike(sql, params, "e2.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "e2.product_key", "e2.device_code", keyword);
        sql.append("""
                    GROUP BY e2.device_code
                ) latest ON latest.latest_id = e.id
                ORDER BY e.create_time DESC, e.id DESC
                LIMIT ? OFFSET ?
                """);
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapAccessErrorRow(rs), params.toArray());
    }

    private List<DevicePageVO> listFromMergedSources(Long tenantId, String keyword, String productKey, String productName, String deviceCode, long offset, long limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  merged.source_record_id,
                  merged.device_code,
                  merged.product_key,
                  merged.protocol_code,
                  merged.asset_source_type,
                  merged.failure_stage,
                  merged.error_message,
                  merged.topic,
                  merged.trace_id,
                  merged.payload,
                  merged.report_time
                FROM (
                """);
        appendAccessErrorLatestSelection(sql, params, tenantId, keyword, productKey, productName, deviceCode);
        sql.append("""
                    UNION ALL
                """);
        appendDispatchFailureLatestSelection(sql, params, tenantId, keyword, productKey, productName, deviceCode, true);
        sql.append("""
                ) merged
                ORDER BY merged.report_time DESC, merged.source_record_id DESC
                LIMIT ? OFFSET ?
                """);
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapMergedRow(rs), params.toArray());
    }

    private long countFromDispatchFailure(Long tenantId, String keyword, String productKey, String productName, String deviceCode) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM (
                    SELECT m.device_code
                    FROM iot_device_message_log m
                    LEFT JOIN iot_device d
                      ON d.device_code = m.device_code
                     AND d.deleted = 0
                    WHERE m.device_id = ?
                      AND m.message_type = ?
                      AND m.device_code IS NOT NULL
                      AND TRIM(m.device_code) <> ''
                      AND d.id IS NULL
                """);
        params.add(UNKNOWN_DEVICE_ID);
        params.add(DISPATCH_FAILED_MESSAGE_TYPE);
        appendTenantEquals(sql, params, "m.tenant_id", tenantId);
        appendTextLike(sql, params, "m.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "m.product_key", productName);
        appendTextLike(sql, params, "m.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "m.product_key", "m.device_code", keyword);
        sql.append("""
                    GROUP BY m.device_code
                ) latest_devices
                """);
        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result == null ? 0L : result;
    }

    private void appendAccessErrorLatestSelection(StringBuilder sql,
                                                  List<Object> params,
                                                  Long tenantId,
                                                  String keyword,
                                                  String productKey,
                                                  String productName,
                                                  String deviceCode) {
        sql.append("""
                SELECT
                  e.id AS source_record_id,
                  e.device_code,
                  e.product_key,
                  e.protocol_code,
                  'access_error' AS asset_source_type,
                  e.failure_stage,
                  e.error_message,
                  e.topic,
                  e.trace_id,
                  e.raw_payload AS payload,
                  e.create_time AS report_time
                FROM iot_device_access_error_log e
                INNER JOIN (
                    SELECT MAX(e2.id) AS latest_id
                    FROM iot_device_access_error_log e2
                    LEFT JOIN iot_device d2
                      ON d2.device_code = e2.device_code
                     AND d2.deleted = 0
                    WHERE e2.deleted = 0
                      AND e2.device_code IS NOT NULL
                      AND TRIM(e2.device_code) <> ''
                      AND d2.id IS NULL
                """);
        appendTenantEquals(sql, params, "e2.tenant_id", tenantId);
        appendTextLike(sql, params, "e2.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "e2.product_key", productName);
        appendTextLike(sql, params, "e2.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "e2.product_key", "e2.device_code", keyword);
        sql.append("""
                    GROUP BY e2.device_code
                ) latest ON latest.latest_id = e.id
                """);
    }

    private void appendInvalidStateSelection(StringBuilder sql,
                                             List<Object> params,
                                             Long tenantId,
                                             String keyword,
                                             String productKey,
                                             String productName,
                                             String deviceCode) {
        sql.append("""
                SELECT
                  s.id AS source_record_id,
                  s.device_code,
                  s.product_key,
                  s.protocol_code,
                  'invalid_report_state' AS asset_source_type,
                  s.failure_stage,
                  s.sample_error_message AS error_message,
                  s.topic,
                  s.last_trace_id AS trace_id,
                  s.last_payload AS payload,
                  s.last_seen_time AS report_time
                FROM iot_device_invalid_report_state s
                LEFT JOIN iot_device d
                  ON d.device_code = s.device_code
                 AND d.deleted = 0
                WHERE s.deleted = 0
                  AND s.resolved = 0
                  AND s.reason_code = 'DEVICE_NOT_FOUND'
                  AND s.device_code IS NOT NULL
                  AND TRIM(s.device_code) <> ''
                  AND d.id IS NULL
                """);
        appendTenantEquals(sql, params, "s.tenant_id", tenantId);
        appendTextLike(sql, params, "s.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "s.product_key", productName);
        appendTextLike(sql, params, "s.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "s.product_key", "s.device_code", keyword);
    }

    private List<DevicePageVO> listFromDispatchFailure(Long tenantId, String keyword, String productKey, String productName, String deviceCode, long offset, long limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  m.id AS source_record_id,
                  m.device_code,
                  m.product_key,
                  m.topic,
                  m.trace_id,
                  m.payload,
                  COALESCE(m.report_time, m.create_time) AS report_time
                FROM iot_device_message_log m
                INNER JOIN (
                    SELECT MAX(m2.id) AS latest_id
                    FROM iot_device_message_log m2
                    LEFT JOIN iot_device d2
                      ON d2.device_code = m2.device_code
                     AND d2.deleted = 0
                    WHERE m2.device_id = ?
                      AND m2.message_type = ?
                      AND m2.device_code IS NOT NULL
                      AND TRIM(m2.device_code) <> ''
                      AND d2.id IS NULL
                """);
        params.add(UNKNOWN_DEVICE_ID);
        params.add(DISPATCH_FAILED_MESSAGE_TYPE);
        appendTenantEquals(sql, params, "m2.tenant_id", tenantId);
        appendTextLike(sql, params, "m2.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "m2.product_key", productName);
        appendTextLike(sql, params, "m2.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "m2.product_key", "m2.device_code", keyword);
        sql.append("""
                    GROUP BY m2.device_code
                ) latest ON latest.latest_id = m.id
                ORDER BY COALESCE(m.report_time, m.create_time) DESC, m.id DESC
                LIMIT ? OFFSET ?
                """);
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapDispatchFailureRow(rs), params.toArray());
    }

    private void appendDispatchFailureLatestSelection(StringBuilder sql,
                                                      List<Object> params,
                                                      Long tenantId,
                                                      String keyword,
                                                      String productKey,
                                                      String productName,
                                                      String deviceCode,
                                                      boolean excludeAccessErrorDevices) {
        sql.append("""
                SELECT
                  m.id AS source_record_id,
                  m.device_code,
                  m.product_key,
                  NULL AS protocol_code,
                  'dispatch_failed' AS asset_source_type,
                  'message_dispatch' AS failure_stage,
                  '未登记设备最近一次上报已记录到失败轨迹。' AS error_message,
                  m.topic,
                  m.trace_id,
                  m.payload,
                  COALESCE(m.report_time, m.create_time) AS report_time
                FROM iot_device_message_log m
                INNER JOIN (
                    SELECT MAX(m2.id) AS latest_id
                    FROM iot_device_message_log m2
                    LEFT JOIN iot_device d2
                      ON d2.device_code = m2.device_code
                     AND d2.deleted = 0
                    WHERE m2.device_id = ?
                      AND m2.message_type = ?
                      AND m2.device_code IS NOT NULL
                      AND TRIM(m2.device_code) <> ''
                      AND d2.id IS NULL
                """);
        params.add(UNKNOWN_DEVICE_ID);
        params.add(DISPATCH_FAILED_MESSAGE_TYPE);
        appendTenantEquals(sql, params, "m2.tenant_id", tenantId);
        appendTextLike(sql, params, "m2.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "m2.product_key", productName);
        appendTextLike(sql, params, "m2.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "m2.product_key", "m2.device_code", keyword);
        sql.append("""
                    GROUP BY m2.device_code
                ) latest ON latest.latest_id = m.id
                """);
        if (!excludeAccessErrorDevices) {
            return;
        }
        sql.append("""
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM iot_device_access_error_log e3
                    LEFT JOIN iot_device d3
                      ON d3.device_code = e3.device_code
                     AND d3.deleted = 0
                    WHERE e3.deleted = 0
                      AND e3.device_code IS NOT NULL
                      AND TRIM(e3.device_code) <> ''
                      AND d3.id IS NULL
                      AND e3.device_code = m.device_code
                """);
        appendTenantEquals(sql, params, "e3.tenant_id", tenantId);
        appendTextLike(sql, params, "e3.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "e3.product_key", productName);
        appendTextLike(sql, params, "e3.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "e3.product_key", "e3.device_code", keyword);
        sql.append("""
                )
                """);
    }

    private void appendDispatchFailureLatestSelectionExcludingInvalidState(StringBuilder sql,
                                                                           List<Object> params,
                                                                           Long tenantId,
                                                                           String keyword,
                                                                           String productKey,
                                                                           String productName,
                                                                           String deviceCode) {
        sql.append("""
                SELECT
                  m.id AS source_record_id,
                  m.device_code,
                  m.product_key,
                  NULL AS protocol_code,
                  'dispatch_failed' AS asset_source_type,
                  'message_dispatch' AS failure_stage,
                  '未登记设备最近一次上报已记录到失败轨迹。' AS error_message,
                  m.topic,
                  m.trace_id,
                  m.payload,
                  COALESCE(m.report_time, m.create_time) AS report_time
                FROM iot_device_message_log m
                INNER JOIN (
                    SELECT MAX(m2.id) AS latest_id
                    FROM iot_device_message_log m2
                    LEFT JOIN iot_device d2
                      ON d2.device_code = m2.device_code
                     AND d2.deleted = 0
                    WHERE m2.device_id = ?
                      AND m2.message_type = ?
                      AND m2.device_code IS NOT NULL
                      AND TRIM(m2.device_code) <> ''
                      AND d2.id IS NULL
                """);
        params.add(UNKNOWN_DEVICE_ID);
        params.add(DISPATCH_FAILED_MESSAGE_TYPE);
        appendTenantEquals(sql, params, "m2.tenant_id", tenantId);
        appendTextLike(sql, params, "m2.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "m2.product_key", productName);
        appendTextLike(sql, params, "m2.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "m2.product_key", "m2.device_code", keyword);
        sql.append("""
                    GROUP BY m2.device_code
                ) latest ON latest.latest_id = m.id
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM iot_device_invalid_report_state s2
                    LEFT JOIN iot_device d3
                      ON d3.device_code = s2.device_code
                     AND d3.deleted = 0
                    WHERE s2.deleted = 0
                      AND s2.resolved = 0
                      AND s2.reason_code = 'DEVICE_NOT_FOUND'
                      AND s2.device_code IS NOT NULL
                      AND TRIM(s2.device_code) <> ''
                      AND d3.id IS NULL
                      AND s2.device_code = m.device_code
                """);
        appendTenantEquals(sql, params, "s2.tenant_id", tenantId);
        appendTextLike(sql, params, "s2.product_key", productKey);
        appendProductNameLike(sql, params, tenantId, "s2.product_key", productName);
        appendTextLike(sql, params, "s2.device_code", deviceCode);
        appendKeywordLike(sql, params, tenantId, "s2.product_key", "s2.device_code", keyword);
        sql.append("""
                )
                """);
    }

    private void appendTenantEquals(StringBuilder sql, List<Object> params, String column, Long tenantId) {
        if (tenantId == null) {
            return;
        }
        sql.append(" AND ").append(column).append(" = ?");
        params.add(tenantId);
    }

    private void appendTextLike(StringBuilder sql, List<Object> params, String column, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sql.append(" AND ").append(column).append(" LIKE ?");
        params.add("%" + value.trim() + "%");
    }

    private void appendProductNameLike(StringBuilder sql, List<Object> params, Long tenantId, String productKeyColumn, String productName) {
        if (!StringUtils.hasText(productName)) {
            return;
        }
        sql.append(" AND EXISTS (SELECT 1 FROM ").append(PRODUCT_TABLE).append(" p WHERE p.deleted = 0");
        appendTenantEquals(sql, params, "p.tenant_id", tenantId);
        sql.append(" AND p.product_key = ").append(productKeyColumn);
        sql.append(" AND p.product_name LIKE ?)");
        params.add("%" + productName.trim() + "%");
    }

    private void appendKeywordLike(StringBuilder sql, List<Object> params, Long tenantId, String productKeyColumn, String deviceCodeColumn, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        sql.append(" AND (")
                .append(deviceCodeColumn).append(" LIKE ?")
                .append(" OR ").append(productKeyColumn).append(" LIKE ?")
                .append(" OR EXISTS (SELECT 1 FROM ").append(PRODUCT_TABLE).append(" p WHERE p.deleted = 0");
        appendTenantEquals(sql, params, "p.tenant_id", tenantId);
        sql.append(" AND p.product_key = ").append(productKeyColumn);
        sql.append(" AND p.product_name LIKE ?))");
        params.add("%" + keyword.trim() + "%");
        params.add("%" + keyword.trim() + "%");
        params.add("%" + keyword.trim() + "%");
    }

    private DevicePageVO mapAccessErrorRow(ResultSet rs) throws SQLException {
        LocalDateTime reportTime = getLocalDateTime(rs, "create_time");
        DevicePageVO row = buildUnregisteredBaseRow(rs.getString("device_code"), rs.getString("product_key"), reportTime);
        row.setProtocolCode(rs.getString("protocol_code"));
        row.setAssetSourceType(ACCESS_ERROR_SOURCE);
        row.setSourceRecordId(getLong(rs, "source_record_id"));
        row.setLastFailureStage(rs.getString("failure_stage"));
        row.setLastErrorMessage(rs.getString("error_message"));
        row.setLastReportTopic(rs.getString("topic"));
        row.setLastTraceId(rs.getString("trace_id"));
        row.setLastPayload(rs.getString("raw_payload"));
        return row;
    }

    private DevicePageVO mapDispatchFailureRow(ResultSet rs) throws SQLException {
        LocalDateTime reportTime = getLocalDateTime(rs, "report_time");
        DevicePageVO row = buildUnregisteredBaseRow(rs.getString("device_code"), rs.getString("product_key"), reportTime);
        row.setAssetSourceType(DISPATCH_FAILED_SOURCE);
        row.setSourceRecordId(getLong(rs, "source_record_id"));
        row.setLastFailureStage(MESSAGE_DISPATCH_FAILURE_STAGE);
        row.setLastErrorMessage(DISPATCH_FAILED_MESSAGE);
        row.setLastReportTopic(rs.getString("topic"));
        row.setLastTraceId(rs.getString("trace_id"));
        row.setLastPayload(rs.getString("payload"));
        return row;
    }

    private DevicePageVO mapMergedRow(ResultSet rs) throws SQLException {
        LocalDateTime reportTime = getLocalDateTime(rs, "report_time");
        DevicePageVO row = buildUnregisteredBaseRow(rs.getString("device_code"), rs.getString("product_key"), reportTime);
        row.setProtocolCode(rs.getString("protocol_code"));
        row.setAssetSourceType(rs.getString("asset_source_type"));
        row.setSourceRecordId(getLong(rs, "source_record_id"));
        row.setLastFailureStage(rs.getString("failure_stage"));
        row.setLastErrorMessage(rs.getString("error_message"));
        row.setLastReportTopic(rs.getString("topic"));
        row.setLastTraceId(rs.getString("trace_id"));
        row.setLastPayload(rs.getString("payload"));
        return row;
    }

    private DevicePageVO buildUnregisteredBaseRow(String deviceCode, String productKey, LocalDateTime reportTime) {
        DevicePageVO row = new DevicePageVO();
        row.setId(null);
        row.setProductId(null);
        row.setGatewayId(null);
        row.setParentDeviceId(null);
        row.setProductKey(productKey);
        row.setProductName(null);
        row.setGatewayDeviceCode(null);
        row.setGatewayDeviceName(null);
        row.setParentDeviceCode(null);
        row.setParentDeviceName(null);
        row.setDeviceName(UNREGISTERED_DEVICE_NAME);
        row.setDeviceCode(deviceCode);
        row.setNodeType(null);
        row.setOnlineStatus(null);
        row.setActivateStatus(null);
        row.setDeviceStatus(null);
        row.setFirmwareVersion(null);
        row.setIpAddress(null);
        row.setAddress(null);
        row.setLastOnlineTime(null);
        row.setLastOfflineTime(null);
        row.setLastReportTime(reportTime);
        row.setCreateTime(reportTime);
        row.setUpdateTime(reportTime);
        row.setRegistrationStatus(0);
        return row;
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
