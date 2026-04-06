package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
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

    private InMemoryProductModelGovernanceReceiptStore governanceReceiptStore;
    private ProductModelServiceImpl productModelService;

    @BeforeEach
    void setUp() {
        governanceReceiptStore = new InMemoryProductModelGovernanceReceiptStore();
        productModelService = new ProductModelServiceImpl(
                productMapper,
                productModelMapper,
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
        dto.setModelName("设置温度");

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
        dto.setModelName("告警触发");
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
        dto.setModelName("设置阈值");
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
        dto.setModelName("设置阈值");
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
        dto.setModelName("告警触发");
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
        dto.setModelName("设置阈值");
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
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "裂缝监测仪"));
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
    void compareGovernanceShouldOnlyMirrorCompositeSensorStateWithoutLeakingParentTerminalStatus() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "裂缝监测仪"));
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
    void compareGovernanceShouldRequireRelationMappingsForCompositeSamples() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "裂缝监测仪"));
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
                applyItem("create", null, "property", "S1_ZT_1.signal_4g", "4G 信号强度"),
                applyItem("update", 2001L, "event", "alarmRaised", "告警触发"),
                applyItem("skip", null, "service", "reboot", "重启设备")
        ));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto);

        verify(productModelMapper).insert(any(ProductModel.class));
        verify(productModelMapper).updateById(any(ProductModel.class));
        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
    }

    @Test
    void applyGovernanceShouldRejectUpdateWithoutTargetModelId() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("update", null, "event", "alarmRaised", "告警触发")));

        BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(1001L, dto));

        assertEquals("治理修订必须指定 targetModelId", ex.getMessage());
    }

    private Product product(Long id) {
        return product(id, "accept-product", "验收产品");
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
        dto.setModelName("温度");
        dto.setDataType(dataType);
        dto.setSpecsJson("{\"unit\":\"℃\"}");
        dto.setSortNo(10);
        dto.setRequiredFlag(1);
        dto.setDescription("温度属性");
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
        item.setDescription("治理测试项");
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
}
