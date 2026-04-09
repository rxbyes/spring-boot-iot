package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.vo.DevicePageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnregisteredDeviceRosterServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DeviceAccessErrorLogSchemaSupport schemaSupport;
    @Mock
    private DeviceInvalidReportStateSchemaSupport invalidReportStateSchemaSupport;

    private UnregisteredDeviceRosterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnregisteredDeviceRosterServiceImpl(jdbcTemplate, schemaSupport, invalidReportStateSchemaSupport);
        lenient().when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of(
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
        )));
        lenient().when(invalidReportStateSchemaSupport.getColumns()).thenReturn(new LinkedHashSet<>());
    }

    @Test
    void countByFiltersShouldIncludeDispatchOnlyDevicesWhenAccessErrorSchemaAvailable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("UNION ALL")) {
                        return 2L;
                    }
                    if (sql.contains("iot_device_message_log")) {
                        return 2L;
                    }
                    return 1L;
                });

        long total = service.countByFilters(null, "obs-product", null, null);

        assertEquals(2L, total);
    }

    @Test
    void listByFiltersShouldPreferAccessErrorAndAppendDispatchOnlyDevices() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DevicePageVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    @SuppressWarnings("unchecked")
                    RowMapper<DevicePageVO> rowMapper = invocation.getArgument(1, RowMapper.class);
                    if (sql.contains("UNION ALL")) {
                        return List.of(
                                rowMapper.mapRow(mockMergedResultSet(
                                        2036009677345259521L,
                                        "shadow-dup",
                                        "obs-product",
                                        "mqtt-json",
                                        "access_error",
                                        "device_validate",
                                        "设备未登记: shadow-dup",
                                        "$dp",
                                        "trace-access",
                                        "{\"code\":\"shadow-dup\"}",
                                        LocalDateTime.of(2026, 3, 27, 21, 38, 0)
                                ), 0),
                                rowMapper.mapRow(mockMergedResultSet(
                                        2036009677345259522L,
                                        "dispatch-only-01",
                                        "obs-product",
                                        null,
                                        "dispatch_failed",
                                        "message_dispatch",
                                        "未登记设备最近一次上报已记录到失败轨迹。",
                                        "$dp",
                                        "trace-dispatch-only",
                                        "{\"code\":\"dispatch-only-01\"}",
                                        LocalDateTime.of(2026, 3, 27, 21, 37, 0)
                                ), 1)
                        );
                    }
                    if (sql.contains("iot_device_access_error_log")) {
                        return List.of(rowMapper.mapRow(mockAccessErrorResultSet(
                                2036009677345259521L,
                                "shadow-dup",
                                "obs-product",
                                "mqtt-json",
                                "device_validate",
                                "设备未登记: shadow-dup",
                                "$dp",
                                "trace-access",
                                "{\"code\":\"shadow-dup\"}",
                                LocalDateTime.of(2026, 3, 27, 21, 38, 0)
                        ), 0));
                    }
                    return List.of(
                            rowMapper.mapRow(mockDispatchFailureResultSet(
                                    2036009677345259510L,
                                    "shadow-dup",
                                    "obs-product",
                                    "$dp",
                                    "trace-duplicate-dispatch",
                                    "{\"code\":\"shadow-dup\"}",
                                    LocalDateTime.of(2026, 3, 27, 21, 39, 0)
                            ), 0),
                            rowMapper.mapRow(mockDispatchFailureResultSet(
                                    2036009677345259522L,
                                    "dispatch-only-01",
                                    "obs-product",
                                    "$dp",
                                    "trace-dispatch-only",
                                    "{\"code\":\"dispatch-only-01\"}",
                                    LocalDateTime.of(2026, 3, 27, 21, 37, 0)
                            ), 1)
                    );
                });

        List<DevicePageVO> records = service.listByFilters(null, "obs-product", null, null, 0L, 10L);

        assertEquals(List.of("shadow-dup", "dispatch-only-01"), records.stream().map(DevicePageVO::getDeviceCode).toList());
        assertEquals(List.of("access_error", "dispatch_failed"), records.stream().map(DevicePageVO::getAssetSourceType).toList());
        assertEquals("device_validate", records.get(0).getLastFailureStage());
        assertEquals("message_dispatch", records.get(1).getLastFailureStage());
    }

    @Test
    void listByFiltersShouldPreferInvalidReportStateForDeviceNotFoundRows() {
        when(invalidReportStateSchemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of(
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
        )));
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DevicePageVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    @SuppressWarnings("unchecked")
                    RowMapper<DevicePageVO> rowMapper = invocation.getArgument(1, RowMapper.class);
                    if (sql.contains("iot_device_invalid_report_state")) {
                        return List.of(rowMapper.mapRow(mockInvalidStateResultSet(
                                2036009677345259601L,
                                "missing-01",
                                "obs-product",
                                "device_validate",
                                "设备不存在: missing-01",
                                "$dp",
                                "trace-state-001",
                                "{\"deviceCode\":\"missing-01\"}",
                                LocalDateTime.of(2026, 3, 27, 22, 10, 0)
                        ), 0));
                    }
                    return List.of();
                });

        List<DevicePageVO> records = service.listByFilters(null, "obs-product", null, "missing", 0L, 10L);

        assertEquals(1, records.size());
        assertEquals("invalid_report_state", records.get(0).getAssetSourceType());
        assertEquals("missing-01", records.get(0).getDeviceCode());
    }

    private ResultSet mockMergedResultSet(Long sourceRecordId,
                                          String deviceCode,
                                          String productKey,
                                          String protocolCode,
                                          String assetSourceType,
                                          String failureStage,
                                          String errorMessage,
                                          String topic,
                                          String traceId,
                                          String payload,
                                          LocalDateTime reportTime) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("source_record_id")).thenReturn(sourceRecordId);
        when(rs.wasNull()).thenReturn(sourceRecordId == null);
        when(rs.getString("device_code")).thenReturn(deviceCode);
        when(rs.getString("product_key")).thenReturn(productKey);
        when(rs.getString("protocol_code")).thenReturn(protocolCode);
        when(rs.getString("asset_source_type")).thenReturn(assetSourceType);
        when(rs.getString("failure_stage")).thenReturn(failureStage);
        when(rs.getString("error_message")).thenReturn(errorMessage);
        when(rs.getString("topic")).thenReturn(topic);
        when(rs.getString("trace_id")).thenReturn(traceId);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getTimestamp("report_time")).thenReturn(Timestamp.valueOf(reportTime));
        return rs;
    }

    private ResultSet mockAccessErrorResultSet(Long sourceRecordId,
                                               String deviceCode,
                                               String productKey,
                                               String protocolCode,
                                               String failureStage,
                                               String errorMessage,
                                               String topic,
                                               String traceId,
                                               String payload,
                                               LocalDateTime createTime) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("source_record_id")).thenReturn(sourceRecordId);
        when(rs.wasNull()).thenReturn(sourceRecordId == null);
        when(rs.getString("device_code")).thenReturn(deviceCode);
        when(rs.getString("product_key")).thenReturn(productKey);
        when(rs.getString("protocol_code")).thenReturn(protocolCode);
        when(rs.getString("failure_stage")).thenReturn(failureStage);
        when(rs.getString("error_message")).thenReturn(errorMessage);
        when(rs.getString("topic")).thenReturn(topic);
        when(rs.getString("trace_id")).thenReturn(traceId);
        when(rs.getString("raw_payload")).thenReturn(payload);
        when(rs.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(createTime));
        return rs;
    }

    private ResultSet mockDispatchFailureResultSet(Long sourceRecordId,
                                                   String deviceCode,
                                                   String productKey,
                                                   String topic,
                                                   String traceId,
                                                   String payload,
                                                   LocalDateTime reportTime) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("source_record_id")).thenReturn(sourceRecordId);
        when(rs.wasNull()).thenReturn(sourceRecordId == null);
        when(rs.getString("device_code")).thenReturn(deviceCode);
        when(rs.getString("product_key")).thenReturn(productKey);
        when(rs.getString("topic")).thenReturn(topic);
        when(rs.getString("trace_id")).thenReturn(traceId);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getTimestamp("report_time")).thenReturn(Timestamp.valueOf(reportTime));
        return rs;
    }

    private ResultSet mockInvalidStateResultSet(Long sourceRecordId,
                                                String deviceCode,
                                                String productKey,
                                                String failureStage,
                                                String errorMessage,
                                                String topic,
                                                String traceId,
                                                String payload,
                                                LocalDateTime reportTime) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("source_record_id")).thenReturn(sourceRecordId);
        when(rs.wasNull()).thenReturn(sourceRecordId == null);
        when(rs.getString("device_code")).thenReturn(deviceCode);
        when(rs.getString("product_key")).thenReturn(productKey);
        when(rs.getString("protocol_code")).thenReturn("mqtt-json");
        when(rs.getString("asset_source_type")).thenReturn("invalid_report_state");
        when(rs.getString("failure_stage")).thenReturn(failureStage);
        when(rs.getString("error_message")).thenReturn(errorMessage);
        when(rs.getString("topic")).thenReturn(topic);
        when(rs.getString("trace_id")).thenReturn(traceId);
        when(rs.getString("payload")).thenReturn(payload);
        when(rs.getTimestamp("report_time")).thenReturn(Timestamp.valueOf(reportTime));
        return rs;
    }
}
