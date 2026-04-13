package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuntimeServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper mapper;

    @Mock
    private NormativeMetricDefinitionService normativeMetricDefinitionService;
    @Mock
    private PublishedProductContractSnapshotService snapshotService;
    @Mock
    private MetricIdentifierResolver metricIdentifierResolver;

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
        product.setProductName("南方测绘 监测型 翻斗式雨量计");
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
}
