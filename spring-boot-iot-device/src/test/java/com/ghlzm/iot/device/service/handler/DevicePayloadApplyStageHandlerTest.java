package com.ghlzm.iot.device.service.handler;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
import com.ghlzm.iot.device.service.model.DevicePayloadApplyResult;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Test
    void applyShouldCaptureRuntimeProtocolEvidenceEvenWhenNoLatestPropertiesAreWritten() {
        DevicePayloadApplyStageHandler handler = new DevicePayloadApplyStageHandler(
                devicePropertyMapper,
                devicePropertyMetadataService,
                commandRecordService,
                deviceFileService,
                productMetricEvidenceService
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
}
