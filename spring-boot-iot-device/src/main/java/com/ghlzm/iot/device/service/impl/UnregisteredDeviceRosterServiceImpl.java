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

    private static final String ACCESS_ERROR_TABLE = "iot_device_access_error_log";
    private static final String MESSAGE_LOG_TABLE = "iot_device_message_log";
    private static final String DEVICE_TABLE = "iot_device";
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
    private static final String ACCESS_ERROR_SOURCE = "access_error";
    private static final String DISPATCH_FAILED_SOURCE = "dispatch_failed";
    private static final String DISPATCH_FAILED_MESSAGE_TYPE = "dispatch_failed";
    private static final Long UNKNOWN_DEVICE_ID = 0L;

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAccessErrorLogSchemaSupport accessErrorLogSchemaSupport;

    public UnregisteredDeviceRosterServiceImpl(JdbcTemplate jdbcTemplate,
                                               DeviceAccessErrorLogSchemaSupport accessErrorLogSchemaSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.accessErrorLogSchemaSupport = accessErrorLogSchemaSupport;
    }

    @Override
    public long countByFilters(String productKey, String deviceCode) {
        if (supportsAccessErrorSource()) {
            try {
                return countFromAccessError(productKey, deviceCode);
            } catch (Exception ex) {
                log.warn("统计未登记设备失败，回退失败轨迹来源, error={}", ex.getMessage());
            }
        }
        return countFromDispatchFailure(productKey, deviceCode);
    }

    @Override
    public List<DevicePageVO> listByFilters(String productKey, String deviceCode, long offset, long limit) {
        long safeOffset = Math.max(offset, 0L);
        long safeLimit = Math.max(limit, 0L);
        if (safeLimit == 0L) {
            return List.of();
        }

        if (supportsAccessErrorSource()) {
            try {
                return listFromAccessError(productKey, deviceCode, safeOffset, safeLimit);
            } catch (Exception ex) {
                log.warn("查询未登记设备失败，回退失败轨迹来源, error={}", ex.getMessage());
            }
        }
        return listFromDispatchFailure(productKey, deviceCode, safeOffset, safeLimit);
    }

    private boolean supportsAccessErrorSource() {
        return accessErrorLogSchemaSupport.getColumns().containsAll(REQUIRED_ACCESS_ERROR_COLUMNS);
    }

    private long countFromAccessError(String productKey, String deviceCode) {
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
        appendTextLike(sql, params, "e.product_key", productKey);
        appendTextLike(sql, params, "e.device_code", deviceCode);
        sql.append("""
                    GROUP BY e.device_code
                ) latest_devices
                """);
        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result == null ? 0L : result;
    }

    private List<DevicePageVO> listFromAccessError(String productKey, String deviceCode, long offset, long limit) {
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
        appendTextLike(sql, params, "e2.product_key", productKey);
        appendTextLike(sql, params, "e2.device_code", deviceCode);
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

    private long countFromDispatchFailure(String productKey, String deviceCode) {
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
        appendTextLike(sql, params, "m.product_key", productKey);
        appendTextLike(sql, params, "m.device_code", deviceCode);
        sql.append("""
                    GROUP BY m.device_code
                ) latest_devices
                """);
        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result == null ? 0L : result;
    }

    private List<DevicePageVO> listFromDispatchFailure(String productKey, String deviceCode, long offset, long limit) {
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
        appendTextLike(sql, params, "m2.product_key", productKey);
        appendTextLike(sql, params, "m2.device_code", deviceCode);
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

    private void appendTextLike(StringBuilder sql, List<Object> params, String column, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sql.append(" AND ").append(column).append(" LIKE ?");
        params.add("%" + value.trim() + "%");
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
        row.setLastFailureStage("message_dispatch");
        row.setLastErrorMessage("未登记设备最近一次上报已记录到失败轨迹。");
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
