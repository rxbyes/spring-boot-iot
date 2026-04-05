package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelCandidateConfirmDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelManualExtractDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.CommandRecordMapper;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.model.DeviceRelationRule;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateVO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private DeviceMessageLogMapper deviceMessageLogMapper;
    @Mock
    private CommandRecordMapper commandRecordMapper;
    @Mock
    private DeviceRelationService deviceRelationService;

    private ProductModelServiceImpl productModelService;

    @BeforeEach
    void setUp() {
        productModelService = new ProductModelServiceImpl(
                productMapper,
                productModelMapper,
                deviceMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                commandRecordMapper,
                deviceRelationService
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
    void listModelCandidatesShouldGroupPropertyCandidatesAndFlagSuspiciousFields() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 27, 10, 0, 0);
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "existing.temperature", 10)));
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L), device(3002L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "L1_QJ_1.angle", "角度", "double", now.minusMinutes(20)),
                property(3002L, "L1_QJ_1.angle", "角度", "double", now.minusMinutes(10)),
                property(3001L, "S1_ZT_1.signal_4g", "4G 信号强度", "integer", now.minusMinutes(9)),
                property(3001L, "lat", "纬度", "double", now.minusMinutes(8)),
                property(3001L, "codex_verify_temp", "验证温度", "double", now.minusMinutes(7)),
                property(3001L, "singal_NB", "NB 信号", "integer", now.minusMinutes(6))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
                messageLog("property", "{\"properties\":{\"L1_QJ_1\":{\"angle\":1.25}}}", now.minusMinutes(5)),
                messageLog("status", "{\"status\":{\"S1_ZT_1\":{\"signal_4g\":91}}}", now.minusMinutes(4))
        ));
        when(commandRecordMapper.selectList(any())).thenReturn(List.of(commandRecord("property", null)));

        ProductModelCandidateResultVO result = productModelService.listModelCandidates(1001L);

        assertEquals(5, result.getPropertyCandidates().size());
        ProductModelCandidateVO telemetry = candidate(result.getPropertyCandidates(), "L1_QJ_1.angle");
        assertEquals("telemetry", telemetry.getGroupKey());
        assertEquals(Boolean.FALSE, telemetry.getNeedsReview());
        assertEquals(2, telemetry.getEvidenceCount());
        assertTrue(telemetry.getDescription().contains("测点属性"));

        ProductModelCandidateVO deviceStatus = candidate(result.getPropertyCandidates(), "S1_ZT_1.signal_4g");
        assertEquals("device_status", deviceStatus.getGroupKey());
        assertEquals(Boolean.FALSE, deviceStatus.getNeedsReview());
        assertTrue(deviceStatus.getDescription().contains("设备状态"));

        ProductModelCandidateVO location = candidate(result.getPropertyCandidates(), "lat");
        assertEquals("location", location.getGroupKey());
        assertTrue(location.getDescription().contains("定位属性"));

        ProductModelCandidateVO verifyField = candidate(result.getPropertyCandidates(), "codex_verify_temp");
        assertEquals(Boolean.TRUE, verifyField.getNeedsReview());
        assertEquals("needs_review", verifyField.getCandidateStatus());
        assertTrue(verifyField.getDescription().contains("人工归一"));

        ProductModelCandidateVO typoField = candidate(result.getPropertyCandidates(), "singal_NB");
        assertEquals(Boolean.TRUE, typoField.getNeedsReview());
        assertEquals("needs_review", typoField.getCandidateStatus());
        assertTrue(typoField.getDescription().contains("人工归一"));

        assertEquals(5, result.getSummary().getPropertyCandidateCount());
        assertEquals(0, result.getSummary().getEventCandidateCount());
        assertEquals(0, result.getSummary().getServiceCandidateCount());
        assertEquals(2, result.getSummary().getNeedsReviewCount());
        assertEquals(1, result.getSummary().getExistingModelCount());
    }

    @Test
    void listModelCandidatesShouldRefineSouthGnssTelemetryAndStatusFields() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 27, 22, 29, 14);
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "south_gnss_monitor", "南方GNSS位移监测仪"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalX", "double", now.minusMinutes(2)),
                property(3001L, "L1_GP_1.gpsTotalY", "L1_GP_1.gpsTotalY", "double", now.minusMinutes(2)),
                property(3001L, "L1_QJ_1.X", "L1_QJ_1.X", "double", now.minusMinutes(2)),
                property(3001L, "L1_QJ_1.AZI", "L1_QJ_1.AZI", "int", now.minusMinutes(2)),
                property(3001L, "S1_ZT_1.sensor_state.L1_GP_1", "S1_ZT_1.sensor_state.L1_GP_1", "int", now.minusMinutes(1))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
                messageLog("property", "{\"properties\":{\"L1_GP_1\":{\"gpsTotalX\":0.12,\"gpsTotalY\":0.08},\"L1_QJ_1\":{\"X\":1.2,\"AZI\":182},\"S1_ZT_1\":{\"sensor_state\":{\"L1_GP_1\":1}}}}", now.minusMinutes(1))
        ));
        when(commandRecordMapper.selectList(any())).thenReturn(List.of());

        ProductModelCandidateResultVO result = productModelService.listModelCandidates(1001L);

        ProductModelCandidateVO gpsTotalX = candidate(result.getPropertyCandidates(), "L1_GP_1.gpsTotalX");
        assertEquals("telemetry", gpsTotalX.getGroupKey());
        assertEquals(Boolean.FALSE, gpsTotalX.getNeedsReview());
        assertTrue(gpsTotalX.getModelName().contains("GNSS"));
        assertTrue(gpsTotalX.getDescription().contains("测点属性"));

        ProductModelCandidateVO inclinometerX = candidate(result.getPropertyCandidates(), "L1_QJ_1.X");
        assertEquals("telemetry", inclinometerX.getGroupKey());
        assertTrue(inclinometerX.getModelName().contains("倾角"));

        ProductModelCandidateVO azimuth = candidate(result.getPropertyCandidates(), "L1_QJ_1.AZI");
        assertEquals("telemetry", azimuth.getGroupKey());
        assertTrue(azimuth.getModelName().contains("方位"));

        ProductModelCandidateVO gnssState = candidate(result.getPropertyCandidates(), "S1_ZT_1.sensor_state.L1_GP_1");
        assertEquals("device_status", gnssState.getGroupKey());
        assertEquals(Boolean.FALSE, gnssState.getNeedsReview());
        assertTrue(gnssState.getModelName().contains("传感器状态"));
    }

    @Test
    void listModelCandidatesShouldIgnoreTransportWrapperFieldsFromMessageLogs() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 3, 12, 0, 0);
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of());
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
                messageLog("property", "{\"bodies\":{\"body\":\"cipher-text\"},\"header\":{\"appId\":\"62000001\"},\"properties\":{\"L1_QJ_1\":{\"angle\":1.25}}}", now)
        ));
        when(commandRecordMapper.selectList(any())).thenReturn(List.of());

        ProductModelCandidateResultVO result = productModelService.listModelCandidates(1001L);

        assertEquals(1, result.getPropertyCandidates().size());
        assertIterableEquals(
                List.of("L1_QJ_1.angle"),
                result.getPropertyCandidates().stream().map(ProductModelCandidateVO::getIdentifier).toList()
        );
    }

    @Test
    void listModelCandidatesShouldReturnEmptyEventAndServiceCandidatesWhenEvidenceMissing() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 27, 10, 0, 0);
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "S1_ZT_1.temp", "设备温度", "double", now.minusMinutes(12))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
                messageLog("status", "{\"status\":{\"S1_ZT_1\":{\"temp\":36.5}}}", now.minusMinutes(6))
        ));
        when(commandRecordMapper.selectList(any())).thenThrow(new RuntimeException("Unknown column 'service_identifier'"));

        ProductModelCandidateResultVO result = productModelService.listModelCandidates(1001L);

        assertTrue(result.getEventCandidates().isEmpty());
        assertTrue(result.getServiceCandidates().isEmpty());
        assertTrue(result.getSummary().getEventHint().contains("暂无真实事件证据"));
        assertTrue(result.getSummary().getServiceHint().contains("iot_command_record"));
        assertTrue(result.getSummary().getServiceHint().contains("字段"));
    }

    @Test
    void manualExtractShouldFlattenSingleDeviceBusinessSampleIntoPropertyCandidates() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
        dto.setSampleType("business");
        dto.setSamplePayload("""
                {"SK11E80D1307426AZ":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"AZI":-8.6772,"X":-0.0376,"Y":-0.0567,"Z":-0.0292,"angle":83.0074}},"L1_JS_1":{"2026-03-31T04:05:55.000Z":{"gX":-0.2396,"gY":-1.1563,"gZ":-0.3125}},"L1_LF_1":{"2026-03-31T04:05:55.000Z":{"value":0.0305}}}}
                """);

        ProductModelCandidateResultVO result = productModelService.manualExtractModelCandidates(1001L, dto);

        assertEquals(9, result.getPropertyCandidates().size());
        assertEquals("manual", result.getSummary().getExtractionMode());
        assertEquals("business", result.getSummary().getSampleType());
        assertEquals("SK11E80D1307426AZ", result.getSummary().getSampleDeviceCode());
        assertEquals(9, result.getSummary().getPropertyEvidenceCount());
        assertTrue(result.getEventCandidates().isEmpty());
        assertTrue(result.getServiceCandidates().isEmpty());

        ProductModelCandidateVO angle = candidate(result.getPropertyCandidates(), "L1_QJ_1.angle");
        assertEquals("telemetry", angle.getGroupKey());
        assertEquals(Boolean.FALSE, angle.getNeedsReview());
        assertEquals(List.of("manual_sample"), angle.getSourceTables());

        ProductModelCandidateVO crack = candidate(result.getPropertyCandidates(), "L1_LF_1.value");
        assertEquals("double", crack.getDataType());
        assertTrue(crack.getDescription().contains("手动"));
    }

    @Test
    void manualExtractShouldMarkOtherSampleCandidatesForReviewAndIgnoreArrays() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
        dto.setSampleType("other");
        dto.setSamplePayload("""
                {"SK11E80D1307426AZ":{"S1_ZT_1":{"2026-03-31T04:05:55.000Z":{"temp":22,"signal_4g":-67,"wave":[1,2,3]}}}}
                """);

        ProductModelCandidateResultVO result = productModelService.manualExtractModelCandidates(1001L, dto);

        ProductModelCandidateVO temp = candidate(result.getPropertyCandidates(), "S1_ZT_1.temp");
        assertEquals("device_status", temp.getGroupKey());
        assertEquals(Boolean.TRUE, temp.getNeedsReview());
        assertEquals("needs_review", temp.getCandidateStatus());
        assertTrue(temp.getReviewReason().contains("其他数据"));
        assertEquals(1, result.getSummary().getIgnoredFieldCount());
    }

    @Test
    void manualExtractShouldRejectMultipleDeviceRoots() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
        dto.setSampleType("business");
        dto.setSamplePayload("""
                {"device-a":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"X":1}}},"device-b":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"X":2}}}}
                """);

        BizException ex = assertThrows(BizException.class, () -> productModelService.manualExtractModelCandidates(1001L, dto));

        assertEquals("单次只支持解析一个设备样本", ex.getMessage());
    }

    @Test
    void manualExtractShouldCanonicalizeCollectorPayloadForChildProductWhenRelationChildModeEnabled() {
        when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "裂缝监测仪"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceRelationService.listEnabledRulesByParentDeviceCode("SK00EA0D1307986")).thenReturn(List.of(
                relationRule("SK00EA0D1307986", "L1_LF_1", "202018143", 2002L, "south-crack-sensor-v1", "LF_VALUE", "SENSOR_STATE"),
                relationRule("SK00EA0D1307986", "L1_LF_2", "202018135", 2002L, "south-crack-sensor-v1", "LF_VALUE", "SENSOR_STATE")
        ));

        ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
        dto.setSampleType("business");
        dto.setExtractMode("relation_child");
        dto.setSourceDeviceCode("SK00EA0D1307986");
        dto.setSamplePayload("""
                {"SK00EA0D1307986":{"S1_ZT_1":{"2026-04-04T22:10:35.000Z":{"temp":18.69,"sensor_state":{"L1_LF_1":0,"L1_LF_2":0}}},"L1_LF_1":{"2026-04-04T22:10:35.000Z":10.86},"L1_LF_2":{"2026-04-04T22:10:35.000Z":6.95}}}
                """);

        ProductModelCandidateResultVO result = productModelService.manualExtractModelCandidates(2002L, dto);

        assertEquals(List.of("sensor_state", "value"), result.getPropertyCandidates().stream()
                .map(ProductModelCandidateVO::getIdentifier)
                .sorted()
                .toList());
        assertEquals(4, result.getSummary().getPropertyEvidenceCount());
        assertEquals("SK00EA0D1307986", result.getSummary().getSampleDeviceCode());
        assertEquals("telemetry", candidate(result.getPropertyCandidates(), "value").getGroupKey());
        assertEquals("device_status", candidate(result.getPropertyCandidates(), "sensor_state").getGroupKey());
    }

    @Test
    void confirmModelCandidatesShouldOnlyInsertConfirmedItemsAndSkipExistingIdentifiers() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "temperature", 10)));

        ProductModelCandidateConfirmDTO dto = new ProductModelCandidateConfirmDTO();
        dto.setItems(List.of(
                confirmItem("temperature", "温度", "double", "已存在正式模型"),
                confirmItem("S1_ZT_1.signal_4g", "4G 信号强度", "integer", "归属设备状态属性")
        ));

        ProductModelCandidateSummaryVO summary = productModelService.confirmModelCandidates(1001L, dto);

        ArgumentCaptor<ProductModel> captor = ArgumentCaptor.forClass(ProductModel.class);
        verify(productModelMapper).insert(captor.capture());
        assertEquals("S1_ZT_1.signal_4g", captor.getValue().getIdentifier());
        assertEquals("property", captor.getValue().getModelType());
        assertEquals("integer", captor.getValue().getDataType());
        assertEquals(1, summary.getCreatedCount());
        assertEquals(1, summary.getConflictCount());
        assertEquals(1, summary.getSkippedCount());
    }

    @Test
    void compareGovernanceShouldBuildRowsAcrossManualRuntimeAndFormalEvidence() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "L1_QJ_1.angle", 10)));
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "S1_ZT_1.signal_4g", "4G 信号强度", "int", LocalDateTime.of(2026, 4, 1, 10, 0))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
                messageLog("event", "{\"eventId\":\"alarmRaised\"}", LocalDateTime.of(2026, 4, 1, 10, 2))
        ));
        when(commandRecordMapper.selectList(any())).thenReturn(List.of(commandRecord("service", "reboot")));

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        dto.setIncludeRuntimeCandidates(true);
        dto.setManualDraftItems(List.of(manualDraftItem("service", "reboot", "重启设备")));

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

        assertEquals("formal_exists", compareRow(result, "property", "L1_QJ_1.angle").getCompareStatus());
        assertEquals("runtime_only", compareRow(result, "property", "S1_ZT_1.signal_4g").getCompareStatus());
        assertEquals("double_aligned", compareRow(result, "service", "reboot").getCompareStatus());
    }

    @Test
    void compareGovernanceShouldFilterCollectorChildOwnedRuntimeFieldsByRelationRegistry() {
        Device collector = device(3001L);
        collector.setDeviceCode("SK00EA0D1307986");
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "south-collector-rtu-v1", "采集中枢"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(collector));
        when(deviceRelationService.listEnabledRulesByParentDeviceCode("SK00EA0D1307986")).thenReturn(List.of(
                relationRule("SK00EA0D1307986", "L1_LF_1", "202018143", 2002L, "south-crack-sensor-v1", "LF_VALUE", "SENSOR_STATE")
        ));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "L1_LF_1", "裂缝值", "double", LocalDateTime.of(2026, 4, 4, 22, 10, 35)),
                property(3001L, "S1_ZT_1.signal_4g", "4G 信号强度", "integer", LocalDateTime.of(2026, 4, 4, 22, 10, 35))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
        when(commandRecordMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        dto.setIncludeRuntimeCandidates(true);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

        assertEquals("runtime_only", compareRow(result, "property", "S1_ZT_1.signal_4g").getCompareStatus());
        assertTrue(result.getCompareRows().stream().noneMatch(row -> "L1_LF_1".equals(row.getIdentifier())));
    }

    @Test
    void compareGovernanceShouldFlagSameIdentifierDefinitionMismatchAsConflict() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of(existingEventModel(2001L, "alarmRaised", 10, "info")));
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of());
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(
                messageLog("event", "{\"eventId\":\"alarmRaised\"}", LocalDateTime.of(2026, 4, 1, 11, 0))
        ));
        when(commandRecordMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        dto.setManualDraftItems(List.of(manualEventDraft("alarmRaised", "warning")));

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

        assertEquals("suspected_conflict", compareRow(result, "event", "alarmRaised").getCompareStatus());
        assertEquals("人工裁决", compareRow(result, "event", "alarmRaised").getSuggestedAction());
    }

    @Test
    void compareGovernanceShouldBuildNormativeRowsForIntegratedPreset() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "south-survey-multi-detector-v1", "南方测绘多维检测仪"));
        when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "L1_QJ_1.AZI", 50)));
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "signal_4g", "4G 信号强度", "int", LocalDateTime.of(2026, 4, 4, 9, 0)),
                property(3001L, "X", "倾角 X", "double", LocalDateTime.of(2026, 4, 4, 9, 1))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
        when(commandRecordMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        dto.setGovernanceMode("normative");
        dto.setNormativePresetCode("landslide-integrated-tilt-accel-crack-v1");
        dto.setSelectedNormativeIdentifiers(List.of("L1_QJ_1.X", "S1_ZT_1.signal_4g", "L1_QJ_1.AZI"));
        dto.setIncludeRuntimeCandidates(true);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);
        ProductModelGovernanceCompareRowVO inclinometerX = compareRow(result, "property", "L1_QJ_1.X");

        assertEquals("double_aligned", inclinometerX.getCompareStatus());
        assertEquals("normative", inclinometerX.getManualCandidate().getEvidenceOrigin());
        assertEquals("°", inclinometerX.getManualCandidate().getUnit());
        assertEquals("表 B.1", inclinometerX.getManualCandidate().getNormativeSource());
        assertIterableEquals(List.of("X", "angleX"), inclinometerX.getManualCandidate().getRawIdentifiers());
        assertEquals("runtime", inclinometerX.getRuntimeCandidate().getEvidenceOrigin());
        assertIterableEquals(List.of("X"), inclinometerX.getRuntimeCandidate().getRawIdentifiers());
        assertEquals("double_aligned", compareRow(result, "property", "S1_ZT_1.signal_4g").getCompareStatus());
        assertEquals("formal_exists", compareRow(result, "property", "L1_QJ_1.AZI").getCompareStatus());
    }

    @Test
    void compareGovernanceShouldKeepUnmappedRuntimeFieldAsRuntimeOnly() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L, "south-survey-multi-detector-v1", "南方测绘多维检测仪"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property(3001L, "mysteryField", "未知字段", "double", LocalDateTime.of(2026, 4, 4, 9, 2))
        ));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
        when(commandRecordMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        dto.setGovernanceMode("normative");
        dto.setNormativePresetCode("landslide-integrated-tilt-accel-crack-v1");
        dto.setIncludeRuntimeCandidates(true);

        ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

        assertEquals("runtime_only", compareRow(result, "property", "mysteryField").getCompareStatus());
        assertTrue(compareRow(result, "property", "mysteryField").getRiskFlags().contains("manual_missing"));
    }

    @Test
    void compareGovernanceShouldRejectInapplicableNormativePresetForWarningProduct() {
        when(productMapper.selectById(1001L))
                .thenReturn(product(1001L, "zhd-warning-sound-light-alarm-v1", "中海达 预警型 声光报警器"));
        when(productModelMapper.selectList(any())).thenReturn(List.of());

        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        dto.setGovernanceMode("normative");
        dto.setNormativePresetCode("landslide-integrated-tilt-accel-crack-v1");

        BizException ex = assertThrows(BizException.class, () -> productModelService.compareGovernance(1001L, dto));

        assertEquals("当前产品不适用规范预设: landslide-integrated-tilt-accel-crack-v1", ex.getMessage());
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

    private Device device(Long id) {
        Device device = new Device();
        device.setId(id);
        device.setProductId(1001L);
        device.setDeviceCode("device-" + id);
        device.setDeviceName("设备-" + id);
        device.setDeleted(0);
        return device;
    }

    private DeviceProperty property(Long deviceId, String identifier, String propertyName, String valueType, LocalDateTime reportTime) {
        DeviceProperty property = new DeviceProperty();
        property.setDeviceId(deviceId);
        property.setIdentifier(identifier);
        property.setPropertyName(propertyName);
        property.setValueType(valueType);
        property.setReportTime(reportTime);
        property.setUpdateTime(reportTime);
        return property;
    }

    private DeviceMessageLog messageLog(String messageType, String payload, LocalDateTime reportTime) {
        DeviceMessageLog log = new DeviceMessageLog();
        log.setProductId(1001L);
        log.setMessageType(messageType);
        log.setPayload(payload);
        log.setReportTime(reportTime);
        log.setCreateTime(reportTime);
        return log;
    }

    private CommandRecord commandRecord(String commandType, String serviceIdentifier) {
        CommandRecord record = new CommandRecord();
        record.setCommandType(commandType);
        record.setServiceIdentifier(serviceIdentifier);
        return record;
    }

    private ProductModelCandidateVO candidate(List<ProductModelCandidateVO> candidates, String identifier) {
        return candidates.stream()
                .filter(item -> identifier.equals(item.getIdentifier()))
                .findFirst()
                .orElseThrow();
    }

    private ProductModelCandidateConfirmDTO.ProductModelCandidateConfirmItem confirmItem(String identifier,
                                                                                         String modelName,
                                                                                         String dataType,
                                                                                         String description) {
        ProductModelCandidateConfirmDTO.ProductModelCandidateConfirmItem item =
                new ProductModelCandidateConfirmDTO.ProductModelCandidateConfirmItem();
        item.setModelType("property");
        item.setIdentifier(identifier);
        item.setModelName(modelName);
        item.setDataType(dataType);
        item.setDescription(description);
        item.setSortNo(10);
        item.setRequiredFlag(0);
        return item;
    }

    private ProductModelGovernanceCompareDTO.ManualDraftItem manualDraftItem(String modelType,
                                                                             String identifier,
                                                                             String modelName) {
        ProductModelGovernanceCompareDTO.ManualDraftItem item = new ProductModelGovernanceCompareDTO.ManualDraftItem();
        item.setModelType(modelType);
        item.setIdentifier(identifier);
        item.setModelName(modelName);
        return item;
    }

    private ProductModelGovernanceCompareDTO.ManualDraftItem manualEventDraft(String identifier, String eventType) {
        ProductModelGovernanceCompareDTO.ManualDraftItem item = manualDraftItem("event", identifier, "事件-" + identifier);
        item.setEventType(eventType);
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

    private DeviceRelationRule relationRule(String parentDeviceCode,
                                            String logicalChannelCode,
                                            String childDeviceCode,
                                            Long childProductId,
                                            String childProductKey,
                                            String canonicalizationStrategy,
                                            String statusMirrorStrategy) {
        DeviceRelationRule rule = new DeviceRelationRule();
        rule.setParentDeviceCode(parentDeviceCode);
        rule.setLogicalChannelCode(logicalChannelCode);
        rule.setChildDeviceCode(childDeviceCode);
        rule.setChildProductId(childProductId);
        rule.setChildProductKey(childProductKey);
        rule.setCanonicalizationStrategy(canonicalizationStrategy);
        rule.setStatusMirrorStrategy(statusMirrorStrategy);
        return rule;
    }
}
