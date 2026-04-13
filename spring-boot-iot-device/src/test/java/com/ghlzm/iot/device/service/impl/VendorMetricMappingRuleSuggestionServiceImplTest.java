package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleSuggestionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleSuggestionServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private VendorMetricEvidenceMapper evidenceMapper;
    @Mock
    private PublishedProductContractSnapshotService snapshotService;
    @Mock
    private NormativeMetricDefinitionService normativeMetricDefinitionService;
    @Mock
    private VendorMetricMappingRuntimeService runtimeService;

    private VendorMetricMappingRuleSuggestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VendorMetricMappingRuleSuggestionServiceImpl(
                productMapper,
                evidenceMapper,
                snapshotService,
                normativeMetricDefinitionService,
                runtimeService
        );
    }

    @Test
    void listSuggestionsShouldReturnReadyToCreateForPublishedCanonicalEvidence() {
        when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
        when(evidenceMapper.selectList(any())).thenReturn(List.of(
                evidence("disp", "value", "L1_LF_1", 4, "0.2136")
        ));
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .canonicalAlias("disp", "value")
                .build());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack"))
                .thenReturn(List.of(normativeDefinition("phase1-crack", "value")));
        when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1"))).thenReturn(null);

        List<VendorMetricMappingRuleSuggestionVO> result =
                service.listSuggestions(1001L, false, false, 1);

        assertEquals(1, result.size());
        assertEquals("READY_TO_CREATE", result.get(0).getStatus());
        assertEquals("PRODUCT", result.get(0).getRecommendedScopeType());
        assertEquals("high", result.get(0).getConfidence());
    }

    @Test
    void listSuggestionsShouldReturnAlreadyCoveredWhenGovernanceResolverMatchesExistingRule() {
        when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
        when(evidenceMapper.selectList(any())).thenReturn(List.of(
                evidence("disp", "value", "L1_LF_1", 5, "0.2136")
        ));
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .build());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack"))
                .thenReturn(List.of(normativeDefinition("phase1-crack", "value")));
        when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1")))
                .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(7001L, "value", "disp", "L1_LF_1"));

        List<VendorMetricMappingRuleSuggestionVO> result =
                service.listSuggestions(1001L, true, false, 1);

        assertEquals(1, result.size());
        assertEquals("ALREADY_COVERED", result.get(0).getStatus());
        assertEquals(7001L, result.get(0).getExistingRuleId());
    }

    @Test
    void listSuggestionsShouldReturnConflictsWithExistingWhenResolverPointsToDifferentCanonical() {
        when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
        when(evidenceMapper.selectList(any())).thenReturn(List.of(
                evidence("disp", "value", "L1_LF_1", 3, "0.2136")
        ));
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .build());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack"))
                .thenReturn(List.of(normativeDefinition("phase1-crack", "value")));
        when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1")))
                .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(7002L, "sensor_state", "disp", "L1_LF_1"));

        List<VendorMetricMappingRuleSuggestionVO> result =
                service.listSuggestions(1001L, true, false, 1);

        assertEquals(1, result.size());
        assertEquals("CONFLICTS_WITH_EXISTING", result.get(0).getStatus());
        assertEquals("sensor_state", result.get(0).getExistingTargetNormativeIdentifier());
    }

    @Test
    void listSuggestionsShouldFilterIgnoredRowsByDefault() {
        when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
        when(evidenceMapper.selectList(any())).thenReturn(List.of(
                evidence("value", "value", "L1_LF_1", 3, "0.2136")
        ));
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.empty(1001L));
        when(normativeMetricDefinitionService.listByScenario("phase1-crack"))
                .thenReturn(List.of(normativeDefinition("phase1-crack", "value")));

        List<VendorMetricMappingRuleSuggestionVO> result =
                service.listSuggestions(1001L, false, false, 1);

        assertTrue(result.isEmpty());
    }

    @Test
    void listSuggestionsShouldReturnLowConfidenceWhenEvidenceCountIsOne() {
        when(productMapper.selectById(1001L)).thenReturn(crackProduct(1001L));
        when(evidenceMapper.selectList(any())).thenReturn(List.of(
                evidence("disp", "value", "L1_LF_1", 1, "0.2136")
        ));
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .build());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack"))
                .thenReturn(List.of(normativeDefinition("phase1-crack", "value")));
        when(runtimeService.resolveForGovernance(any(), eq("disp"), eq("L1_LF_1"))).thenReturn(null);

        List<VendorMetricMappingRuleSuggestionVO> result =
                service.listSuggestions(1001L, false, false, 1);

        assertEquals(1, result.size());
        assertEquals("LOW_CONFIDENCE", result.get(0).getStatus());
        assertEquals("low", result.get(0).getConfidence());
    }

    private Product crackProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("phase1-crack-product");
        product.setProductName("crack-monitor");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private VendorMetricEvidence evidence(String rawIdentifier,
                                          String canonicalIdentifier,
                                          String logicalChannelCode,
                                          int evidenceCount,
                                          String sampleValue) {
        VendorMetricEvidence evidence = new VendorMetricEvidence();
        evidence.setRawIdentifier(rawIdentifier);
        evidence.setCanonicalIdentifier(canonicalIdentifier);
        evidence.setLogicalChannelCode(logicalChannelCode);
        evidence.setEvidenceCount(evidenceCount);
        evidence.setSampleValue(sampleValue);
        evidence.setValueType("double");
        evidence.setEvidenceOrigin("mqtt-json");
        evidence.setLastSeenTime(LocalDateTime.of(2026, 4, 13, 16, 40, 0));
        return evidence;
    }

    private NormativeMetricDefinition normativeDefinition(String scenarioCode, String identifier) {
        NormativeMetricDefinition definition = new NormativeMetricDefinition();
        definition.setScenarioCode(scenarioCode);
        definition.setIdentifier(identifier);
        return definition;
    }
}
