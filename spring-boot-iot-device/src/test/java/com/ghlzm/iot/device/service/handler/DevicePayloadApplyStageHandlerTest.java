package com.ghlzm.iot.device.service.handler;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.service.impl.DefaultMetricIdentifierResolver;
import com.ghlzm.iot.device.service.model.DevicePayloadApplyResult;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicePayloadApplyStageHandlerTest {

    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private DevicePropertyMetadataService devicePropertyMetadataService;
    @Mock
    private CommandRecordService commandRecordService;
    @Mock
    private DeviceFileService deviceFileService;
    @Mock
    private ProductMetricEvidenceService productMetricEvidenceService;
    @Mock
    private VendorMetricMappingRuntimeService vendorMetricMappingRuntimeService;
    @Mock
    private PublishedProductContractSnapshotService snapshotService;
    @Mock
    private MetricIdentifierResolver metricIdentifierResolver;

    @Test
    void applyShouldCaptureRuntimeProtocolEvidenceEvenWhenNoLatestPropertiesAreWritten() {
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService,
                vendorMetricMappingRuntimeService
        );

        Product product = new Product();
        product.setId(2002L);
        product.setProductKey("south-crack-sensor-v1");
        product.setProductName("裂缝监测仪");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(1L);
        device.setProductId(2002L);
        device.setDeviceCode("CHILD-01");

        DeviceUpProtocolMetadata metadata = new DeviceUpProtocolMetadata();
        ProtocolMetricEvidence evidence = new ProtocolMetricEvidence();
        evidence.setRawIdentifier("L1_LF_1");
        evidence.setCanonicalIdentifier("value");
        evidence.setLogicalChannelCode("L1_LF_1");
        evidence.setParentDeviceCode("GW001");
        evidence.setChildDeviceCode("CHILD-01");
        evidence.setSampleValue("10.86");
        evidence.setValueType("double");
        evidence.setEvidenceOrigin("legacy_dp_child_template");
        metadata.setMetricEvidence(List.of(evidence));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("CHILD-01");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 6, 10, 0, 0));
        upMessage.setProperties(Map.of());
        upMessage.setProtocolMetadata(metadata);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);

        DevicePayloadApplyResult result = handler.apply(target);

        assertEquals("PROPERTY", result.getBranch());
        assertEquals(0, result.getSummary().get("propertyCount"));
        verify(productMetricEvidenceService).captureRuntimeEvidence(product, upMessage);
        verify(devicePropertyMapper, never()).selectOne(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void applyShouldPersistLatestPropertiesUsingCanonicalIdentifier() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(2002L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .canonicalAlias("L1_LF_1.value", "value")
                .build();
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService,
                vendorMetricMappingRuntimeService,
                snapshotService,
                metricIdentifierResolver
        );

        Product product = new Product();
        product.setId(2002L);
        product.setProductKey("nf-monitor-laser-rangefinder-v1");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(1L);
        device.setProductId(2002L);
        device.setDeviceCode("CHILD-01");

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("CHILD-01");
        upMessage.setProtocolCode("mqtt-json");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 12, 21, 30, 28));
        upMessage.setProperties(Map.of("L1_LF_1.value", 0.2136D));

        when(snapshotService.getRequiredSnapshot(2002L)).thenReturn(snapshot);
        when(metricIdentifierResolver.resolveForRuntime(snapshot, "L1_LF_1.value"))
                .thenReturn(MetricIdentifierResolution.of(
                        "L1_LF_1.value",
                        "value",
                        MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT
                ));
        when(devicePropertyMetadataService.listPropertyMetadataMap(2002L)).thenReturn(Map.of());
        when(devicePropertyMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);

        handler.apply(target);

        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).insert(propertyCaptor.capture());
        assertEquals("value", propertyCaptor.getValue().getIdentifier());
        assertEquals("0.2136", propertyCaptor.getValue().getPropertyValue());
    }

    @Test
    void applyShouldCanonicalizeBareRuntimeIdentifierWhenPublishedSuffixIsUnique() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(2005L)
                .releaseBatchId(9002L)
                .publishedIdentifier("L1_JS_1.gX")
                .publishedIdentifier("L1_JS_1.gY")
                .publishedIdentifier("L1_QJ_1.angle")
                .build();
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService,
                vendorMetricMappingRuntimeService,
                snapshotService,
                new DefaultMetricIdentifierResolver()
        );

        Product product = new Product();
        product.setId(2005L);
        product.setProductKey("zhd-monitor-multi-displacement-v1");

        Device device = new Device();
        device.setId(3005L);
        device.setTenantId(1L);
        device.setProductId(2005L);
        device.setDeviceCode("CXH15522812");

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("CXH15522812");
        upMessage.setProtocolCode("mqtt-json");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 24, 9, 0, 3));
        upMessage.setProperties(Map.of("gX", 0.95D));

        when(snapshotService.getRequiredSnapshot(2005L)).thenReturn(snapshot);
        when(devicePropertyMetadataService.listPropertyMetadataMap(2005L)).thenReturn(Map.of());
        when(devicePropertyMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);

        handler.apply(target);

        assertEquals(Map.of("L1_JS_1.gX", 0.95D), upMessage.getProperties());
        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).insert(propertyCaptor.capture());
        assertEquals("L1_JS_1.gX", propertyCaptor.getValue().getIdentifier());
        assertEquals("0.95", propertyCaptor.getValue().getPropertyValue());
    }

    @Test
    void applyShouldNormalizeRuntimePropertiesAndMetricEvidenceByVendorMappingRule() {
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService,
                vendorMetricMappingRuntimeService
        );

        Product product = new Product();
        product.setId(2002L);
        product.setProductKey("south-crack-sensor-v1");
        product.setProductName("瑁傜紳鐩戞祴浠?");
        product.setProtocolCode("mqtt-json");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(1L);
        device.setProductId(2002L);
        device.setDeviceCode("CHILD-01");

        DeviceUpProtocolMetadata metadata = new DeviceUpProtocolMetadata();
        ProtocolMetricEvidence evidence = new ProtocolMetricEvidence();
        evidence.setRawIdentifier("disp");
        evidence.setCanonicalIdentifier("disp");
        evidence.setChildDeviceCode("CHILD-01");
        evidence.setSampleValue("10.86");
        evidence.setValueType("double");
        evidence.setEvidenceOrigin("mqtt-json");
        metadata.setMetricEvidence(List.of(evidence));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("CHILD-01");
        upMessage.setProtocolCode("mqtt-json");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 6, 10, 0, 0));
        upMessage.setProperties(Map.of("disp", 10.86D));
        upMessage.setProtocolMetadata(metadata);

        when(vendorMetricMappingRuntimeService.resolveForRuntime(product, upMessage, "disp", null))
                .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(7001L, "value", "disp", null));
        when(devicePropertyMetadataService.listPropertyMetadataMap(2002L)).thenReturn(Map.of());
        when(devicePropertyMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);

        handler.apply(target);

        assertEquals(Map.of("value", 10.86D), upMessage.getProperties());
        assertEquals("value", upMessage.getProtocolMetadata().getMetricEvidence().get(0).getCanonicalIdentifier());
        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).insert(propertyCaptor.capture());
        assertEquals("value", propertyCaptor.getValue().getIdentifier());
        verify(productMetricEvidenceService).captureRuntimeEvidence(product, upMessage);
    }

    @Test
    void applyShouldDescribePrimaryTargetLatestBoundaryInSummary() {
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService,
                vendorMetricMappingRuntimeService
        );

        Product product = new Product();
        product.setId(2003L);
        product.setProductKey("south-collector-v1");

        Device device = new Device();
        device.setId(3002L);
        device.setTenantId(1L);
        device.setProductId(2003L);
        device.setDeviceCode("SK00FB0D1310195");

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("SK00FB0D1310195");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 9, 13, 47, 28));
        upMessage.setProperties(Map.of("temp", 22.5D, "humidity", 63.0D));

        when(devicePropertyMetadataService.listPropertyMetadataMap(2003L)).thenReturn(Map.of());
        when(devicePropertyMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);
        target.setChildTarget(Boolean.FALSE);

        DevicePayloadApplyResult result = handler.apply(target);

        assertEquals("SK00FB0D1310195", result.getSummary().get("targetDeviceCode"));
        assertEquals(Boolean.FALSE, result.getSummary().get("childTarget"));
        assertEquals("PRIMARY", result.getSummary().get("targetRole"));
        assertEquals(2, result.getSummary().get("latestPropertyCount"));
        assertEquals(2, result.getSummary().get("propertyCount"));
    }

    @Test
    void applyShouldDescribeChildTargetLatestBoundaryInSummary() {
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService,
                vendorMetricMappingRuntimeService
        );

        Product product = new Product();
        product.setId(2004L);
        product.setProductKey("south-deep-displacement-v1");

        Device device = new Device();
        device.setId(3003L);
        device.setTenantId(1L);
        device.setProductId(2004L);
        device.setDeviceCode("84330701");

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("84330701");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 9, 13, 47, 28));
        upMessage.setProperties(Map.of("dispsX", -0.0446D, "dispsY", 0.0293D, "sensor_state", 0));

        when(devicePropertyMetadataService.listPropertyMetadataMap(2004L)).thenReturn(Map.of());
        when(devicePropertyMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(upMessage);
        target.setChildTarget(Boolean.TRUE);

        DevicePayloadApplyResult result = handler.apply(target);

        assertEquals("84330701", result.getSummary().get("targetDeviceCode"));
        assertEquals(Boolean.TRUE, result.getSummary().get("childTarget"));
        assertEquals("CHILD", result.getSummary().get("targetRole"));
        assertEquals(3, result.getSummary().get("latestPropertyCount"));
        assertEquals(3, result.getSummary().get("propertyCount"));
    }
}
