package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareRowVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductModelServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductModelMapper productModelMapper;
    @Mock
    private NormativeMetricDefinitionService normativeMetricDefinitionService;
    @Mock
    private ProductMetricEvidenceService productMetricEvidenceService;
    @Mock
    private ProductContractReleaseService productContractReleaseService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private VendorMetricMappingRuntimeService vendorMetricMappingRuntimeService;

    private InMemoryProductModelGovernanceReceiptStore governanceReceiptStore;
    private ProductModelServiceImpl productModelService;

    @BeforeEach
    void setUp() {
        governanceReceiptStore = new InMemoryProductModelGovernanceReceiptStore();
        productModelService = new ProductModelServiceImpl(
                productMapper,
                productModelMapper,
                normativeMetricDefinitionService,
                productMetricEvidenceService,
                productContractReleaseService,
                governanceReceiptStore,
                applicationEventPublisher,
                vendorMetricMappingRuntimeService
        );
    }

    @Test
    void createModelShouldRejectDuplicateIdentifierWithinSameProduct() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectOne(any())).thenReturn(existingModel(2001L, "temperature", 1));

        BizException ex = assertThrows(
                BizException.class,
                () -> productModelService.createModel(1001L, propertyDto("temperature", "double"))
        );

        assertTrue(ex.getMessage().contains("物模型标识已存在"));
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createModelShouldRejectUnsupportedModelType() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("command");
        dto.setIdentifier("set-temp");
        dto.setModelName("璁剧疆娓╁害");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("物模型类型不支持: command", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createPropertyModelShouldRequireDataType() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = propertyDto("temperature", null);

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("属性物模型必须填写 dataType", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createPropertyModelShouldRejectInvalidSpecsJson() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = propertyDto("temperature", "double");
        dto.setSpecsJson("{invalid");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("specsJson 必须是合法 JSON", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createEventModelShouldOnlyAllowEventTypeField() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("event");
        dto.setIdentifier("alarmRaised");
        dto.setModelName("鍛婅瑙﹀彂");
        dto.setEventType("warning");
        dto.setServiceInputJson("{\"unexpected\":true}");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("事件物模型只允许填写 eventType", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createServiceModelShouldOnlyAllowServiceInputAndOutputFields() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("service");
        dto.setIdentifier("setThreshold");
        dto.setModelName("set-threshold");
        dto.setServiceInputJson("{\"threshold\":10}");
        dto.setServiceOutputJson("{\"accepted\":true}");
        dto.setEventType("warning");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("服务物模型只允许填写 serviceInputJson 和 serviceOutputJson", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createServiceModelShouldRejectInvalidServiceInputJson() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("service");
        dto.setIdentifier("setThreshold");
        dto.setModelName("set-threshold");
        dto.setServiceInputJson("{invalid");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("serviceInputJson 必须是合法 JSON", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createEventModelShouldPersistCompatibilityDataTypeForExistingSchema() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectOne(any())).thenReturn(null);

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("event");
        dto.setIdentifier("alarmRaised");
        dto.setModelName("鍛婅瑙﹀彂");
        dto.setEventType("warning");
        dto.setSortNo(20);

        ProductModelVO result = productModelService.createModel(1001L, dto);

        ArgumentCaptor<ProductModel> captor = ArgumentCaptor.forClass(ProductModel.class);
        verify(productModelMapper).insert(captor.capture());
        assertEquals("json", captor.getValue().getDataType());
        assertNull(result.getDataType());
    }

    @Test
    void createServiceModelShouldPersistCompatibilityDataTypeForExistingSchema() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectOne(any())).thenReturn(null);

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("service");
        dto.setIdentifier("setThreshold");
        dto.setModelName("set-threshold");
        dto.setServiceInputJson("{\"threshold\":10}");
        dto.setServiceOutputJson("{\"accepted\":true}");
        dto.setSortNo(30);

        ProductModelVO result = productModelService.createModel(1001L, dto);

        ArgumentCaptor<ProductModel> captor = ArgumentCaptor.forClass(ProductModel.class);
        verify(productModelMapper).insert(captor.capture());
        assertEquals("json", captor.getValue().getDataType());
        assertNull(result.getDataType());
    }

    @Test
    void listModelsShouldSortBySortNoThenIdentifier() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                existingModel(2001L, "zeta", 20),
                existingModel(2002L, "alpha", 10),
                existingModel(2003L, "beta", 10)
        ));

        List<ProductModelVO> result = productModelService.listModels(1001L);

        assertEquals(3, result.size());
        assertIterableEquals(List.of("alpha", "beta", "zeta"), result.stream().map(ProductModelVO::getIdentifier).toList());
        assertIterableEquals(List.of(10, 10, 20), result.stream().map(ProductModelVO::getSortNo).toList());
        verify(productModelMapper).selectList(any());
    }

    @Test
    void compareGovernanceShouldExtractSingleDeviceBusinessSampleFromManualPayload() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

        assertEquals(
                List.of("temperature"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
    }

    @Test
    void compareGovernanceShouldCanonicalizeCompositeBusinessSampleIntoChildValueField() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "crack-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307986");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018143")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T20:34:06.000Z":10.86}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(2002L, dto);

        assertEquals(
                List.of("value"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
    }

    @Test
    void compareGovernanceShouldPreserveDeepDisplacementLegacyChildFieldsForCompositeBusinessSample() {
        when(productMapper.selectById(4004L)).thenReturn(product(4004L, "nf-monitor-deep-displacement-v1", "南方测绘 深部位移"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00FB0D1310195");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_SW_1", "84330701", "LEGACY", "NONE")));
        manualExtract.setSamplePayload("""
                {"SK00FB0D1310195":{"L1_SW_1":{"2026-04-09T13:53:10.000Z":{"dispsX":-0.0166,"dispsY":-0.0368}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(4004L, dto);

        assertEquals(
                List.of("dispsX", "dispsY"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
    }

    @Test
    void compareGovernanceShouldInferDeepDisplacementLegacyStrategyFromLogicalChannelCode() {
        when(productMapper.selectById(4004L)).thenReturn(product(4004L, "nf-monitor-deep-displacement-v1", "南方测绘 深部位移"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00FB0D1310195");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_SW_1", "84330701")));
        manualExtract.setSamplePayload("""
                {"SK00FB0D1310195":{"L1_SW_1":{"2026-04-09T13:53:10.000Z":{"dispsX":-0.0166,"dispsY":-0.0368}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(4004L, dto);

        assertEquals(
                List.of("dispsX", "dispsY"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
    }

    @Test
    void compareGovernanceShouldDecorateCrackRowsWithNormativeAndRiskMetadata() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "crack-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
                normativeDefinition("phase1-crack", "value", "crack-value", 1),
                normativeDefinition("phase1-crack", "sensor_state", "sensor-state", 0)
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307986");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018143")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T20:34:06.000Z":10.86}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(2002L, dto);

        ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
        assertEquals("value", row.getIdentifier());
        assertEquals("value", row.getNormativeIdentifier());
        assertEquals("crack-value", row.getNormativeName());
        assertTrue(row.getRiskReady());
        assertEquals(List.of("L1_LF_1"), row.getRawIdentifiers());
    }

    @Test
    void compareGovernanceShouldOnlyMirrorCompositeSensorStateWithoutLeakingParentTerminalStatus() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "crack-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307986");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018143")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307986":{"S1_ZT_1":{"2026-04-05T20:14:06.000Z":{"temp":20.31,"humidity":89.04,"sensor_state":{"L1_LF_1":0}}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(2002L, dto);

        assertEquals(
                List.of("sensor_state"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
        assertTrue(result.getCompareRows().stream().noneMatch(row -> "temp".equals(row.getIdentifier())));
    }

    @Test
    void compareGovernanceShouldNotMirrorCompositeSensorStateWhenStatusStrategyIsNone() {
        when(productMapper.selectById(4004L)).thenReturn(product(4004L, "nf-monitor-deep-displacement-v1", "南方测绘 深部位移"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00FB0D1310195");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_SW_1", "84330701", "LEGACY", "NONE")));
        manualExtract.setSamplePayload("""
                {"SK00FB0D1310195":{"S1_ZT_1":{"2026-04-09T13:53:10.000Z":{"temp":19.0,"sensor_state":{"L1_SW_1":0}}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(4004L, dto);

        assertTrue(result.getCompareRows().isEmpty());
    }

    @Test
    void compareGovernanceShouldDecorateGnssRowsWithNormativeMetadata() {
        when(productMapper.selectById(3003L)).thenReturn(product(3003L, "gnss-monitor-v1", "gnss-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase2-gnss")).thenReturn(List.of(
                normativeDefinition("phase2-gnss", "gpsInitial", "GNSS 鍘熷瑙傛祴鍩虹鏁版嵁", 0),
                normativeDefinition("phase2-gnss", "gpsTotalX", "GNSS 绱浣嶇Щ X", 1),
                normativeDefinition("phase2-gnss", "gpsTotalY", "GNSS 绱浣嶇Щ Y", 1),
                normativeDefinition("phase2-gnss", "gpsTotalZ", "GNSS 绱浣嶇Щ Z", 1)
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"device-gnss-01":{"gpsTotalX":{"2026-04-06T08:00:00.000Z":12.6},"gpsTotalY":{"2026-04-06T08:00:00.000Z":3.2}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(3003L, dto);

        assertEquals(
                List.of("gpsTotalX", "gpsTotalY"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
        ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
        assertEquals("gpsTotalX", row.getNormativeIdentifier());
        assertEquals("GNSS 绱浣嶇Щ X", row.getNormativeName());
        assertTrue(row.getRiskReady());
        assertEquals(List.of("gpsTotalX"), row.getRawIdentifiers());
    }

    @Test
    void compareGovernanceShouldRequireRelationMappingsForCompositeSamples() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "crack-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307986");
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T20:34:06.000Z":10.86}}}
                """);
        dto.setManualExtract(manualExtract);

        BizException ex = assertThrows(BizException.class, () -> productModelService.compareGovernance(2002L, dto));

        assertTrue(ex.getMessage().contains("映射关系"));
    }

    @Test
    void compareGovernanceShouldNormalizeRawFieldByVendorMappingRule() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "phase1-crack-product", "crack-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
                normativeDefinition("phase1-crack", "value", "crack-value", 1)
        ));
        when(vendorMetricMappingRuntimeService.resolveForGovernance(any(Product.class), eq("disp"), eq(null)))
                .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(8801L, "value", "disp", null));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"device-001":{"disp":{"2026-04-05T20:14:06.000Z":26.5}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

        ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
        assertEquals("value", row.getIdentifier());
        assertEquals("value", row.getNormativeIdentifier());
        assertEquals(List.of("disp"), row.getRawIdentifiers());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VendorMetricEvidence>> evidenceCaptor = ArgumentCaptor.forClass(List.class);
        verify(productMetricEvidenceService).replaceManualEvidence(eq(1001L), eq("phase1-crack"), evidenceCaptor.capture());
        assertEquals("value", evidenceCaptor.getValue().get(0).getCanonicalIdentifier());
        assertEquals("disp", evidenceCaptor.getValue().get(0).getRawIdentifier());
    }

    @Test
    void applyGovernanceShouldCreateUpdateAndSkipExplicitDecisions() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectById(2001L)).thenReturn(existingEventModel(2001L, "alarmRaised", 10, "info"));

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(
                applyItem("create", null, "property", "S1_ZT_1.signal_4g", "4G 淇″彿寮哄害"),
                applyItem("update", 2001L, "event", "alarmRaised", "鍛婅瑙﹀彂"),
                applyItem("skip", null, "service", "reboot", "閲嶅惎璁惧")
        ));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto, 10001L);

        verify(productModelMapper).insert(any(ProductModel.class));
        verify(productModelMapper).updateById(any(ProductModel.class));
        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
    }

    @Test
    void applyGovernanceShouldNormalizeRawIdentifierByVendorMappingRuleBeforePersisting() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "phase1-crack-product", "crack-monitor"));
        when(productModelMapper.selectOne(any())).thenReturn(null);
        when(vendorMetricMappingRuntimeService.normalizeApplyIdentifier(any(Product.class), eq("disp")))
                .thenReturn("value");

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "disp", "monitor-value")));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto, 10001L);

        ArgumentCaptor<ProductModel> captor = ArgumentCaptor.forClass(ProductModel.class);
        verify(productModelMapper).insert(captor.capture());
        assertEquals("value", captor.getValue().getIdentifier());
        assertEquals("value", result.getAppliedItems().get(0).getIdentifier());
    }

    @Test
    void applyGovernanceShouldReturnReleaseBatchIdAfterPublishingFormalFields() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "phase1-crack-product", "瑁傜紳鐩戞祴浜у搧"));
        when(productModelMapper.selectOne(any())).thenReturn(null);
        when(productContractReleaseService.createBatch(
                eq(1001L),
                eq("phase1-crack"),
                eq("manual_compare_apply"),
                eq(1),
                eq(10001L),
                eq(null),
                eq("manual_compare_apply")
        ))
                .thenReturn(12345L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "value", "crack-value")));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto, 10001L);

        assertNotNull(result.getReleaseBatchId());
        assertEquals(1, result.getCreatedCount());
    }

    @Test
    void applyGovernanceShouldPublishContractReleasedEventAfterReleaseBatchCreated() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "phase1-crack-product", "crack-monitor"));
        when(productModelMapper.selectOne(any())).thenReturn(null);
        ProductModel releasedValue = new ProductModel();
        releasedValue.setId(3101L);
        releasedValue.setProductId(1001L);
        releasedValue.setModelType("property");
        releasedValue.setIdentifier("value");
        releasedValue.setModelName("crack-value");
        releasedValue.setDataType("integer");
        releasedValue.setDeleted(0);
        when(productModelMapper.selectList(any())).thenReturn(List.of(), List.of(releasedValue));
        when(productContractReleaseService.createBatch(
                eq(1001L),
                eq("phase1-crack"),
                eq("manual_compare_apply"),
                eq(1),
                eq(10001L),
                eq(99001L),
                eq("manual_compare_apply")
        )).thenReturn(12345L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "value", "crack-value")));

        productModelService.applyGovernance(1001L, dto, 10001L, 99001L);

        verify(applicationEventPublisher).publishEvent(argThat((ProductContractReleasedEvent event) ->
                Long.valueOf(1L).equals(event.tenantId())
                        && Long.valueOf(1001L).equals(event.productId())
                        && Long.valueOf(12345L).equals(event.releaseBatchId())
                        && "phase1-crack".equals(event.scenarioCode())
                        && List.of("value").equals(event.releasedIdentifiers())
                        && Long.valueOf(10001L).equals(event.operatorUserId())
                        && Long.valueOf(99001L).equals(event.approvalOrderId())
        ));
    }

    @Test
    void applyGovernanceShouldReturnGnssReleaseBatchIdAfterPublishingFormalFields() {
        when(productMapper.selectById(3003L)).thenReturn(product(3003L, "gnss-monitor-v1", "gnss-monitor"));
        when(productModelMapper.selectOne(any())).thenReturn(null);
        when(productContractReleaseService.createBatch(
                eq(3003L),
                eq("phase2-gnss"),
                eq("manual_compare_apply"),
                eq(1),
                eq(10001L),
                eq(null),
                eq("manual_compare_apply")
        ))
                .thenReturn(22345L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "gpsTotalX", "GNSS 绱浣嶇Щ X")));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(3003L, dto, 10001L);

        assertEquals(22345L, result.getReleaseBatchId());
        assertEquals(1, result.getCreatedCount());
    }

    @Test
    void applyGovernanceShouldRejectUpdateWithoutTargetModelId() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("update", null, "event", "alarmRaised", "鍛婅瑙﹀彂")));

        BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(1001L, dto, 10001L));

        assertEquals("治理修订必须指定 targetModelId", ex.getMessage());
    }

    private Product product(Long id) {
        return product(id, "accept-product", "楠屾敹浜у搧");
    }

    private Product product(Long id, String productKey, String productName) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(1L);
        product.setProductKey(productKey);
        product.setProductName(productName);
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        return product;
    }

    private ProductModel existingModel(Long id, String identifier, Integer sortNo) {
        ProductModel model = new ProductModel();
        model.setId(id);
        model.setProductId(1001L);
        model.setModelType("property");
        model.setIdentifier(identifier);
        model.setModelName(identifier);
        model.setDataType("double");
        model.setSortNo(sortNo);
        model.setDeleted(0);
        return model;
    }

    private ProductModel existingEventModel(Long id, String identifier, Integer sortNo, String eventType) {
        ProductModel model = new ProductModel();
        model.setId(id);
        model.setProductId(1001L);
        model.setModelType("event");
        model.setIdentifier(identifier);
        model.setModelName(identifier);
        model.setDataType("json");
        model.setEventType(eventType);
        model.setSortNo(sortNo);
        model.setDeleted(0);
        return model;
    }

    private ProductModelUpsertDTO propertyDto(String identifier, String dataType) {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("property");
        dto.setIdentifier(identifier);
        dto.setModelName("娓╁害");
        dto.setDataType(dataType);
        dto.setSpecsJson("{\"unit\":\"celsius\"}");
        dto.setSortNo(10);
        dto.setRequiredFlag(1);
        dto.setDescription("temperature");
        return dto;
    }

    private ProductModelGovernanceCompareDTO.RelationMappingInput relationMapping(String logicalChannelCode,
                                                                                  String childDeviceCode) {
        return relationMapping(logicalChannelCode, childDeviceCode, null, null);
    }

    private ProductModelGovernanceCompareDTO.RelationMappingInput relationMapping(String logicalChannelCode,
                                                                                  String childDeviceCode,
                                                                                  String canonicalizationStrategy,
                                                                                  String statusMirrorStrategy) {
        ProductModelGovernanceCompareDTO.RelationMappingInput item =
                new ProductModelGovernanceCompareDTO.RelationMappingInput();
        item.setLogicalChannelCode(logicalChannelCode);
        item.setChildDeviceCode(childDeviceCode);
        item.setCanonicalizationStrategy(canonicalizationStrategy);
        item.setStatusMirrorStrategy(statusMirrorStrategy);
        return item;
    }

    private ProductModelGovernanceApplyDTO.ApplyItem applyItem(String decision,
                                                               Long targetModelId,
                                                               String modelType,
                                                               String identifier,
                                                               String modelName) {
        ProductModelGovernanceApplyDTO.ApplyItem item = new ProductModelGovernanceApplyDTO.ApplyItem();
        item.setDecision(decision);
        item.setTargetModelId(targetModelId);
        item.setModelType(modelType);
        item.setIdentifier(identifier);
        item.setModelName(modelName);
        item.setSortNo(10);
        item.setRequiredFlag(0);
        item.setDescription("governance-test-item");
        if ("property".equals(modelType)) {
            item.setDataType("integer");
            item.setSpecsJson("{\"unit\":\"dBm\"}");
        } else if ("event".equals(modelType)) {
            item.setEventType("warning");
        } else if ("service".equals(modelType)) {
            item.setServiceInputJson("[]");
            item.setServiceOutputJson("[]");
        }
        return item;
    }

    private ProductModelGovernanceCompareRowVO compareRow(ProductModelGovernanceCompareVO result,
                                                          String modelType,
                                                          String identifier) {
        return result.getCompareRows().stream()
                .filter(row -> modelType.equals(row.getModelType()) && identifier.equals(row.getIdentifier()))
                .findFirst()
                .orElseThrow();
    }

    private com.ghlzm.iot.device.entity.NormativeMetricDefinition normativeDefinition(String scenarioCode,
                                                                                      String identifier,
                                                                                      String displayName,
                                                                                      int riskEnabled) {
        com.ghlzm.iot.device.entity.NormativeMetricDefinition definition =
                new com.ghlzm.iot.device.entity.NormativeMetricDefinition();
        definition.setScenarioCode(scenarioCode);
        definition.setIdentifier(identifier);
        definition.setDisplayName(displayName);
        definition.setRiskEnabled(riskEnabled);
        return definition;
    }
}
