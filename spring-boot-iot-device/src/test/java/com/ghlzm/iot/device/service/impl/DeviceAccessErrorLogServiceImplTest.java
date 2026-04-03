package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.vo.DeviceAccessErrorStatsVO;
import com.ghlzm.iot.device.vo.DeviceStatsBucketVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.invalidreport.InvalidReportCounterStore;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAccessErrorLogServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DeviceAccessErrorLogSchemaSupport schemaSupport;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private InvalidReportCounterStore invalidReportCounterStore;
    @Mock
    private PermissionService permissionService;

    private DeviceAccessErrorLogServiceImpl service;

    @BeforeEach
    void setUp() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Protocol protocol = new IotProperties.Protocol();
        protocol.setDefaultCode("mqtt-json");
        iotProperties.setProtocol(protocol);
        service = new DeviceAccessErrorLogServiceImpl(
                jdbcTemplate,
                schemaSupport,
                deviceMapper,
                productMapper,
                iotProperties,
                invalidReportCounterStore,
                permissionService
        );
    }

    @Test
    void pageLogsShouldApplyTenantFilter() {
        List<String> executedSql = new ArrayList<>();
        List<Object[]> executedArgs = new ArrayList<>();

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of("id", "tenant_id", "create_time")));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return 1L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceAccessErrorLog>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return List.of(new DeviceAccessErrorLog());
                });

        service.pageLogs(99L, new com.ghlzm.iot.device.dto.DeviceAccessErrorQuery(), 1, 10);

        assertTrue(executedSql.stream().allMatch(sql -> sql.contains("tenant_id")));
        assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains(8L)));
    }

    @Test
    void getStatsShouldUseTenantScopedSqlInsteadOfGlobalCounters() {
        List<String> executedSql = new ArrayList<>();
        List<Object[]> executedArgs = new ArrayList<>();

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of(
                "tenant_id", "create_time", "trace_id", "device_code", "failure_stage", "error_code", "exception_class", "protocol_code", "topic"
        )));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    executedSql.add(sql);
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    if (sql.contains("COUNT(DISTINCT trace_id)")) {
                        return 7L;
                    }
                    if (sql.contains("COUNT(DISTINCT device_code)")) {
                        return 4L;
                    }
                    if (sql.contains("INTERVAL 1 HOUR")) {
                        return 3L;
                    }
                    if (sql.contains("INTERVAL 24 HOUR")) {
                        return 9L;
                    }
                    return 12L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return List.of();
                });

        service.getStats(99L, new com.ghlzm.iot.device.dto.DeviceAccessErrorQuery());

        verifyNoInteractions(invalidReportCounterStore);
        assertTrue(executedSql.stream().allMatch(sql -> sql.contains("tenant_id")));
        assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains(8L)));
    }

    @Test
    void archiveMqttFailureShouldPersistContractSnapshot() {
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of("id", "contract_snapshot")));

        Device device = new Device();
        device.setDeviceCode("demo-device-01");
        device.setProductId(1001L);
        device.setProtocolCode("");
        when(deviceMapper.selectOne(any())).thenReturn(device);

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("mqtt-json");
        when(productMapper.selectById(1001L)).thenReturn(product);

        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setTopicRouteType("legacy");
        rawDeviceMessage.setProtocolCode("mqtt-json");
        rawDeviceMessage.setDeviceCode("demo-device-01");
        rawDeviceMessage.setProductKey("demo-product");

        service.archiveMqttFailure(
                "$dp",
                "{\"body\":1}".getBytes(StandardCharsets.UTF_8),
                rawDeviceMessage,
                "device_validate",
                new BizException("设备未绑定产品: demo-device-01")
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), argsCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("contract_snapshot"));
        String contractSnapshot = (String) argsCaptor.getValue()[1];
        assertTrue(contractSnapshot.contains("\"routeType\":\"legacy\""));
        assertTrue(contractSnapshot.contains("\"expectedProtocolCode\":\"mqtt-json\""));
        assertTrue(contractSnapshot.contains("\"protocolSource\":\"product-fallback\""));
    }

    @Test
    void mapRowShouldReadContractSnapshot() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(2001L);
        when(resultSet.wasNull()).thenReturn(false);
        when(resultSet.getString("contract_snapshot")).thenReturn("{\"expectedProtocolCode\":\"mqtt-json\"}");

        Method mapRow = DeviceAccessErrorLogServiceImpl.class
                .getDeclaredMethod("mapRow", ResultSet.class, Set.class);
        mapRow.setAccessible(true);

        DeviceAccessErrorLog log = (DeviceAccessErrorLog) mapRow.invoke(
                service,
                resultSet,
                new LinkedHashSet<>(List.of("id", "contract_snapshot"))
        );

        assertEquals(2001L, log.getId());
        assertEquals("{\"expectedProtocolCode\":\"mqtt-json\"}", log.getContractSnapshot());
    }

    @Test
    void getStatsShouldAggregateFailureSummary() {
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of(
                "create_time", "trace_id", "device_code", "failure_stage", "error_code", "exception_class", "protocol_code", "topic"
        )));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("COUNT(DISTINCT trace_id)")) {
                        return 7L;
                    }
                    if (sql.contains("COUNT(DISTINCT device_code)")) {
                        return 4L;
                    }
                    return 12L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("error_code")) {
                        return List.of(new DeviceStatsBucketVO("400", "400", 3L));
                    }
                    if (sql.contains("exception_class")) {
                        return List.of(new DeviceStatsBucketVO("BizException", "BizException", 2L));
                    }
                    if (sql.contains("protocol_code")) {
                        return List.of(new DeviceStatsBucketVO("mqtt-json", "mqtt-json", 6L));
                    }
                    return List.of(new DeviceStatsBucketVO("$dp", "$dp", 4L));
                });
        when(invalidReportCounterStore.sumFailureStageSince(eq("topic_route"), any(Instant.class))).thenReturn(0L);
        when(invalidReportCounterStore.sumFailureStageSince(eq("protocol_decode"), any(Instant.class))).thenReturn(5L);
        when(invalidReportCounterStore.sumFailureStageSince(eq("device_validate"), any(Instant.class))).thenReturn(4L);
        when(invalidReportCounterStore.sumFailureStageSince(eq("message_dispatch"), any(Instant.class))).thenReturn(2L);

        DeviceAccessErrorStatsVO stats = service.getStats(new com.ghlzm.iot.device.dto.DeviceAccessErrorQuery());

        assertEquals(12L, stats.getTotal());
        assertEquals(11L, stats.getRecentHourCount());
        assertEquals(11L, stats.getRecent24HourCount());
        assertEquals(7L, stats.getDistinctTraceCount());
        assertEquals(4L, stats.getDistinctDeviceCount());
        assertEquals("protocol_decode", stats.getTopFailureStages().get(0).getValue());
        assertEquals(5L, stats.getTopFailureStages().get(0).getCount());
        assertEquals("400", stats.getTopErrorCodes().get(0).getValue());
        assertEquals("BizException", stats.getTopExceptionClasses().get(0).getValue());
        assertEquals("mqtt-json", stats.getTopProtocolCodes().get(0).getValue());
        assertEquals("$dp", stats.getTopTopics().get(0).getValue());
    }

    @Test
    void listFailureStageCountsSinceShouldReadAggregatedBucketsInsteadOfDetailRows() {
        when(invalidReportCounterStore.sumFailureStageSince("protocol_decode", Instant.parse("2026-03-27T13:30:00Z"))).thenReturn(12L);
        when(invalidReportCounterStore.sumFailureStageSince("device_validate", Instant.parse("2026-03-27T13:30:00Z"))).thenReturn(9L);
        when(invalidReportCounterStore.sumFailureStageSince("topic_route", Instant.parse("2026-03-27T13:30:00Z"))).thenReturn(0L);
        when(invalidReportCounterStore.sumFailureStageSince("message_dispatch", Instant.parse("2026-03-27T13:30:00Z"))).thenReturn(0L);

        List<DeviceAccessErrorLogService.FailureStageCount> counts = service.listFailureStageCountsSince(
                Date.from(Instant.parse("2026-03-27T13:30:00Z"))
        );

        assertEquals("protocol_decode", counts.get(0).failureStage());
        assertEquals(12L, counts.get(0).failureCount());
    }
}
