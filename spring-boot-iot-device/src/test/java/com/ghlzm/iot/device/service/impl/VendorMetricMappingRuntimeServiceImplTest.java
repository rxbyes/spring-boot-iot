package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuntimeServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper mapper;
    @Mock
    private VendorMetricMappingRuleSnapshotMapper snapshotMapper;
    @Mock
    private NormativeMetricDefinitionService normativeMetricDefinitionService;
    @Mock
    private PublishedProductContractSnapshotService snapshotService;
    @Mock
    private MetricIdentifierResolver metricIdentifierResolver;
    @Mock
    private ProtocolSecurityDefinitionProvider definitionProvider;

    @Test
    void resolveForGovernanceShouldPreferLogicalChannelSpecificRule() {
        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(mapper, null);
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(7001L, 1001L, "disp", null, "value", "ACTIVE"),
                mappingRule(7002L, 1001L, "disp", "L1_LF_1", "sensor_state", "ACTIVE")
        ));

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForGovernance(crackProduct(1001L), "disp", "L1_LF_1");

        assertEquals(7002L, resolution.ruleId());
        assertEquals("sensor_state", resolution.targetNormativeIdentifier());
    }

    @Test
    void normalizeApplyIdentifierShouldRejectAmbiguousTargets() {
        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(mapper, null);
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(7001L, 1001L, "disp", null, "value", "ACTIVE"),
                mappingRule(7002L, 1001L, "disp", null, "gpsTotalX", "ACTIVE")
        ));

        BizException error = assertThrows(
                BizException.class,
                () -> service.normalizeApplyIdentifier(crackProduct(1001L), "disp")
        );

        assertEquals("厂商字段映射规则命中多个目标规范字段，请先清理冲突规则: disp", error.getMessage());
    }

    @Test
    void resolveForRuntimeShouldIgnoreDraftConflictAndReturnNull() {
        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(mapper, null);
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(7001L, 1001L, "disp", null, "value", "ACTIVE"),
                mappingRule(7002L, 1001L, "disp", null, "gpsTotalX", "ACTIVE")
        ));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode("mqtt-json");

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForRuntime(crackProduct(1001L), upMessage, "disp", null);

        assertNull(resolution);
    }

    @Test
    void resolveForRuntimeShouldPreferPublishedCanonicalSnapshot() {
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .canonicalAlias("L1_LF_1.value", "value")
                .build();
        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(
                mapper,
                normativeMetricDefinitionService,
                snapshotService,
                metricIdentifierResolver
        );
        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode("mqtt-json");
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(snapshot);
        when(metricIdentifierResolver.resolveForRuntime(snapshot, "L1_LF_1.value"))
                .thenReturn(MetricIdentifierResolution.of(
                        "L1_LF_1.value",
                        "value",
                        MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT
                ));

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForRuntime(crackProduct(1001L), upMessage, "L1_LF_1.value", "L1_LF_1");

        assertEquals("value", resolution.targetNormativeIdentifier());
        assertEquals("L1_LF_1", resolution.logicalChannelCode());
    }

    @Test
    void resolveForGovernanceShouldReadPublishedSnapshotBeforeDraftTable() {
        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setId(8101L);
        snapshot.setRuleId(7101L);
        snapshot.setProductId(1001L);
        snapshot.setPublishedVersionNo(3);
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setSnapshotJson("""
                {"ruleId":7101,"productId":1001,"expectedVersionNo":3,"rawIdentifier":"disp",
                "logicalChannelCode":"L1_LF_1","targetNormativeIdentifier":"value","scopeType":"PRODUCT"}
                """);
        when(snapshotMapper.selectPublishedByProductId(1001L)).thenReturn(List.of(snapshot));

        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(
                mapper,
                snapshotMapper,
                normativeMetricDefinitionService,
                snapshotService,
                metricIdentifierResolver,
                definitionProvider
        );

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForGovernance(crackProduct(1001L), "disp", "L1_LF_1");

        assertEquals("value", resolution.targetNormativeIdentifier());
        verify(mapper, never()).selectList(any());
    }

    @Test
    void resolveForRuntimeShouldSupportDeviceFamilyScopeWhenNoProductRuleExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(8801L, 7007L, "L3_YL_1.value", "L3_YL_1", "value", "ACTIVE",
                        "DEVICE_FAMILY", "phase4-rain-gauge", "RAIN_GAUGE")
        ));
        when(normativeMetricDefinitionService.listByScenario("phase4-rain-gauge"))
                .thenReturn(List.of(normativeDefinition("phase4-rain-gauge", "value", "RAIN_GAUGE")));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode("mqtt-json");

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForRuntime(rainGaugeProduct(7007L), upMessage, "L3_YL_1.value", "L3_YL_1");

        assertEquals(8801L, resolution.ruleId());
        assertEquals("value", resolution.targetNormativeIdentifier());
    }

    @Test
    void resolveForRuntimeShouldFallbackToWaterSurfaceIdentifierWhenNoRuleExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listActive()).thenReturn(List.of(
                normativeDefinitionWithCodes("phase3-water-surface", "temp", "WATER_SURFACE", "L3", "DB"),
                normativeDefinitionWithCodes("phase3-water-surface", "value", "WATER_SURFACE", "L3", "DB")
        ));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode("mqtt-json");

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForRuntime(waterSurfaceProduct(7008L), upMessage, "L3_DB_1.temp", "L3_DB_1");

        assertEquals("temp", resolution.targetNormativeIdentifier());
        assertEquals("L3_DB_1.temp", resolution.rawIdentifier());
        assertEquals("L3_DB_1", resolution.logicalChannelCode());
    }

    @Test
    void normalizeApplyIdentifierShouldFallbackToRadarIdentifierWhenNoRuleExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listActive()).thenReturn(List.of(
                normativeDefinitionWithCodes("phase6-radar", "X", "RADAR", "L4", "LD"),
                normativeDefinitionWithCodes("phase6-radar", "Y", "RADAR", "L4", "LD"),
                normativeDefinitionWithCodes("phase6-radar", "Z", "RADAR", "L4", "LD"),
                normativeDefinitionWithCodes("phase6-radar", "speed", "RADAR", "L4", "LD")
        ));

        String normalized = service.normalizeApplyIdentifier(radarProduct(8008L), "L4_LD_1.speed");

        assertEquals("speed", normalized);
    }

    @Test
    void normalizeApplyIdentifierShouldUseLeaflessValueWhenOnlyOneActiveDefinitionExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listActive()).thenReturn(List.of(
                normativeDefinitionWithCodes("phase3-water-surface", "value", "WATER_SURFACE", "L3", "DB")
        ));

        String normalized = service.normalizeApplyIdentifier(waterSurfaceProduct(7008L), "L3_DB_1");

        assertEquals("value", normalized);
    }

    @Test
    void resolveForRuntimeShouldPreferProtocolFamilyRuleOverBaseProtocolRule() {
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.FamilyDefinition familyDefinition = new IotProperties.Protocol.FamilyDefinition();
        familyDefinition.setFamilyCode("legacy-dp-crack");
        familyDefinition.setProtocolCode("mqtt-json");
        properties.getProtocol().getFamilyDefinitions().put("legacy-dp-crack", familyDefinition);
        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(
                mapper,
                normativeMetricDefinitionService,
                snapshotService,
                metricIdentifierResolver,
                properties
        );
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(9901L, 1001L, "disp", null, "sensor_state", "ACTIVE",
                        "PROTOCOL", null, null, "mqtt-json"),
                mappingRule(9902L, 1001L, "disp", null, "value", "ACTIVE",
                        "PROTOCOL", null, null, "family:legacy-dp-crack")
        ));

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode("mqtt-json");
        DeviceUpProtocolMetadata protocolMetadata = new DeviceUpProtocolMetadata();
        protocolMetadata.setFamilyCodes(List.of("legacy-dp-crack"));
        upMessage.setProtocolMetadata(protocolMetadata);

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForRuntime(crackProduct(1001L), upMessage, "disp", null);

        assertEquals(9902L, resolution.ruleId());
        assertEquals("value", resolution.targetNormativeIdentifier());
    }

    @Test
    void resolveForRuntimeShouldFallbackToNormativePrefixWhenNoMappingRuleExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listActive()).thenReturn(List.of(
                normativeDefinitionWithCodes("phase3-water-surface", "temp", "SURFACE_WATER", "L3", "DB"),
                normativeDefinitionWithCodes("phase3-water-surface", "value", "SURFACE_WATER", "L3", "DB"),
                normativeDefinitionWithCodes("phase5-mud-level", "value", "MUD_LEVEL", "L4", "NW"),
                normativeDefinitionWithCodes("phase6-radar", "speed", "RADAR", "L4", "LD")
        ));
        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode("mqtt-json");

        VendorMetricMappingRuntimeService.MappingResolution waterTemp =
                service.resolveForRuntime(genericProduct(9011L), upMessage, "L3_DB_1.temp", null);
        VendorMetricMappingRuntimeService.MappingResolution mudLevel =
                service.resolveForRuntime(genericProduct(9011L), upMessage, "value", "L4_NW_1");
        VendorMetricMappingRuntimeService.MappingResolution radarSpeed =
                service.resolveForRuntime(genericProduct(9011L), upMessage, "L4_LD_1.speed", null);

        assertEquals("temp", waterTemp.targetNormativeIdentifier());
        assertEquals("value", mudLevel.targetNormativeIdentifier());
        assertEquals("speed", radarSpeed.targetNormativeIdentifier());
        assertNull(waterTemp.ruleId());
    }

    @Test
    void normalizeApplyIdentifierShouldFallbackToNormativePrefixWhenNoMappingRuleExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of());
        when(normativeMetricDefinitionService.listActive()).thenReturn(List.of(
                normativeDefinitionWithCodes("phase5-mud-level", "value", "MUD_LEVEL", "L4", "NW")
        ));

        String normalized = service.normalizeApplyIdentifier(genericProduct(9011L), "L4_NW_1");

        assertEquals("value", normalized);
    }

    @Test
    void resolveForGovernanceShouldMatchDeviceFamilyFromNormativeDefinitions() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(8801L, 7007L, "L3_YL_1.value", "L3_YL_1", "value", "ACTIVE",
                        "DEVICE_FAMILY", "phase4-rain-gauge", "RAIN_GAUGE")
        ));
        when(normativeMetricDefinitionService.listByScenario("phase4-rain-gauge"))
                .thenReturn(List.of(normativeDefinition("phase4-rain-gauge", "value", "RAIN_GAUGE")));

        VendorMetricMappingRuntimeService.MappingResolution resolution =
                service.resolveForGovernance(rainGaugeProduct(7007L), "L3_YL_1.value", "L3_YL_1");

        assertEquals(8801L, resolution.ruleId());
        assertEquals("value", resolution.targetNormativeIdentifier());
    }

    @Test
    void replayForGovernanceShouldReturnPublishedSnapshotHitDetails() {
        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setId(8101L);
        snapshot.setRuleId(7101L);
        snapshot.setProductId(1001L);
        snapshot.setPublishedVersionNo(3);
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setSnapshotJson("""
                {"ruleId":7101,"productId":1001,"expectedVersionNo":3,"rawIdentifier":"disp",
                "logicalChannelCode":"L1_LF_1","targetNormativeIdentifier":"value","scopeType":"PRODUCT"}
                """);
        when(snapshotMapper.selectPublishedByProductId(1001L)).thenReturn(List.of(snapshot));

        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(
                mapper,
                snapshotMapper,
                normativeMetricDefinitionService,
                snapshotService,
                metricIdentifierResolver,
                definitionProvider
        );

        VendorMetricMappingRuntimeServiceImpl.ReplayResolution replay =
                service.replayForGovernance(crackProduct(1001L), "disp", "L1_LF_1");

        assertEquals(true, replay.matched());
        assertEquals("PUBLISHED_SNAPSHOT", replay.hitSource());
        assertEquals("PRODUCT", replay.matchedScopeType());
        assertEquals("value", replay.targetNormativeIdentifier());
    }

    @Test
    void replayForGovernanceShouldReturnDraftRuleHitDetailsWhenNoPublishedSnapshotExists() {
        VendorMetricMappingRuntimeServiceImpl service =
                new VendorMetricMappingRuntimeServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of(
                mappingRule(8801L, 7007L, "L3_YL_1.value", "L3_YL_1", "value", "ACTIVE",
                        "DEVICE_FAMILY", "phase4-rain-gauge", "RAIN_GAUGE")
        ));
        when(mapper.selectById(8801L)).thenReturn(mappingRule(
                8801L, 7007L, "L3_YL_1.value", "L3_YL_1", "value", "ACTIVE",
                "DEVICE_FAMILY", "phase4-rain-gauge", "RAIN_GAUGE"
        ));
        when(normativeMetricDefinitionService.listByScenario("phase4-rain-gauge"))
                .thenReturn(List.of(normativeDefinition("phase4-rain-gauge", "value", "RAIN_GAUGE")));

        VendorMetricMappingRuntimeServiceImpl.ReplayResolution replay =
                service.replayForGovernance(rainGaugeProduct(7007L), "L3_YL_1.value", "L3_YL_1");

        assertEquals(true, replay.matched());
        assertEquals("DRAFT_RULE", replay.hitSource());
        assertEquals("DEVICE_FAMILY", replay.matchedScopeType());
        assertEquals("value", replay.targetNormativeIdentifier());
    }

    @Test
    void replayForGovernanceShouldKeepPublishedSnapshotAsOnlyTruthWhenSnapshotExistsButMisses() {
        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setId(8101L);
        snapshot.setRuleId(7101L);
        snapshot.setProductId(1001L);
        snapshot.setPublishedVersionNo(3);
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setSnapshotJson("""
                {"ruleId":7101,"productId":1001,"expectedVersionNo":3,"rawIdentifier":"other",
                "logicalChannelCode":"L1_LF_1","targetNormativeIdentifier":"value","scopeType":"PRODUCT"}
                """);
        when(snapshotMapper.selectPublishedByProductId(1001L)).thenReturn(List.of(snapshot));

        VendorMetricMappingRuntimeServiceImpl service = new VendorMetricMappingRuntimeServiceImpl(
                mapper,
                snapshotMapper,
                normativeMetricDefinitionService,
                snapshotService,
                metricIdentifierResolver,
                definitionProvider
        );

        VendorMetricMappingRuntimeServiceImpl.ReplayResolution replay =
                service.replayForGovernance(crackProduct(1001L), "disp", "L1_LF_1");

        assertEquals(false, replay.matched());
        assertEquals("PUBLISHED_SNAPSHOT", replay.hitSource());
        verify(mapper, never()).selectList(any());
    }

    private Product crackProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("phase1-crack-product");
        product.setProductName("crack-monitor");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private Product rainGaugeProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("nf-monitor-tipping-bucket-rain-gauge-v1");
        product.setProductName("南方测绘 翻斗式雨量计");
        product.setManufacturer("南方测绘");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private Product genericProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("future-monitor-l3-l4-v1");
        product.setProductName("未来厂商 L3 L4 综合监测设备");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private Product waterSurfaceProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("nf-monitor-water-surface-v1");
        product.setProductName("南方测绘 地表水位监测仪");
        product.setManufacturer("南方测绘");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private Product radarProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("nf-monitor-radar-v1");
        product.setProductName("南方测绘 雷达监测仪");
        product.setManufacturer("南方测绘");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private VendorMetricMappingRule mappingRule(Long id,
                                                Long productId,
                                                String rawIdentifier,
                                                String logicalChannelCode,
                                                String targetNormativeIdentifier,
                                                String status) {
        return mappingRule(id, productId, rawIdentifier, logicalChannelCode, targetNormativeIdentifier, status,
                "PRODUCT", "phase1-crack", null);
    }

    private VendorMetricMappingRule mappingRule(Long id,
                                                Long productId,
                                                String rawIdentifier,
                                                String logicalChannelCode,
                                                String targetNormativeIdentifier,
                                                String status,
                                                String scopeType,
                                                String scenarioCode,
                                                String deviceFamily) {
        return mappingRule(id, productId, rawIdentifier, logicalChannelCode, targetNormativeIdentifier, status,
                scopeType, scenarioCode, deviceFamily, "mqtt-json");
    }

    private VendorMetricMappingRule mappingRule(Long id,
                                                Long productId,
                                                String rawIdentifier,
                                                String logicalChannelCode,
                                                String targetNormativeIdentifier,
                                                String status,
                                                String scopeType,
                                                String scenarioCode,
                                                String deviceFamily,
                                                String protocolCode) {
        VendorMetricMappingRule rule = new VendorMetricMappingRule();
        rule.setId(id);
        rule.setProductId(productId);
        rule.setScopeType(scopeType);
        rule.setRawIdentifier(rawIdentifier);
        rule.setLogicalChannelCode(logicalChannelCode);
        rule.setTargetNormativeIdentifier(targetNormativeIdentifier);
        rule.setScenarioCode(scenarioCode);
        rule.setProtocolCode(protocolCode);
        rule.setDeviceFamily(deviceFamily);
        rule.setStatus(status);
        rule.setDeleted(0);
        return rule;
    }

    private NormativeMetricDefinition normativeDefinition(String scenarioCode, String identifier, String deviceFamily) {
        NormativeMetricDefinition definition = new NormativeMetricDefinition();
        definition.setScenarioCode(scenarioCode);
        definition.setIdentifier(identifier);
        definition.setDeviceFamily(deviceFamily);
        return definition;
    }

    private NormativeMetricDefinition normativeDefinitionWithCodes(String scenarioCode,
                                                                   String identifier,
                                                                   String deviceFamily,
                                                                   String monitorContentCode,
                                                                   String monitorTypeCode) {
        NormativeMetricDefinition definition = normativeDefinition(scenarioCode, identifier, deviceFamily);
        definition.setMonitorContentCode(monitorContentCode);
        definition.setMonitorTypeCode(monitorTypeCode);
        return definition;
    }

}
