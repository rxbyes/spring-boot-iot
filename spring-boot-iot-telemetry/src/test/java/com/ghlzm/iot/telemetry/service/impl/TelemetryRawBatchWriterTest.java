package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryRawBatchWriterTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TelemetryV2SchemaSupport schemaSupport;

    private TelemetryRawBatchWriter writer;

    @BeforeEach
    void setUp() {
        writer = new TelemetryRawBatchWriter(
                jdbcTemplateProvider,
                schemaSupport,
                new TelemetryV2TableNamingStrategy()
        );
    }

    @Test
    void shouldGroupPointsByTenantDeviceAndStreamKind() {
        List<TelemetryV2Point> points = writer.toPoints(buildTarget(), buildProperties(), metadataMap());

        assertEquals(3, points.size());
        assertTrue(points.stream().anyMatch(point ->
                point.getStreamKind() == TelemetryStreamKind.MEASURE
                        && "temperature".equals(point.getMetricId())));
        assertTrue(points.stream().anyMatch(point ->
                point.getStreamKind() == TelemetryStreamKind.STATUS
                        && "signal_4g".equals(point.getMetricId())));
    }

    @Test
    void shouldBatchInsertGroupedV2Points() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        List<TelemetryV2Point> points = writer.toPoints(buildTarget(), buildProperties(), metadataMap());

        writer.write(points);

        verify(schemaSupport).ensureTables();
        verify(schemaSupport).ensureChildTable(points.get(0));
        verify(jdbcTemplate, never()).update(anyString(), anyList());
        verify(jdbcTemplate, org.mockito.Mockito.times(2)).batchUpdate(anyString(), anyList());
    }

    private DeviceProcessingTarget buildTarget() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceUpMessage message = new DeviceUpMessage();
        message.setMessageType("property");
        message.setProductKey("demo-product");
        message.setProtocolCode("mqtt-json");
        message.setTraceId("trace-001");
        message.setTopic("/sys/demo-product/demo-device-01/thing/property/post");
        message.setTimestamp(LocalDateTime.of(2026, 3, 27, 9, 0));

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(message);
        return target;
    }

    private Map<String, Object> buildProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("temperature", 26.5D);
        properties.put("humidity", 68D);
        properties.put("signal_4g", 4);
        return properties;
    }

    private Map<String, DevicePropertyMetadata> metadataMap() {
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
        metadataMap.put("temperature", metadata("temperature", "温度", "decimal"));
        metadataMap.put("humidity", metadata("humidity", "湿度", "decimal"));
        metadataMap.put("signal_4g", metadata("signal_4g", "4G 信号", "integer"));
        return metadataMap;
    }

    private DevicePropertyMetadata metadata(String identifier, String name, String dataType) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setIdentifier(identifier);
        metadata.setPropertyName(name);
        metadata.setDataType(dataType);
        return metadata;
    }
}
