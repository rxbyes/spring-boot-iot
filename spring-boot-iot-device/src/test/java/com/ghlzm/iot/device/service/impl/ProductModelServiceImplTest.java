package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceAppliedItemVO;
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
import static org.mockito.Mockito.lenient;
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
    private DeviceMapper deviceMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private VendorMetricEvidenceMapper vendorMetricEvidenceMapper;
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
                deviceMapper,
                devicePropertyMapper,
                vendorMetricEvidenceMapper,
                normativeMetricDefinitionService,
                productMetricEvidenceService,
                productContractReleaseService,
                governanceReceiptStore,
                applicationEventPublisher,
                vendorMetricMappingRuntimeService
        );
        lenient().when(productModelMapper.selectAnyByProductAndIdentifier(any(), any())).thenReturn(List.of());
    }

    @Test
    void createModelShouldRejectDuplicateIdentifierWithinSameProduct() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectAnyByProductAndIdentifier(1001L, "temperature"))
                .thenReturn(List.of(existingModel(2001L, "temperature", 1)));

        BizException ex = assertThrows(
                BizException.class,
                () -> productModelService.createModel(1001L, propertyDto("temperature", "double"))
        );

        assertTrue(ex.getMessage().contains("物模型标识已存在"));
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createModelShouldReviveSoftDeletedHistoricalIdentifierInsteadOfInsertingDuplicate() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        ProductModel deletedModel = existingModel(3001L, "temperature", 6);
        deletedModel.setDeleted(1);
        when(productModelMapper.selectAnyByProductAndIdentifier(1001L, "temperature"))
                .thenReturn(List.of(deletedModel));
        when(productModelMapper.reviveDeletedById(any(ProductModel.class))).thenReturn(1);

        ProductModelVO result = productModelService.createModel(1001L, propertyDto("temperature", "double"));

        assertEquals(3001L, result.getId());
        assertEquals("temperature", result.getIdentifier());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
        verify(productModelMapper).reviveDeletedById(org.mockito.ArgumentMatchers.<ProductModel>argThat(model ->
                Long.valueOf(3001L).equals(model.getId())
                        && Long.valueOf(1001L).equals(model.getProductId())
                        && Integer.valueOf(0).equals(model.getDeleted())
                        && "temperature".equals(model.getIdentifier())
                        && "double".equals(model.getDataType())
        ));
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
    void updateModelShouldRejectIdentifierReservedBySoftDeletedHistoricalRow() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        ProductModel current = existingModel(2001L, "humidity", 3);
        current.setProductId(1001L);
        when(productModelMapper.selectById(2001L)).thenReturn(current);
        ProductModel deletedModel = existingModel(3001L, "temperature", 6);
        deletedModel.setDeleted(1);
        when(productModelMapper.selectAnyByProductAndIdentifier(1001L, "temperature"))
                .thenReturn(List.of(deletedModel));

        BizException ex = assertThrows(
                BizException.class,
                () -> productModelService.updateModel(1001L, 2001L, propertyDto("temperature", "double"))
        );

        assertEquals("同一产品下物模型标识已存在历史记录，请先恢复或更换标识: temperature", ex.getMessage());
        verify(productModelMapper, never()).updateById(any(ProductModel.class));
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
    void productModelSpecsJsonShouldAllowNullUpdate() throws NoSuchFieldException {
        TableField tableField = ProductModel.class.getDeclaredField("specsJson").getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals(FieldStrategy.ALWAYS, tableField.updateStrategy());
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
        manualExtract.setRelationMappings(List.of(relationMapping("L1_SW_1", "84330701", "LEGACY", "SENSOR_STATE")));
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
    void compareGovernanceShouldReturnRuntimeOnlyRowFromDeviceProperties() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(5001L, 1001L, "device-001")));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                deviceProperty(5001L, "humidity", "设备湿度", "61.2", "double", LocalDateTime.of(2026, 4, 13, 12, 30, 0))
        ));

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
        ProductModelGovernanceCompareRowVO row = compareRow(result, "property", "humidity");

        assertEquals("runtime_only", row.getCompareStatus());
        assertNull(row.getManualCandidate());
        assertNotNull(row.getRuntimeCandidate());
        assertEquals("runtime", row.getRuntimeCandidate().getEvidenceOrigin());
        assertEquals(1, row.getRuntimeCandidate().getEvidenceCount());
        assertEquals(0, row.getRuntimeCandidate().getMessageEvidenceCount());
        assertEquals(List.of("iot_device_property"), row.getRuntimeCandidate().getSourceTables());
    }

    @Test
    void compareGovernanceShouldMergeRuntimeEvidenceAliasesIntoRuntimeCandidate() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "crack-monitor"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(5002L, 2002L, "device-002")));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                deviceProperty(5002L, "value", "裂缝值", "0.2136", "double", LocalDateTime.of(2026, 4, 12, 21, 30, 28))
        ));
        when(vendorMetricEvidenceMapper.selectList(any())).thenReturn(List.of(
                runtimeEvidence(
                        2002L,
                        "L1_LF_1.value",
                        "value",
                        6,
                        LocalDateTime.of(2026, 4, 12, 21, 31, 0),
                        "0.2136",
                        "double"
                )
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"device-002":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(2002L, dto);
        ProductModelGovernanceCompareRowVO row = compareRow(result, "property", "value");

        assertEquals("runtime_only", row.getCompareStatus());
        assertNotNull(row.getRuntimeCandidate());
        assertEquals(List.of("value", "L1_LF_1.value"), row.getRuntimeCandidate().getRawIdentifiers());
        assertEquals(6, row.getRuntimeCandidate().getEvidenceCount());
        assertEquals(6, row.getRuntimeCandidate().getMessageEvidenceCount());
        assertEquals(
                List.of("iot_device_property", "iot_vendor_metric_evidence"),
                row.getRuntimeCandidate().getSourceTables()
        );
        assertEquals(LocalDateTime.of(2026, 4, 12, 21, 31, 0), row.getRuntimeCandidate().getLastReportTime());
    }

    @Test
    void compareGovernanceShouldInferDeepDisplacementSensorStateMirrorFromLogicalChannelCode() {
        when(productMapper.selectById(4004L)).thenReturn(product(4004L, "nf-monitor-deep-displacement-v1", "南方测绘 深部位移"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00FB0D1310195");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_SW_1", "84330701")));
        manualExtract.setSamplePayload("""
                {"SK00FB0D1310195":{"S1_ZT_1":{"2026-04-09T13:53:10.000Z":{"temp":19.0,"sensor_state":{"L1_SW_1":0}}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(4004L, dto);

        assertEquals(
                List.of("sensor_state"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
        assertTrue(result.getCompareRows().stream().noneMatch(row -> "temp".equals(row.getIdentifier())));
    }

    @Test
    void compareGovernanceShouldIgnoreCompositeChildMetricsForCollectorProduct() {
        Product collector = product(6006L, "nf-monitor-collector-v1", "南方测绘 监测型 采集器");
        collector.setNodeType(2);
        when(productMapper.selectById(6006L)).thenReturn(collector);
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307988");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307988":{"L1_LF_1":{"2026-04-09T13:47:28.000Z":10.86}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6006L, dto);

        assertTrue(result.getCompareRows().isEmpty());
    }

    @Test
    void compareGovernanceShouldKeepCollectorRuntimeStatusButDropChildSensorState() {
        Product collector = product(6006L, "nf-monitor-collector-v1", "南方测绘 监测型 采集器");
        collector.setNodeType(2);
        when(productMapper.selectById(6006L)).thenReturn(collector);
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307988");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307988":{"S1_ZT_1":{"2026-04-09T13:47:28.000Z":{"temp":20.31,"humidity":89.04,"signal_4g":-71,"sensor_state":{"L1_LF_1":0}}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6006L, dto);

        assertEquals(
                List.of("humidity", "signal_4g", "temp"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .sorted()
                        .toList()
        );
        assertTrue(result.getCompareRows().stream().noneMatch(item -> "sensor_state".equals(item.getIdentifier())));
    }

    @Test
    void compareGovernanceShouldKeepAllParentStatusFieldsForDirectCollectorRtuProduct() {
        Product collector = directCollectorRtuProduct(6007L);
        when(productMapper.selectById(6007L)).thenReturn(collector);
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307986");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018143")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307986":{"S1_ZT_1":{"2026-04-13T17:34:04.000Z":{"ext_power_volt":12.12,"solar_volt":0,"battery_dump_energy":0,"battery_volt":0,"supply_power":0,"consume_power":0,"temp":20.44,"humidity":89.04,"temp_out":20.44,"humidity_out":89.04,"lon":"0.000000","lat":"0.000000","signal_4g":22,"singal_NB":0,"singal_db":0,"sw_version":"1.09.250808.RK00PX.BETA","sensor_state":{"L1_LF_1":0}}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6007L, dto);

        assertEquals(
                List.of(
                        "battery_dump_energy",
                        "battery_volt",
                        "consume_power",
                        "ext_power_volt",
                        "humidity",
                        "humidity_out",
                        "lat",
                        "lon",
                        "signal_4g",
                        "signal_NB",
                        "signal_db",
                        "solar_volt",
                        "supply_power",
                        "sw_version",
                        "temp",
                        "temp_out"
                ),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .sorted()
                        .toList()
        );
        assertTrue(result.getCompareRows().stream().noneMatch(item -> "sensor_state".equals(item.getIdentifier())));
    }

    @Test
    void compareGovernanceShouldKeepSingleDeviceStatusPathsAndFriendlyNamesForDirectCollectorRtuProduct() {
        Product collector = directCollectorRtuProduct(6007L);
        when(productMapper.selectById(6007L)).thenReturn(collector);
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"SK00D50D1305080":{"S1_ZT_1":{"2026-04-20T04:33:10.000Z":{"ext_power_volt":14.04,"battery_dump_energy":0}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6007L, dto);

        assertEquals(
                List.of("S1_ZT_1.battery_dump_energy", "S1_ZT_1.ext_power_volt"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .sorted()
                        .toList()
        );
        assertEquals(
                "外接电源电压",
                compareRow(result, "property", "S1_ZT_1.ext_power_volt").getManualCandidate().getModelName()
        );
        assertEquals(
                "电池剩余电量",
                compareRow(result, "property", "S1_ZT_1.battery_dump_energy").getManualCandidate().getModelName()
        );
    }

    @Test
    void compareGovernanceShouldKeepSingleDeviceStatusFullPathsForWarningSoundLightAlarmProduct() {
        Product product = product(8008L, "zhd-warning-sound-light-alarm-v1", "中海达 预警型 声光报警器");
        when(productMapper.selectById(8008L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"6260370286":{"S1_ZT_1":{"2026-04-20T09:05:00.000Z":{"sound_state":1,"battery_dump_energy":86,"pa_state":0}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(8008L, dto);

        assertEquals("FULL_PATH", result.getManualSummary().getResolvedContractIdentifierMode());
        assertEquals(
                List.of("S1_ZT_1.battery_dump_energy", "S1_ZT_1.pa_state", "S1_ZT_1.sound_state"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .sorted()
                        .toList()
        );
        assertEquals(
                "电池剩余电量",
                compareRow(result, "property", "S1_ZT_1.battery_dump_energy").getManualCandidate().getModelName()
        );
        assertNull(compareRow(result, "property", "S1_ZT_1.sound_state").getNormativeIdentifier());
        assertNull(compareRow(result, "property", "S1_ZT_1.pa_state").getNormativeIdentifier());
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
    void compareGovernanceShouldDecorateLaserRowsWithLaserFacingNormativeMetadata() {
        when(productMapper.selectById(5005L)).thenReturn(product(
                5005L,
                "nf-monitor-laser-rangefinder-v1",
                "南方测绘 监测型 激光测距仪"
        ));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
                normativeDefinition("phase1-crack", "value", "裂缝监测值", 1),
                normativeDefinition("phase1-crack", "sensor_state", "传感器状态", 0)
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307988");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307988":{"L1_LF_1":{"2026-04-09T13:47:28.000Z":10.86}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(5005L, dto);

        ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
        assertEquals("value", row.getIdentifier());
        assertEquals("激光测距值", row.getManualCandidate().getModelName());
        assertEquals("value", row.getNormativeIdentifier());
        assertEquals("激光测距值", row.getNormativeName());
        assertTrue(row.getRiskReady());
        assertEquals(List.of("L1_LF_1"), row.getRawIdentifiers());
        verify(productMetricEvidenceService).replaceManualEvidence(eq(5005L), eq("phase1-crack"), any());
    }

    @Test
    void compareGovernanceShouldDecorateLaserSensorStateRowsWithoutLeakingParentCollectorStatus() {
        when(productMapper.selectById(5005L)).thenReturn(product(
                5005L,
                "nf-monitor-laser-rangefinder-v1",
                "南方测绘 监测型 激光测距仪"
        ));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
                normativeDefinition("phase1-crack", "value", "裂缝监测值", 1),
                normativeDefinition("phase1-crack", "sensor_state", "传感器状态", 0)
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00EA0D1307988");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
        manualExtract.setSamplePayload("""
                {"SK00EA0D1307988":{"S1_ZT_1":{"2026-04-09T13:47:28.000Z":{"temp":20.31,"humidity":89.04,"sensor_state":{"L1_LF_1":0}}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(5005L, dto);

        ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
        assertEquals("sensor_state", row.getIdentifier());
        assertEquals("sensor_state", row.getNormativeIdentifier());
        assertEquals("传感器状态", row.getNormativeName());
        assertTrue(result.getCompareRows().stream().noneMatch(item -> "temp".equals(item.getIdentifier())));
    }

    @Test
    void compareGovernanceShouldDecorateDeepDisplacementRowsWithNormativeMetadata() {
        when(productMapper.selectById(4004L)).thenReturn(product(
                4004L,
                "nf-monitor-deep-displacement-v1",
                "南方测绘 监测型 深部位移监测仪"
        ));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase3-deep-displacement")).thenReturn(List.of(
                normativeDefinition("phase3-deep-displacement", "dispsX", "顺滑动方向累计变形量", 1),
                normativeDefinition("phase3-deep-displacement", "dispsY", "垂直坡面方向累计变形量", 1),
                normativeDefinition("phase3-deep-displacement", "sensor_state", "传感器状态", 0)
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("composite");
        manualExtract.setParentDeviceCode("SK00FB0D1310195");
        manualExtract.setRelationMappings(List.of(relationMapping("L1_SW_1", "84330701", "LEGACY", "SENSOR_STATE")));
        manualExtract.setSamplePayload("""
                {"SK00FB0D1310195":{"L1_SW_1":{"2026-04-09T13:53:10.000Z":{"dispsX":-0.0166,"dispsY":-0.0368}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(4004L, dto);

        ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
        assertEquals("dispsX", row.getIdentifier());
        assertEquals("dispsX", row.getNormativeIdentifier());
        assertEquals("顺滑动方向累计变形量", row.getNormativeName());
        assertTrue(row.getRiskReady());
        assertEquals(List.of("L1_SW_1.dispsX"), row.getRawIdentifiers());
        verify(productMetricEvidenceService).replaceManualEvidence(eq(4004L), eq("phase3-deep-displacement"), any());
    }

    @Test
    void compareGovernanceShouldNormalizeRainGaugeRowsIntoNormativeIdentifiers() {
        when(productMapper.selectById(7007L)).thenReturn(product(
                7007L,
                "nf-monitor-tipping-bucket-rain-gauge-v1",
                "南方测绘 监测型 翻斗式雨量计"
        ));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listByScenario("phase4-rain-gauge")).thenReturn(List.of(
                normativeDefinition("phase4-rain-gauge", "value", "当前雨量", 1),
                normativeDefinition("phase4-rain-gauge", "totalValue", "累计雨量", 0)
        ));
        when(vendorMetricMappingRuntimeService.resolveForGovernance(any(Product.class), eq("L3_YL_1.value"), eq("L3_YL_1")))
                .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(9907001L, "value", "L3_YL_1.value", "L3_YL_1"));
        when(vendorMetricMappingRuntimeService.resolveForGovernance(any(Product.class), eq("L3_YL_1.totalValue"), eq("L3_YL_1")))
                .thenReturn(new VendorMetricMappingRuntimeService.MappingResolution(9907002L, "totalValue", "L3_YL_1.totalValue", "L3_YL_1"));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("business");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"SK00D50D1305080":{"L3_YL_1":{"2026-04-10T11:48:55.000Z":{"value":0,"totalValue":0}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(7007L, dto);

        assertEquals(
                List.of("value", "totalValue"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .toList()
        );
        ProductModelGovernanceCompareRowVO valueRow = compareRow(result, "property", "value");
        ProductModelGovernanceCompareRowVO totalValueRow = compareRow(result, "property", "totalValue");
        assertEquals("当前雨量", valueRow.getManualCandidate().getModelName());
        assertEquals("value", valueRow.getNormativeIdentifier());
        assertEquals("当前雨量", valueRow.getNormativeName());
        assertTrue(valueRow.getRiskReady());
        assertEquals(List.of("L3_YL_1.value"), valueRow.getRawIdentifiers());
        assertEquals("累计雨量", totalValueRow.getManualCandidate().getModelName());
        assertEquals("totalValue", totalValueRow.getNormativeIdentifier());
        assertEquals("累计雨量", totalValueRow.getNormativeName());
        assertEquals(Boolean.FALSE, totalValueRow.getRiskReady());
        assertEquals(List.of("L3_YL_1.totalValue"), totalValueRow.getRawIdentifiers());
        verify(productMetricEvidenceService).replaceManualEvidence(eq(7007L), eq("phase4-rain-gauge"), any());
    }

    @Test
    void compareGovernanceShouldKeepRainGaugeRuntimeOnlyStatusRowsOnFullPathAfterSingleDeviceModeResolved() {
        when(productMapper.selectById(7007L)).thenReturn(product(
                7007L,
                "nf-monitor-tipping-bucket-rain-gauge-v1",
                "南方测绘 监测型 翻斗式雨量计"
        ));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(5007L, 7007L, "SK00D50D1305080")));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                deviceProperty(5007L, "humidity", "设备湿度", "89.04", "double", LocalDateTime.of(2026, 4, 20, 4, 35, 0))
        ));
        when(vendorMetricEvidenceMapper.selectList(any())).thenReturn(List.of(
                runtimeEvidence(
                        7007L,
                        "S1_ZT_1.humidity",
                        "humidity",
                        4,
                        LocalDateTime.of(2026, 4, 20, 4, 35, 0),
                        "89.04",
                        "double"
                )
        ));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
                new ProductModelGovernanceCompareDTO.ManualExtractInput();
        manualExtract.setSampleType("status");
        manualExtract.setDeviceStructure("single");
        manualExtract.setSamplePayload("""
                {"SK00D50D1305080":{"S1_ZT_1":{"2026-04-20T04:33:10.000Z":{"ext_power_volt":14.04}}}}
                """);
        dto.setManualExtract(manualExtract);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(7007L, dto);

        assertEquals(
                List.of("S1_ZT_1.ext_power_volt", "S1_ZT_1.humidity"),
                result.getCompareRows().stream()
                        .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                        .sorted()
                        .toList()
        );
        ProductModelGovernanceCompareRowVO runtimeOnlyRow = compareRow(result, "property", "S1_ZT_1.humidity");
        assertEquals("runtime_only", runtimeOnlyRow.getCompareStatus());
        assertEquals("继续观察", runtimeOnlyRow.getSuggestedAction());
        assertNotNull(runtimeOnlyRow.getRuntimeCandidate());
        assertEquals(List.of("humidity", "S1_ZT_1.humidity"), runtimeOnlyRow.getRuntimeCandidate().getRawIdentifiers());
        assertTrue(result.getCompareRows().stream().noneMatch(item -> "humidity".equals(item.getIdentifier())));
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
    void applyGovernanceShouldReturnLaserReleaseBatchIdAfterPublishingFormalFields() {
        when(productMapper.selectById(5005L)).thenReturn(product(
                5005L,
                "nf-monitor-laser-rangefinder-v1",
                "南方测绘 监测型 激光测距仪"
        ));
        when(productContractReleaseService.createBatch(
                eq(5005L),
                eq("phase1-crack"),
                eq("manual_compare_apply"),
                eq(1),
                eq(10001L),
                eq(null),
                eq("manual_compare_apply")
        ))
                .thenReturn(55667L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "value", "激光测距值")));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(5005L, dto, 10001L);

        assertEquals(55667L, result.getReleaseBatchId());
        assertEquals(1, result.getCreatedCount());
    }

    @Test
    void applyGovernanceShouldReturnDeepDisplacementReleaseBatchIdAfterPublishingFormalFields() {
        when(productMapper.selectById(4004L)).thenReturn(product(
                4004L,
                "nf-monitor-deep-displacement-v1",
                "南方测绘 监测型 深部位移监测仪"
        ));
        when(productContractReleaseService.createBatch(
                eq(4004L),
                eq("phase3-deep-displacement"),
                eq("manual_compare_apply"),
                eq(1),
                eq(10001L),
                eq(null),
                eq("manual_compare_apply")
        ))
                .thenReturn(44678L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "dispsX", "顺滑动方向累计变形量")));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(4004L, dto, 10001L);

        assertEquals(44678L, result.getReleaseBatchId());
        assertEquals(1, result.getCreatedCount());
    }

    @Test
    void applyGovernanceShouldNormalizeRainGaugeIdentifiersAndCreateReleaseBatch() {
        when(productMapper.selectById(7007L)).thenReturn(product(
                7007L,
                "nf-monitor-tipping-bucket-rain-gauge-v1",
                "南方测绘 监测型 翻斗式雨量计"
        ));
        when(vendorMetricMappingRuntimeService.normalizeApplyIdentifier(any(Product.class), eq("L3_YL_1.value")))
                .thenReturn("value");
        when(vendorMetricMappingRuntimeService.normalizeApplyIdentifier(any(Product.class), eq("L3_YL_1.totalValue")))
                .thenReturn("totalValue");
        when(productContractReleaseService.createBatch(
                eq(7007L),
                eq("phase4-rain-gauge"),
                eq("manual_compare_apply"),
                eq(2),
                eq(10001L),
                eq(null),
                eq("manual_compare_apply")
        ))
                .thenReturn(77007L);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(
                applyItem("create", null, "property", "L3_YL_1.value", "当前雨量"),
                applyItem("create", null, "property", "L3_YL_1.totalValue", "累计雨量")
        ));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(7007L, dto, 10001L);

        assertEquals(77007L, result.getReleaseBatchId());
        assertEquals(
                List.of("value", "totalValue"),
                result.getAppliedItems().stream()
                        .map(ProductModelGovernanceAppliedItemVO::getIdentifier)
                        .toList()
        );
        verify(productModelMapper, times(2)).insert(any(ProductModel.class));
    }

    @Test
    void applyGovernanceShouldRejectCollectorPayloadContainingChildMetricValue() {
        Product collector = collectorProduct(6006L);
        when(productMapper.selectById(6006L)).thenReturn(collector);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "value", "激光测距值")));

        BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(6006L, dto, 10001L));

        assertEquals("采集器产品不能发布子设备正式字段: value", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void applyGovernanceShouldRejectCollectorPayloadContainingChildSensorState() {
        Product collector = collectorProduct(6006L);
        when(productMapper.selectById(6006L)).thenReturn(collector);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "sensor_state", "传感器状态")));

        BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(6006L, dto, 10001L));

        assertEquals("采集器产品不能发布子设备正式字段: sensor_state", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void applyGovernanceShouldRejectCollectorPayloadContainingDeepDisplacementMetric() {
        Product collector = collectorProduct(6006L);
        when(productMapper.selectById(6006L)).thenReturn(collector);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "dispsX", "顺滑动方向累计变形量")));

        BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(6006L, dto, 10001L));

        assertEquals("采集器产品不能发布子设备正式字段: dispsX", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void applyGovernanceShouldAllowCollectorOwnedRuntimeStatusFields() {
        Product collector = collectorProduct(6006L);
        when(productMapper.selectById(6006L)).thenReturn(collector);
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(
                applyItem("create", null, "property", "temp", "设备温度"),
                applyItem("create", null, "property", "humidity", "设备湿度"),
                applyItem("create", null, "property", "signal_4g", "4G 信号强度")
        ));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(6006L, dto, 10001L);

        assertEquals(3, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(productModelMapper, times(3)).insert(any(ProductModel.class));
    }

    @Test
    void applyGovernanceShouldAllowDirectCollectorRtuRuntimeStatusFields() {
        Product collector = directCollectorRtuProduct(6007L);
        when(productMapper.selectById(6007L)).thenReturn(collector);
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(
                applyItem("create", null, "property", "ext_power_volt", "外接电源电压"),
                applyItem("create", null, "property", "lat", "纬度"),
                applyItem("create", null, "property", "signal_NB", "NB 信号强度"),
                applyItem("create", null, "property", "sw_version", "软件版本")
        ));

        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(6007L, dto, 10001L);

        assertEquals(4, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        verify(productModelMapper, times(4)).insert(any(ProductModel.class));
    }

    @Test
    void applyGovernanceShouldRejectDirectCollectorRtuChildSensorState() {
        Product collector = directCollectorRtuProduct(6007L);
        when(productMapper.selectById(6007L)).thenReturn(collector);

        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(applyItem("create", null, "property", "sensor_state", "传感器状态")));

        BizException ex = assertThrows(BizException.class, () -> productModelService.applyGovernance(6007L, dto, 10001L));

        assertEquals("采集器产品不能发布子设备正式字段: sensor_state", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
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

    private Product collectorProduct(Long id) {
        Product product = product(id, "nf-monitor-collector-v1", "南方测绘 监测型 采集器");
        product.setNodeType(2);
        return product;
    }

    private Product directCollectorRtuProduct(Long id) {
        return product(id, "nf-collect-rtu-v1", "南方测绘 采集型 遥测终端");
    }

    private Device device(Long id, Long productId, String deviceCode) {
        Device device = new Device();
        device.setId(id);
        device.setTenantId(1L);
        device.setProductId(productId);
        device.setDeviceCode(deviceCode);
        device.setDeleted(0);
        return device;
    }

    private DeviceProperty deviceProperty(Long deviceId,
                                          String identifier,
                                          String propertyName,
                                          String propertyValue,
                                          String valueType,
                                          LocalDateTime reportTime) {
        DeviceProperty property = new DeviceProperty();
        property.setDeviceId(deviceId);
        property.setIdentifier(identifier);
        property.setPropertyName(propertyName);
        property.setPropertyValue(propertyValue);
        property.setValueType(valueType);
        property.setReportTime(reportTime);
        return property;
    }

    private VendorMetricEvidence runtimeEvidence(Long productId,
                                                 String rawIdentifier,
                                                 String canonicalIdentifier,
                                                 int evidenceCount,
                                                 LocalDateTime lastSeenTime,
                                                 String sampleValue,
                                                 String valueType) {
        VendorMetricEvidence evidence = new VendorMetricEvidence();
        evidence.setProductId(productId);
        evidence.setRawIdentifier(rawIdentifier);
        evidence.setCanonicalIdentifier(canonicalIdentifier);
        evidence.setEvidenceOrigin("runtime_history");
        evidence.setEvidenceCount(evidenceCount);
        evidence.setLastSeenTime(lastSeenTime);
        evidence.setSampleValue(sampleValue);
        evidence.setValueType(valueType);
        evidence.setDeleted(0);
        return evidence;
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
