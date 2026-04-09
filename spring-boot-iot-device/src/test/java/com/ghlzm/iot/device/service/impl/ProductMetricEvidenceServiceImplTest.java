package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.ProtocolMetricEvidence;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductMetricEvidenceServiceImplTest {

    @Mock
    private VendorMetricEvidenceMapper vendorMetricEvidenceMapper;

    @Test
    void captureRuntimeEvidenceShouldPersistOnlyRowsForCurrentTargetDeviceInCrackScenario() {
        when(vendorMetricEvidenceMapper.selectOne(any())).thenReturn(null);
        ProductMetricEvidenceServiceImpl service = new ProductMetricEvidenceServiceImpl(vendorMetricEvidenceMapper);

        Product product = new Product();
        product.setId(2002L);
        product.setProductKey("south-crack-sensor-v1");
        product.setProductName("裂缝监测仪");

        DeviceUpProtocolMetadata metadata = new DeviceUpProtocolMetadata();
        metadata.setMetricEvidence(List.of(
                protocolEvidence("L1_LF_1", "value", "L1_LF_1", "GW001", "CHILD-01", "10.86"),
                protocolEvidence("L1_LF_2", "value", "L1_LF_2", "GW001", "CHILD-02", "9.12")
        ));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("CHILD-01");
        upMessage.setTopic("$dp");
        upMessage.setTimestamp(LocalDateTime.of(2026, 4, 6, 10, 0, 0));
        upMessage.setProtocolMetadata(metadata);

        service.captureRuntimeEvidence(product, upMessage);

        ArgumentCaptor<VendorMetricEvidence> captor = ArgumentCaptor.forClass(VendorMetricEvidence.class);
        verify(vendorMetricEvidenceMapper, times(1)).insert(captor.capture());
        VendorMetricEvidence saved = captor.getValue();
        assertEquals(2002L, saved.getProductId());
        assertEquals("L1_LF_1", saved.getRawIdentifier());
        assertEquals("value", saved.getCanonicalIdentifier());
        assertEquals("L1_LF_1", saved.getLogicalChannelCode());
        assertEquals("GW001", saved.getParentDeviceCode());
        assertEquals("CHILD-01", saved.getChildDeviceCode());
        assertEquals("legacy_dp_child_template", saved.getEvidenceOrigin());
        assertEquals("10.86", saved.getSampleValue());
        assertEquals("double", saved.getValueType());
        assertEquals(1, saved.getEvidenceCount());
        assertEquals(LocalDateTime.of(2026, 4, 6, 10, 0, 0), saved.getLastSeenTime());
        assertTrue(saved.getMetadataJson().contains("phase1-crack"));
    }

    private ProtocolMetricEvidence protocolEvidence(String rawIdentifier,
                                                    String canonicalIdentifier,
                                                    String logicalChannelCode,
                                                    String parentDeviceCode,
                                                    String childDeviceCode,
                                                    String sampleValue) {
        ProtocolMetricEvidence evidence = new ProtocolMetricEvidence();
        evidence.setRawIdentifier(rawIdentifier);
        evidence.setCanonicalIdentifier(canonicalIdentifier);
        evidence.setLogicalChannelCode(logicalChannelCode);
        evidence.setParentDeviceCode(parentDeviceCode);
        evidence.setChildDeviceCode(childDeviceCode);
        evidence.setSampleValue(sampleValue);
        evidence.setValueType("double");
        evidence.setEvidenceOrigin("legacy_dp_child_template");
        return evidence;
    }
}
