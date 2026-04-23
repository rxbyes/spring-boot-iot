package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.RuntimeMetricDisplayRule;
import com.ghlzm.iot.device.mapper.RuntimeMetricDisplayRuleMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.RuntimeMetricDisplayRuleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeMetricDisplayRuleServiceImplTest {

    @Mock
    private RuntimeMetricDisplayRuleMapper mapper;
    @Mock
    private NormativeMetricDefinitionService normativeMetricDefinitionService;

    @Test
    void resolveForDisplayShouldPreferProductScopeOverScenarioScope() {
        RuntimeMetricDisplayRuleServiceImpl service =
                new RuntimeMetricDisplayRuleServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of(
                rule(7101L, 1001L, "S1_ZT_1.humidity", "场景湿度", "%", "ACTIVE", "SCENARIO", "phase4-rain-gauge", null, null),
                rule(7102L, 1001L, "S1_ZT_1.humidity", "产品湿度", "%RH", "ACTIVE", "PRODUCT", null, null, null),
                rule(7103L, null, "S1_ZT_1.humidity", "默认湿度", "%", "ACTIVE", "TENANT_DEFAULT", null, null, null)
        ));

        RuntimeMetricDisplayRuleService.DisplayResolution resolution =
                service.resolveForDisplay(rainGaugeProduct(1001L), "S1_ZT_1.humidity");

        assertEquals(7102L, resolution.ruleId());
        assertEquals("PRODUCT", resolution.scopeType());
        assertEquals("产品湿度", resolution.displayName());
        assertEquals("%RH", resolution.unit());
    }

    @Test
    void resolveForDisplayShouldSupportDeviceFamilyScope() {
        RuntimeMetricDisplayRuleServiceImpl service =
                new RuntimeMetricDisplayRuleServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of(
                rule(7201L, 7007L, "L3_YL_1.value", "当前雨量", "mm", "ACTIVE",
                        "DEVICE_FAMILY", "phase4-rain-gauge", "RAIN_GAUGE", null)
        ));
        when(normativeMetricDefinitionService.listByScenario("phase4-rain-gauge"))
                .thenReturn(List.of(normativeDefinition("phase4-rain-gauge", "value", "RAIN_GAUGE")));

        RuntimeMetricDisplayRuleService.DisplayResolution resolution =
                service.resolveForDisplay(rainGaugeProduct(7007L), "L3_YL_1.value");

        assertEquals(7201L, resolution.ruleId());
        assertEquals("DEVICE_FAMILY", resolution.scopeType());
        assertEquals("当前雨量", resolution.displayName());
        assertEquals("mm", resolution.unit());
    }

    @Test
    void resolveForDisplayShouldReturnNullWhenRuleDoesNotMatch() {
        RuntimeMetricDisplayRuleServiceImpl service =
                new RuntimeMetricDisplayRuleServiceImpl(mapper, normativeMetricDefinitionService);
        when(mapper.selectList(any())).thenReturn(List.of(
                rule(7301L, 1001L, "S1_ZT_1.signal_4g", "4G 信号强度", "dBm", "DISABLED", "PRODUCT", null, null, null)
        ));

        RuntimeMetricDisplayRuleService.DisplayResolution resolution =
                service.resolveForDisplay(rainGaugeProduct(1001L), "S1_ZT_1.humidity");

        assertNull(resolution);
    }

    private RuntimeMetricDisplayRule rule(Long id,
                                          Long productId,
                                          String rawIdentifier,
                                          String displayName,
                                          String unit,
                                          String status,
                                          String scopeType,
                                          String scenarioCode,
                                          String deviceFamily,
                                          String protocolCode) {
        RuntimeMetricDisplayRule rule = new RuntimeMetricDisplayRule();
        rule.setId(id);
        rule.setProductId(productId);
        rule.setRawIdentifier(rawIdentifier);
        rule.setDisplayName(displayName);
        rule.setUnit(unit);
        rule.setStatus(status);
        rule.setScopeType(scopeType);
        rule.setScenarioCode(scenarioCode);
        rule.setDeviceFamily(deviceFamily);
        rule.setProtocolCode(protocolCode);
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

    private Product rainGaugeProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("nf-monitor-tipping-bucket-rain-gauge-v1");
        product.setProductName("翻斗式雨量计");
        product.setProtocolCode("mqtt-json");
        return product;
    }
}
