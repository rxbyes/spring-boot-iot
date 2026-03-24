package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TdengineTelemetryStorageServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private TdengineTelemetrySchemaSupport tdengineTelemetrySchemaSupport;
    @Mock
    private DevicePropertyMetadataService devicePropertyMetadataService;

    private TdengineTelemetryStorageService tdengineTelemetryStorageService;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        tdengineTelemetryStorageService = new TdengineTelemetryStorageService(
                jdbcTemplateProvider,
                tdengineTelemetrySchemaSupport,
                devicePropertyMetadataService
        );
    }

    @Test
    void persistShouldWriteOneRowPerProperty() {
        DeviceProcessingTarget target = buildTarget(Map.of("temperature", 26.5, "humidity", 68L));
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "temperature", metadata("temperature", "温度", "double"),
                "humidity", metadata("humidity", "湿度", "int")
        ));

        TelemetryPersistResult result = tdengineTelemetryStorageService.persist(target);

        assertEquals(2, result.getPointCount());
        verify(tdengineTelemetrySchemaSupport).ensureTable();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture(), argsCaptor.capture());
        assertEquals(2, argsCaptor.getAllValues().size());
        assertNotNull(argsCaptor.getAllValues().get(0)[0]);
        assertEquals(argsCaptor.getAllValues().get(0)[1], argsCaptor.getAllValues().get(1)[1]);
        assertNotEquals(argsCaptor.getAllValues().get(0)[0], argsCaptor.getAllValues().get(1)[0]);
        assertEquals(2001L, argsCaptor.getAllValues().get(0)[3]);
    }

    @Test
    void persistShouldGenerateDistinctStorageTimestampsAcrossTargetsWithSameReportedAt() {
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "dispsX", metadata("dispsX", "X位移", "double"),
                "dispsY", metadata("dispsY", "Y位移", "double")
        ));

        LocalDateTime reportedAt = LocalDateTime.of(2026, 3, 24, 15, 4, 2);
        tdengineTelemetryStorageService.persist(buildTarget(
                2001L,
                "84330701",
                "trace-split-001",
                reportedAt,
                Map.of("dispsX", -0.0445D, "dispsY", 0.0293D)
        ));
        tdengineTelemetryStorageService.persist(buildTarget(
                2002L,
                "84330695",
                "trace-split-001",
                reportedAt,
                Map.of("dispsX", -0.0293D, "dispsY", 0.0330D)
        ));

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, times(4)).update(anyString(), argsCaptor.capture());

        Set<Object> storageTimestamps = new LinkedHashSet<>();
        for (Object[] args : argsCaptor.getAllValues()) {
            storageTimestamps.add(args[0]);
        }
        assertEquals(4, storageTimestamps.size(),
                "共享 fallback 表在同一 reportedAt 下也必须为每条 target 生成唯一 ts，避免后写覆盖先写");
    }

    @Test
    void listLatestPointsShouldQueryByReportedAtBeforeStorageTimestamp() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), anyLong()))
                .thenReturn(List.of());

        tdengineTelemetryStorageService.listLatestPoints(2001L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq(2001L));
        assertTrue(sqlCaptor.getValue().contains("p.reported_at DESC"));
        assertTrue(sqlCaptor.getValue().contains("p.ts DESC"));
    }

    @Test
    void listLatestPointsShouldMapTdengineRows() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class), anyLong()))
                .thenAnswer(invocation -> {
                    var resultSet = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(resultSet.next()).thenReturn(true, true, true, false);
                    when(resultSet.getTimestamp("reported_at")).thenReturn(
                            Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0)),
                            Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0)),
                            Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0))
                    );
                    when(resultSet.getTimestamp("ts")).thenReturn(
                            Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0, 0, 0)),
                            Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0, 0, 1_000_000)),
                            Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 9, 59, 59, 0))
                    );
                    when(resultSet.getString("device_code")).thenReturn("demo-device-01", "demo-device-01", "demo-device-01");
                    when(resultSet.getString("product_key")).thenReturn("demo-product", "demo-product", "demo-product");
                    when(resultSet.getString("metric_code")).thenReturn("humidity", "temperature", "temperature");
                    when(resultSet.getString("metric_name")).thenReturn("湿度", "温度", "温度");
                    when(resultSet.getString("value_type")).thenReturn("int", "int", "double", "double");
                    when(resultSet.getString("value_text")).thenReturn("68", "26.5", "25.1");
                    when(resultSet.getObject("value_long")).thenReturn("68", null, null);
                    when(resultSet.getObject("value_double")).thenReturn(null, 26.5D, 25.1D);
                    when(resultSet.getObject("value_bool")).thenReturn(null, null, null);
                    when(resultSet.getString("trace_id")).thenReturn("trace-001", "trace-001", "trace-old");
                    @SuppressWarnings("unchecked")
                    org.springframework.jdbc.core.ResultSetExtractor<List<TelemetryLatestPoint>> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });

        List<TelemetryLatestPoint> latestPoints = tdengineTelemetryStorageService.listLatestPoints(2001L);

        assertEquals(2, latestPoints.size());
        assertEquals("humidity", latestPoints.get(0).getMetricCode());
        assertEquals(68, latestPoints.get(0).getValue());
        assertEquals("temperature", latestPoints.get(1).getMetricCode());
        assertEquals(26.5D, latestPoints.get(1).getValue());
        verify(tdengineTelemetrySchemaSupport).ensureTable();
    }

    private DeviceProcessingTarget buildTarget(Map<String, Object> properties) {
        return buildTarget(
                2001L,
                "demo-device-01",
                "trace-001",
                LocalDateTime.of(2026, 3, 23, 10, 0),
                properties
        );
    }

    private DeviceProcessingTarget buildTarget(Long deviceId,
                                               String deviceCode,
                                               String traceId,
                                               LocalDateTime timestamp,
                                               Map<String, Object> properties) {
        Device device = new Device();
        device.setId(deviceId);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode(deviceCode);

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceUpMessage message = new DeviceUpMessage();
        message.setProductKey("demo-product");
        message.setDeviceCode(deviceCode);
        message.setProtocolCode("mqtt-json");
        message.setTraceId(traceId);
        message.setMessageType("property");
        message.setTopic("/sys/demo-product/" + deviceCode + "/thing/property/post");
        message.setTimestamp(timestamp);
        message.setProperties(properties);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(message);
        return target;
    }

    private DevicePropertyMetadata metadata(String identifier, String propertyName, String dataType) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setIdentifier(identifier);
        metadata.setPropertyName(propertyName);
        metadata.setDataType(dataType);
        return metadata;
    }
}
