package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.alarm.vo.DeviceThresholdOverviewVO;
import com.ghlzm.iot.alarm.vo.RuleDefinitionEffectivePreviewVO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceThresholdReadServiceImplTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private ProductService productService;
    @Mock
    private RiskPointBindingMaintenanceService bindingMaintenanceService;
    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;
    @Mock
    private RuleDefinitionService ruleDefinitionService;

    @Test
    void getDeviceThresholdsShouldPreferBindingRuleOverProductDefault() {
        Device device = buildDevice(8001L, 1001L, 1L, "crack-device-01", "North Crack 01");
        Product product = buildProduct(1001L, "monitor-product", "North Slope Product");
        DeviceMetricOptionVO formalMetric = buildMetric(7001L, "value", "Crack Value");
        RiskPointDevice binding = buildBinding(9101L, 8001L, 7001L, "value", "Crack Value");

        when(deviceService.getRequiredById(99L, 8001L)).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);
        when(bindingMaintenanceService.listFormalBindingMetricOptions(8001L, 99L)).thenReturn(List.of(formalMetric));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));

        RuleDefinition productRule = buildRule(3001L, "product-default", "PRODUCT", "value", "Crack Value",
                "value >= 6", "orange", 1001L, null, null);
        RuleDefinition bindingRule = buildRule(3002L, "binding-red", "BINDING", "value", "Crack Value",
                "value >= 8", "red", 1001L, 8001L, 9101L);
        when(ruleDefinitionService.previewEffectiveRule(eq(1L), eq(7001L), eq("value"), eq(1001L), eq("MONITORING"),
                eq(8001L), isNull()))
                .thenReturn(preview(productRule, "PRODUCT",
                        candidate(3001L, "product-default", "PRODUCT", "value", "Crack Value", "value >= 6", "orange",
                                "产品 1001", true)));
        when(ruleDefinitionService.previewEffectiveRule(1L, 7001L, "value", 1001L, "MONITORING", 8001L, 9101L))
                .thenReturn(preview(bindingRule, "BINDING",
                        candidate(3002L, "binding-red", "BINDING", "value", "Crack Value", "value >= 8", "red",
                                "绑定 9101", true),
                        candidate(3001L, "product-default", "PRODUCT", "value", "Crack Value", "value >= 6", "orange",
                                "产品 1001", false)));

        DeviceThresholdOverviewVO overview = buildService().getDeviceThresholds(99L, 8001L);

        assertEquals("crack-device-01", overview.getDeviceCode());
        assertEquals(1, overview.getMatchedMetricCount());
        assertEquals(0, overview.getMissingMetricCount());
        assertEquals(1, overview.getItems().size());
        var item = overview.getItems().get(0);
        assertEquals("value", item.getMetricIdentifier());
        assertEquals(List.of("binding-red"), item.getEffectiveRules().stream().map(rule -> rule.getRuleName()).toList());
        assertEquals(List.of("绑定个性"), item.getEffectiveRules().stream().map(rule -> rule.getSourceLabel()).toList());
        assertEquals(List.of("绑定 9101"), item.getBindingRules().stream().map(rule -> rule.getTargetLabel()).toList());
        assertEquals(List.of("产品默认"), item.getProductRules().stream().map(rule -> rule.getSourceLabel()).toList());
    }

    @Test
    void getDeviceThresholdsShouldFallBackToProductDefaultWhenNoSpecificRuleExists() {
        Device device = buildDevice(8002L, 1001L, 1L, "crack-device-02", "North Crack 02");
        Product product = buildProduct(1001L, "monitor-product", "North Slope Product");
        DeviceMetricOptionVO formalMetric = buildMetric(7001L, "value", "Crack Value");
        RuleDefinition productRule = buildRule(3001L, "product-default", "PRODUCT", "value", "Crack Value",
                "value >= 6", "orange", 1001L, null, null);

        when(deviceService.getRequiredById(99L, 8002L)).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);
        when(bindingMaintenanceService.listFormalBindingMetricOptions(8002L, 99L)).thenReturn(List.of(formalMetric));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionService.previewEffectiveRule(eq(1L), eq(7001L), eq("value"), eq(1001L), eq("MONITORING"),
                eq(8002L), isNull()))
                .thenReturn(preview(productRule, "PRODUCT",
                        candidate(3001L, "product-default", "PRODUCT", "value", "Crack Value", "value >= 6", "orange",
                                "产品 1001", true)));

        DeviceThresholdOverviewVO overview = buildService().getDeviceThresholds(99L, 8002L);

        assertEquals(1, overview.getMatchedMetricCount());
        assertEquals(0, overview.getMissingMetricCount());
        assertEquals(1, overview.getItems().size());
        var item = overview.getItems().get(0);
        assertEquals(List.of("product-default"), item.getEffectiveRules().stream().map(rule -> rule.getRuleName()).toList());
        assertTrue(item.getBindingRules().isEmpty());
        assertEquals(List.of("产品默认"), item.getProductRules().stream().map(rule -> rule.getSourceLabel()).toList());
    }

    @Test
    void getDeviceThresholdsShouldKeepBindingOnlyMetricWhenNoFormalMetricExists() {
        Device device = buildDevice(8003L, 1001L, 1L, "tilt-device-01", "North Tilt 01");
        Product product = buildProduct(1001L, "monitor-product", "North Slope Product");
        RiskPointDevice binding = buildBinding(9201L, 8003L, 7002L, "tilt", "Tilt");
        RuleDefinition bindingRule = buildRule(3101L, "binding-tilt", "BINDING", "tilt", "Tilt",
                "tilt >= 3", "yellow", 1001L, 8003L, 9201L);

        when(deviceService.getRequiredById(99L, 8003L)).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);
        when(bindingMaintenanceService.listFormalBindingMetricOptions(8003L, 99L)).thenReturn(List.of());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(ruleDefinitionService.previewEffectiveRule(eq(1L), eq(7002L), eq("tilt"), eq(1001L), eq("MONITORING"),
                eq(8003L), isNull()))
                .thenReturn(preview(null, null));
        when(ruleDefinitionService.previewEffectiveRule(1L, 7002L, "tilt", 1001L, "MONITORING", 8003L, 9201L))
                .thenReturn(preview(bindingRule, "BINDING",
                        candidate(3101L, "binding-tilt", "BINDING", "tilt", "Tilt", "tilt >= 3", "yellow",
                                "绑定 9201", true)));

        DeviceThresholdOverviewVO overview = buildService().getDeviceThresholds(99L, 8003L);

        assertEquals(1, overview.getItems().size());
        assertEquals(1, overview.getMatchedMetricCount());
        assertEquals(0, overview.getMissingMetricCount());
        var item = overview.getItems().get(0);
        assertEquals("tilt", item.getMetricIdentifier());
        assertEquals(List.of("binding-tilt"), item.getEffectiveRules().stream().map(rule -> rule.getRuleName()).toList());
        assertEquals(List.of("绑定 9201"), item.getBindingRules().stream().map(rule -> rule.getTargetLabel()).toList());
        assertTrue(item.getProductRules().isEmpty());
    }

    private DeviceThresholdReadServiceImpl buildService() {
        return new DeviceThresholdReadServiceImpl(
                deviceService,
                productService,
                bindingMaintenanceService,
                riskPointDeviceMapper,
                ruleDefinitionService
        );
    }

    private static Device buildDevice(Long deviceId, Long productId, Long tenantId, String deviceCode, String deviceName) {
        Device device = new Device();
        device.setId(deviceId);
        device.setProductId(productId);
        device.setTenantId(tenantId);
        device.setDeviceCode(deviceCode);
        device.setDeviceName(deviceName);
        return device;
    }

    private static Product buildProduct(Long productId, String productKey, String productName) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey(productKey);
        product.setProductName(productName);
        return product;
    }

    private static DeviceMetricOptionVO buildMetric(Long riskMetricId, String metricIdentifier, String metricName) {
        DeviceMetricOptionVO option = new DeviceMetricOptionVO();
        option.setRiskMetricId(riskMetricId);
        option.setIdentifier(metricIdentifier);
        option.setName(metricName);
        return option;
    }

    private static RiskPointDevice buildBinding(Long bindingId,
                                                Long deviceId,
                                                Long riskMetricId,
                                                String metricIdentifier,
                                                String metricName) {
        RiskPointDevice binding = new RiskPointDevice();
        binding.setId(bindingId);
        binding.setDeviceId(deviceId);
        binding.setRiskMetricId(riskMetricId);
        binding.setMetricIdentifier(metricIdentifier);
        binding.setMetricName(metricName);
        binding.setDeleted(0);
        return binding;
    }

    private static RuleDefinition buildRule(Long ruleId,
                                            String ruleName,
                                            String ruleScope,
                                            String metricIdentifier,
                                            String metricName,
                                            String expression,
                                            String alarmLevel,
                                            Long productId,
                                            Long deviceId,
                                            Long bindingId) {
        RuleDefinition rule = new RuleDefinition();
        rule.setId(ruleId);
        rule.setRuleName(ruleName);
        rule.setRuleScope(ruleScope);
        rule.setMetricIdentifier(metricIdentifier);
        rule.setMetricName(metricName);
        rule.setExpression(expression);
        rule.setAlarmLevel(alarmLevel);
        rule.setProductId(productId);
        rule.setDeviceId(deviceId);
        rule.setRiskPointDeviceId(bindingId);
        return rule;
    }

    private static RuleDefinitionEffectivePreviewVO preview(RuleDefinition matchedRule,
                                                            String matchedScope,
                                                            RuleDefinitionEffectivePreviewVO.Candidate... candidates) {
        RuleDefinitionEffectivePreviewVO preview = new RuleDefinitionEffectivePreviewVO();
        preview.setHasMatchedRule(matchedRule != null);
        preview.setMatchedRule(matchedRule);
        preview.setMatchedScope(matchedScope);
        preview.setMatchedScopeText(scopeText(matchedScope));
        preview.setCandidates(new ArrayList<>(List.of(candidates)));
        return preview;
    }

    private static RuleDefinitionEffectivePreviewVO.Candidate candidate(Long ruleId,
                                                                        String ruleName,
                                                                        String ruleScope,
                                                                        String metricIdentifier,
                                                                        String metricName,
                                                                        String expression,
                                                                        String alarmLevel,
                                                                        String scopeTarget,
                                                                        boolean selected) {
        RuleDefinitionEffectivePreviewVO.Candidate candidate = new RuleDefinitionEffectivePreviewVO.Candidate();
        candidate.setRuleId(ruleId);
        candidate.setRuleName(ruleName);
        candidate.setRuleScope(ruleScope);
        candidate.setRuleScopeText(scopeText(ruleScope));
        candidate.setMetricIdentifier(metricIdentifier);
        candidate.setMetricName(metricName);
        candidate.setExpression(expression);
        candidate.setAlarmLevel(alarmLevel);
        candidate.setScopeTarget(scopeTarget);
        candidate.setMatchedContext(true);
        candidate.setSelected(selected);
        return candidate;
    }

    private static String scopeText(String scope) {
        if (scope == null) {
            return null;
        }
        return switch (scope) {
            case "PRODUCT" -> "产品默认";
            case "DEVICE" -> "设备个性";
            case "BINDING" -> "绑定个性";
            case "PRODUCT_TYPE" -> "产品类型模板";
            default -> "测点通用";
        };
    }
}
