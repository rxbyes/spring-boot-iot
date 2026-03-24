package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyTdengineTelemetryWriterTest {

    @Mock
    private TdengineTelemetryJdbcTemplateProvider jdbcTemplateProvider;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private LegacyTdengineDeviceMetadataResolver deviceMetadataResolver;
    @Mock
    private DeviceTelemetryMappingService deviceTelemetryMappingService;

    private LegacyTdengineTelemetryWriter writer;

    @BeforeEach
    void setUp() {
        when(jdbcTemplateProvider.getJdbcTemplate()).thenReturn(jdbcTemplate);
        LegacyTdengineSchemaInspector schemaInspector = new LegacyTdengineSchemaInspector(jdbcTemplateProvider);
        writer = new LegacyTdengineTelemetryWriter(
                jdbcTemplateProvider,
                schemaInspector,
                deviceMetadataResolver,
                deviceTelemetryMappingService
        );
    }

    @Test
    void persistShouldWriteOneLegacyStableRowPerStableAndExposeFallbackCounts() throws Exception {
        when(jdbcTemplate.query(eq("DESCRIBE s1_zt_1"), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> {
                    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
                    when(resultSet.next()).thenReturn(true, true, true, true, true, false);
                    when(resultSet.getString(1)).thenReturn("ts", "rd", "id", "temp", "humidity");
                    when(resultSet.getString(2)).thenReturn("TIMESTAMP", "TIMESTAMP", "BIGINT", "DOUBLE", "DOUBLE");
                    @SuppressWarnings("unchecked")
                    ResultSetExtractor<LegacyTdengineSchemaInspector.LegacyTdengineTableSchema> extractor = invocation.getArgument(1);
                    return extractor.extractData(resultSet);
                });
        LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata deviceMetadata =
                new LegacyTdengineDeviceMetadataResolver.LegacyTdengineDeviceMetadata();
        deviceMetadata.setDeviceSn("SN001");
        deviceMetadata.setLocation("A01");
        when(deviceMetadataResolver.resolve(any())).thenReturn(deviceMetadata);
        when(deviceMetadataResolver.resolveSubTableName(deviceMetadata, "s1_zt_1")).thenReturn("tb_s1_zt_1_SN001");
        when(deviceTelemetryMappingService.listMetricMappings(1001L)).thenReturn(mappingMap());

        LegacyTdengineTelemetryWriter.LegacyTdenginePersistOutcome outcome = writer.persist(
                buildTarget(Map.of("temperature", 26.5D, "humidity", 68D, "noise", 4.2D)),
                Map.of("temperature", 26.5D, "humidity", 68D, "noise", 4.2D),
                metadataMap()
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).execute(sqlCaptor.capture());
        verify(jdbcTemplate).update(sqlCaptor.capture(), argsCaptor.capture());

        assertTrue(sqlCaptor.getAllValues().get(0).contains("CREATE TABLE IF NOT EXISTS tb_s1_zt_1_SN001 USING s1_zt_1"));
        assertTrue(sqlCaptor.getAllValues().get(1).contains("INSERT INTO tb_s1_zt_1_SN001"));
        assertEquals(1, outcome.getStableCount());
        assertEquals(2, outcome.getMetricCount());
        assertEquals(2, outcome.getMappedMetricCount());
        assertEquals(1, outcome.getUnmappedMetricCount());
        assertEquals(Set.of("MISSING_TDENGINE_LEGACY_MAPPING"), outcome.getFallbackReasons());
        assertEquals(5, argsCaptor.getValue().length);
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
        message.setTimestamp(LocalDateTime.of(2026, 3, 23, 10, 0));
        message.setProperties(properties);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(message);
        return target;
    }

    private Map<String, DevicePropertyMetadata> metadataMap() {
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
        metadataMap.put("temperature", metadata("temperature", "s1_zt_1", "temp"));
        metadataMap.put("humidity", metadata("humidity", "s1_zt_1", "humidity"));
        metadataMap.put("noise", metadata("noise", null, null));
        return metadataMap;
    }

    private Map<String, TelemetryMetricMapping> mappingMap() {
        Map<String, TelemetryMetricMapping> mappingMap = new LinkedHashMap<>();
        mappingMap.put("temperature", mapping("temperature", Boolean.TRUE, "s1_zt_1", "temp", null));
        mappingMap.put("humidity", mapping("humidity", Boolean.TRUE, "s1_zt_1", "humidity", null));
        mappingMap.put("noise", mapping("noise", Boolean.TRUE, null, null, "MISSING_TDENGINE_LEGACY_MAPPING"));
        return mappingMap;
    }

    private DevicePropertyMetadata metadata(String identifier, String stable, String column) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setIdentifier(identifier);
        if (stable != null && column != null) {
            DevicePropertyMetadata.TdengineLegacyMapping mapping = new DevicePropertyMetadata.TdengineLegacyMapping();
            mapping.setStable(stable);
            mapping.setColumn(column);
            metadata.setTdengineLegacyMapping(mapping);
        }
        return metadata;
    }

    private TelemetryMetricMapping mapping(String metricCode,
                                           Boolean enabled,
                                           String stable,
                                           String column,
                                           String reason) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(metricCode);
        mapping.setEnabled(enabled);
        mapping.setStable(stable);
        mapping.setColumn(column);
        mapping.setReason(reason);
        mapping.setSource("PRODUCT_SPECS_TDENGINE_LEGACY");
        return mapping;
    }
}
