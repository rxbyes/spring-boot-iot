package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
                governanceReceiptStore
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
    void applyGovernanceShouldReturnReleaseBatchIdAfterPublishingFormalFields() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "phase1-crack-product", "瑁傜紳鐩戞祴浜у搧"));
        when(productModelMapper.selectOne(any())).thenReturn(null);
        when(productContractReleaseService.createBatch(1001L, "phase1-crack", "manual_compare_apply", 1, 10001L))
                .thenReturn(12345L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "value", "crack-value")));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto, 10001L);

        assertNotNull(result.getReleaseBatchId());
        assertEquals(1, result.getCreatedCount());
    }

    @Test
    void applyGovernanceShouldReturnGnssReleaseBatchIdAfterPublishingFormalFields() {
        when(productMapper.selectById(3003L)).thenReturn(product(3003L, "gnss-monitor-v1", "gnss-monitor"));
        when(productModelMapper.selectOne(any())).thenReturn(null);
        when(productContractReleaseService.createBatch(3003L, "phase2-gnss", "manual_compare_apply", 1, 10001L))
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
        ProductModelGovernanceCompareDTO.RelationMappingInput item =
                new ProductModelGovernanceCompareDTO.RelationMappingInput();
        item.setLogicalChannelCode(logicalChannelCode);
        item.setChildDeviceCode(childDeviceCode);
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

