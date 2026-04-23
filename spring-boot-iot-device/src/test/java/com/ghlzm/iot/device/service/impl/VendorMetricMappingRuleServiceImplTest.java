package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleBatchStatusDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleReplayDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleReplayVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper mapper;
    @Mock
    private VendorMetricMappingRuleSnapshotMapper snapshotMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private VendorMetricMappingRuntimeServiceImpl runtimeService;

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

    @Test
    void batchStatusShouldOnlyUpdateMatchedRulesWithinProduct() {
        VendorMetricMappingRule first = new VendorMetricMappingRule();
        first.setId(7101L);
        first.setProductId(1001L);
        first.setStatus("ACTIVE");
        first.setVersionNo(3);
        first.setDeleted(0);

        VendorMetricMappingRule second = new VendorMetricMappingRule();
        second.setId(7102L);
        second.setProductId(1001L);
        second.setStatus("DRAFT");
        second.setVersionNo(2);
        second.setDeleted(0);

        VendorMetricMappingRule ignored = new VendorMetricMappingRule();
        ignored.setId(7199L);
        ignored.setProductId(2002L);
        ignored.setStatus("ACTIVE");
        ignored.setVersionNo(9);
        ignored.setDeleted(0);

        when(mapper.selectBatchIds(List.of(7101L, 7102L, 7199L))).thenReturn(List.of(first, second, ignored));
        when(mapper.updateById(any(VendorMetricMappingRule.class))).thenReturn(1);

        VendorMetricMappingRuleBatchStatusDTO dto = new VendorMetricMappingRuleBatchStatusDTO();
        dto.setRuleIds(List.of(7101L, 7102L, 7199L));
        dto.setTargetStatus("disabled");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(
                mapper,
                snapshotMapper,
                (ProtocolSecurityDefinitionProvider) null,
                productMapper,
                runtimeService
        );

        Map<String, Object> result = service.batchStatus(1001L, 10001L, dto);

        assertEquals(3, result.get("requestedCount"));
        assertEquals(2, result.get("matchedCount"));
        assertEquals(2, result.get("changedCount"));
        assertEquals("DISABLED", result.get("targetStatus"));
        verify(mapper).updateById(argThat((VendorMetricMappingRule row) ->
                Long.valueOf(7101L).equals(row.getId())
                        && "DISABLED".equals(row.getStatus())
                        && Integer.valueOf(4).equals(row.getVersionNo())
        ));
        verify(mapper).updateById(argThat((VendorMetricMappingRule row) ->
                Long.valueOf(7102L).equals(row.getId())
                        && "DISABLED".equals(row.getStatus())
                        && Integer.valueOf(3).equals(row.getVersionNo())
        ));
    }

    @Test
    void batchStatusShouldNotCountFailedUpdatesAsChanged() {
        VendorMetricMappingRule first = new VendorMetricMappingRule();
        first.setId(7101L);
        first.setProductId(1001L);
        first.setStatus("DRAFT");
        first.setVersionNo(3);
        first.setDeleted(0);

        VendorMetricMappingRule second = new VendorMetricMappingRule();
        second.setId(7102L);
        second.setProductId(1001L);
        second.setStatus("DRAFT");
        second.setVersionNo(2);
        second.setDeleted(0);

        when(mapper.selectBatchIds(List.of(7101L, 7102L))).thenReturn(List.of(first, second));
        when(mapper.updateById(any(VendorMetricMappingRule.class))).thenAnswer((invocation) -> {
            VendorMetricMappingRule row = invocation.getArgument(0);
            return Long.valueOf(7101L).equals(row.getId()) ? 1 : 0;
        });

        VendorMetricMappingRuleBatchStatusDTO dto = new VendorMetricMappingRuleBatchStatusDTO();
        dto.setRuleIds(List.of(7101L, 7102L));
        dto.setTargetStatus("ACTIVE");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(
                mapper,
                snapshotMapper,
                (ProtocolSecurityDefinitionProvider) null,
                productMapper,
                runtimeService
        );

        Map<String, Object> result = service.batchStatus(1001L, 10001L, dto);

        assertEquals(2, result.get("requestedCount"));
        assertEquals(2, result.get("matchedCount"));
        assertEquals(1, result.get("changedCount"));
        assertEquals("ACTIVE", result.get("targetStatus"));
    }

    @Test
    void batchStatusShouldRejectUnsupportedTargetStatus() {
        VendorMetricMappingRuleBatchStatusDTO dto = new VendorMetricMappingRuleBatchStatusDTO();
        dto.setRuleIds(List.of(7101L));
        dto.setTargetStatus("ARCHIVED");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(
                mapper,
                snapshotMapper,
                (ProtocolSecurityDefinitionProvider) null,
                productMapper,
                runtimeService
        );

        BizException error = assertThrows(BizException.class, () -> service.batchStatus(1001L, 10001L, dto));
        assertTrue(error.getMessage().contains("targetStatus"));
        verify(mapper, never()).updateById(any(VendorMetricMappingRule.class));
    }

    @Test
    void replayShouldReuseRuntimeResolutionAndEchoSampleValue() {
        Product product = new Product();
        product.setId(1001L);
        product.setProtocolCode("mqtt-json");
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(runtimeService.replayForGovernance(eq(product), eq("disp"), eq("L1_LF_1")))
                .thenReturn(new VendorMetricMappingRuntimeServiceImpl.ReplayResolution(
                        true,
                        "PUBLISHED_SNAPSHOT",
                        "PRODUCT",
                        "disp",
                        "L1_LF_1",
                        "value",
                        7101L
                ));

        VendorMetricMappingRuleReplayDTO dto = new VendorMetricMappingRuleReplayDTO();
        dto.setRawIdentifier("disp");
        dto.setLogicalChannelCode("L1_LF_1");
        dto.setSampleValue("0.2136");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(
                mapper,
                snapshotMapper,
                (ProtocolSecurityDefinitionProvider) null,
                productMapper,
                runtimeService
        );

        VendorMetricMappingRuleReplayVO result = service.replay(1001L, dto);

        assertEquals(Boolean.TRUE, result.getMatched());
        assertEquals("PUBLISHED_SNAPSHOT", result.getHitSource());
        assertEquals("PRODUCT", result.getMatchedScopeType());
        assertEquals("value", result.getCanonicalIdentifier());
        assertEquals("value", result.getTargetNormativeIdentifier());
        assertEquals("0.2136", result.getSampleValue());
    }
}
