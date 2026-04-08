package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper mapper;

    @Test
    void createRuleShouldNormalizeIdentifiersAndPersistProductScope() {
        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("PRODUCT");
        dto.setProtocolCode("mqtt-json");
        dto.setScenarioCode("phase1-crack");
        dto.setRawIdentifier(" TEMP_A ");
        dto.setLogicalChannelCode("L1_LF_1");
        dto.setTargetNormativeIdentifier("value");
        dto.setNormalizationRuleJson("{\"unit\":\"mm\",\"transform\":\"identity\"}");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper);

        Long ruleId = service.createRule(1001L, 10001L, dto);

        assertNotNull(ruleId);
        verify(mapper).insert(argThat((VendorMetricMappingRule rule) ->
                Long.valueOf(1001L).equals(rule.getProductId())
                        && "PRODUCT".equals(rule.getScopeType())
                        && "mqtt-json".equals(rule.getProtocolCode())
                        && "phase1-crack".equals(rule.getScenarioCode())
                        && "temp_a".equals(rule.getRawIdentifier())
                        && "L1_LF_1".equals(rule.getLogicalChannelCode())
                        && "value".equals(rule.getTargetNormativeIdentifier())
                        && "DRAFT".equals(rule.getStatus())
                        && Integer.valueOf(1).equals(rule.getVersionNo())
        ));
    }
}
