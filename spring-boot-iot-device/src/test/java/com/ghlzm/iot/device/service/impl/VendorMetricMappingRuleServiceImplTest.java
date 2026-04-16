package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleVO;
import com.ghlzm.iot.framework.config.IotProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper mapper;
    @Mock
    private VendorMetricMappingRuleSnapshotMapper snapshotMapper;

    @Test
    void pageRulesShouldMergeLatestPublishedSnapshotState() {
        VendorMetricMappingRule rule = new VendorMetricMappingRule();
        rule.setId(7101L);
        rule.setProductId(1001L);
        rule.setScopeType("PRODUCT");
        rule.setRawIdentifier("disp");
        rule.setTargetNormativeIdentifier("value");
        rule.setStatus("DRAFT");
        rule.setVersionNo(4);

        Page<VendorMetricMappingRule> page = new Page<>(1, 10);
        page.setRecords(List.of(rule));
        page.setTotal(1L);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        VendorMetricMappingRuleSnapshot snapshot = new VendorMetricMappingRuleSnapshot();
        snapshot.setId(8101L);
        snapshot.setRuleId(7101L);
        snapshot.setProductId(1001L);
        snapshot.setApprovalOrderId(99001L);
        snapshot.setPublishedVersionNo(3);
        snapshot.setLifecycleStatus("PUBLISHED");
        when(snapshotMapper.selectPublishedByProductId(1001L)).thenReturn(List.of(snapshot));

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper, snapshotMapper, (IotProperties) null);

        VendorMetricMappingRuleVO row = service.pageRules(1001L, null, 1L, 10L).getRecords().get(0);

        assertEquals("PRODUCT", row.getScopeType());
        assertEquals("PUBLISHED", row.getPublishedStatus());
        assertEquals(3, row.getPublishedVersionNo());
        assertEquals(99001L, row.getApprovalOrderId());
    }

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

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper, snapshotMapper, (IotProperties) null);

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

    @Test
    void createRuleShouldAllowDeviceFamilyScopeWhenFamilyIsProvided() {
        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("device_family");
        dto.setDeviceFamily(" rain_gauge ");
        dto.setRawIdentifier(" L3_YL_1.value ");
        dto.setLogicalChannelCode("L3_YL_1");
        dto.setTargetNormativeIdentifier("value");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper, snapshotMapper, (IotProperties) null);

        service.createRule(1001L, 10001L, dto);

        verify(mapper).insert(argThat((VendorMetricMappingRule rule) ->
                Long.valueOf(1001L).equals(rule.getProductId())
                        && "DEVICE_FAMILY".equals(rule.getScopeType())
                        && "rain_gauge".equals(rule.getDeviceFamily())
                        && "l3_yl_1.value".equals(rule.getRawIdentifier())
                        && "value".equals(rule.getTargetNormativeIdentifier())
        ));
    }

    @Test
    void createRuleShouldRejectConflictingTargetUnderSameScopeSignature() {
        VendorMetricMappingRule existing = new VendorMetricMappingRule();
        existing.setId(7001L);
        existing.setProductId(1001L);
        existing.setScopeType("DEVICE_FAMILY");
        existing.setDeviceFamily("rain_gauge");
        existing.setRawIdentifier("l3_yl_1.value");
        existing.setLogicalChannelCode("L3_YL_1");
        existing.setTargetNormativeIdentifier("totalValue");
        existing.setDeleted(0);
        when(mapper.selectList(any())).thenReturn(List.of(existing));

        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("DEVICE_FAMILY");
        dto.setDeviceFamily("rain_gauge");
        dto.setRawIdentifier("L3_YL_1.value");
        dto.setLogicalChannelCode("L3_YL_1");
        dto.setTargetNormativeIdentifier("value");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper, snapshotMapper, (IotProperties) null);

        BizException error = assertThrows(BizException.class, () -> service.createRule(1001L, 10001L, dto));

        assertEquals("厂商字段映射规则存在冲突，请先清理同 scope 下的重复目标: l3_yl_1.value", error.getMessage());
        verify(mapper, never()).insert(any(VendorMetricMappingRule.class));
    }

    @Test
    void createRuleShouldAcceptConfiguredProtocolFamilySelector() {
        IotProperties properties = new IotProperties();
        IotProperties.Protocol.FamilyDefinition familyDefinition = new IotProperties.Protocol.FamilyDefinition();
        familyDefinition.setFamilyCode("legacy-dp-crack");
        familyDefinition.setProtocolCode("mqtt-json");
        properties.getProtocol().getFamilyDefinitions().put("legacy-dp-crack", familyDefinition);

        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("PROTOCOL");
        dto.setProtocolCode("family:legacy-dp-crack");
        dto.setRawIdentifier("disp");
        dto.setTargetNormativeIdentifier("value");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper, snapshotMapper, properties);

        service.createRule(1001L, 10001L, dto);

        verify(mapper).insert(argThat((VendorMetricMappingRule rule) ->
                "PROTOCOL".equals(rule.getScopeType())
                        && "family:legacy-dp-crack".equals(rule.getProtocolCode())
                        && "disp".equals(rule.getRawIdentifier())
        ));
    }

    @Test
    void createRuleShouldRejectUnknownProtocolFamilySelector() {
        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("PROTOCOL");
        dto.setProtocolCode("family:missing-family");
        dto.setRawIdentifier("disp");
        dto.setTargetNormativeIdentifier("value");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper, snapshotMapper, new IotProperties());

        BizException error = assertThrows(BizException.class, () -> service.createRule(1001L, 10001L, dto));

        assertEquals("protocolCode 对应的协议族不存在或未启用: family:missing-family", error.getMessage());
        verify(mapper, never()).insert(any(VendorMetricMappingRule.class));
    }
}
