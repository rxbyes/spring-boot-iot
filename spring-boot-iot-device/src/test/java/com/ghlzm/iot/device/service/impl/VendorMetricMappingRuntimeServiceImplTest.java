package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
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

    private Product crackProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setProductKey("phase1-crack-product");
        product.setProductName("crack-monitor");
        product.setProtocolCode("mqtt-json");
        return product;
    }

    private VendorMetricMappingRule mappingRule(Long id,
                                                Long productId,
                                                String rawIdentifier,
                                                String logicalChannelCode,
                                                String targetNormativeIdentifier,
                                                String status) {
        VendorMetricMappingRule rule = new VendorMetricMappingRule();
        rule.setId(id);
        rule.setProductId(productId);
        rule.setScopeType("PRODUCT");
        rule.setRawIdentifier(rawIdentifier);
        rule.setLogicalChannelCode(logicalChannelCode);
        rule.setTargetNormativeIdentifier(targetNormativeIdentifier);
        rule.setScenarioCode("phase1-crack");
        rule.setProtocolCode("mqtt-json");
        rule.setStatus(status);
        rule.setDeleted(0);
        return rule;
    }
}
