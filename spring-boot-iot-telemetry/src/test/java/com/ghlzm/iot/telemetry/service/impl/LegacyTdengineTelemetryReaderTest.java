package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyTdengineTelemetryReaderTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private LegacyTdengineDeviceMetadataResolver deviceMetadataResolver;

    private LegacyTdengineTelemetryReader reader;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        LegacyTdengineSchemaInspector schemaInspector = new LegacyTdengineSchemaInspector(jdbcTemplateProvider);
        reader = new LegacyTdengineTelemetryReader(jdbcTemplateProvider, schemaInspector, deviceMetadataResolver);
    }

    @Test
    void listLatestPointsShouldReadLegacyStableRows() throws Exception {
        when(jdbcTemplate.query(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.startsWith("DESCRIBE s1_zt_1")),
                any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> {
                    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
                    when(resultSet.next()).thenReturn(true, true, true, true, true, false);
                    when(resultSet.getString(1)).thenReturn("ts", "rd", "id", "temp", "humidity");
                    when(resultSet.getString(2)).thenReturn("TIMESTAMP", "TIMESTAMP", "BIGINT", "DOUBLE", "DOUBLE");
                    @SuppressWarnings("unchecked")
                    ResultSetExtractor<LegacyTdengineSchemaInspector.LegacyTdengineTableSchema> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.startsWith("SELECT ts, rd, temp, humidity FROM s1_zt_1")),
                any(ResultSetExtractor.class),
                any(Object[].class)
        ))
                .thenAnswer(invocation -> {
                    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
                    when(resultSet.next()).thenReturn(true, false);
                    when(resultSet.getTimestamp("ts")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0)));
                    when(resultSet.getTimestamp("rd")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 3, 23, 10, 0)));
                    when(resultSet.getObject("temp")).thenReturn(26.5D);
                    when(resultSet.getObject("humidity")).thenReturn(68D);
                    @SuppressWarnings("unchecked")
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });
        LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata =
                new LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata();
        deviceMetadata.setDeviceSn("SN001");
        deviceMetadata.setLocation("A01");
        when(deviceMetadataResolver.resolve(any())).thenReturn(deviceMetadata);

        List<TelemetryLatestPoint> points = reader.listLatestPoints(buildDevice(), buildProduct(), metadataMap());

        assertEquals(2, points.size());
        assertEquals("temperature", points.get(0).getMetricCode());
        assertEquals(26.5D, points.get(0).getValue());
        assertEquals("humidity", points.get(1).getMetricCode());
        assertEquals(68.0D, points.get(1).getValue());
    }

    private Device buildDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        return device;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        return product;
    }

    private Map<String, DevicePropertyMetadata> metadataMap() {
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
        metadataMap.put("temperature", metadata("temperature", "温度", "double", "s1_zt_1", "temp"));
        metadataMap.put("humidity", metadata("humidity", "湿度", "double", "s1_zt_1", "humidity"));
        return metadataMap;
    }

    private DevicePropertyMetadata metadata(String identifier,
                                            String propertyName,
                                            String dataType,
                                            String stable,
                                            String column) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setIdentifier(identifier);
        metadata.setPropertyName(propertyName);
        metadata.setDataType(dataType);
        DevicePropertyMetadata.TdengineLegacyMapping mapping = new DevicePropertyMetadata.TdengineLegacyMapping();
        mapping.setStable(stable);
        mapping.setColumn(column);
        metadata.setTdengineLegacyMapping(mapping);
        return metadata;
    }
}
