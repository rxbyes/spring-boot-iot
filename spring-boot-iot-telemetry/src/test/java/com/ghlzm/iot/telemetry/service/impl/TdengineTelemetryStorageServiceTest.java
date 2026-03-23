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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
        assertEquals(2001L, argsCaptor.getAllValues().get(0)[2]);
    }

    @Test
    void listLatestPointsShouldMapTdengineRows() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<TelemetryLatestPoint> rowMapper = invocation.getArgument(1);
                    org.mockito.stubbing.Answer<TelemetryLatestPoint> unused = null;
                    org.mockito.MockedStatic<Timestamp> ignored = null;
                    var resultSet = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(resultSet.getTimestamp("ts")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0)));
                    when(resultSet.getString("device_code")).thenReturn("demo-device-01");
                    when(resultSet.getString("product_key")).thenReturn("demo-product");
                    when(resultSet.getString("metric_code")).thenReturn("temperature");
                    when(resultSet.getString("metric_name")).thenReturn("温度");
                    when(resultSet.getString("value_type")).thenReturn("double");
                    when(resultSet.getString("value_text")).thenReturn("26.5");
                    when(resultSet.getObject("value_long")).thenReturn(null);
                    when(resultSet.getObject("value_double")).thenReturn(26.5D);
                    when(resultSet.getObject("value_bool")).thenReturn(null);
                    when(resultSet.getString("trace_id")).thenReturn("trace-001");
                    return List.of(rowMapper.mapRow(resultSet, 0));
                });

        List<TelemetryLatestPoint> latestPoints = tdengineTelemetryStorageService.listLatestPoints(2001L);

        assertEquals(1, latestPoints.size());
        assertEquals("temperature", latestPoints.get(0).getMetricCode());
        assertEquals(26.5D, latestPoints.get(0).getValue());
        verify(tdengineTelemetrySchemaSupport).ensureTable();
    }

    private DeviceProcessingTarget buildTarget(Map<String, Object> properties) {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceUpMessage message = new DeviceUpMessage();
        message.setProductKey("demo-product");
        message.setDeviceCode("demo-device-01");
        message.setProtocolCode("mqtt-json");
        message.setTraceId("trace-001");
        message.setMessageType("property");
        message.setTopic("/sys/demo-product/demo-device-01/thing/property/post");
        message.setTimestamp(LocalDateTime.of(2026, 3, 23, 10, 0));
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
