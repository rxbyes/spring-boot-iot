package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.GovernanceOpsAlert;
import com.ghlzm.iot.system.mapper.GovernanceOpsAlertMapper;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertCommand;
import com.ghlzm.iot.system.service.model.GovernanceOpsAlertPageQuery;
import com.ghlzm.iot.system.vo.GovernanceOpsAlertVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceOpsAlertServiceImplTest {

    @Mock
    private GovernanceOpsAlertMapper alertMapper;

    @Test
    void raiseOrRefreshShouldReuseExistingAlertCodeAndRefreshLastSeenTime() {
        GovernanceOpsAlert existing = new GovernanceOpsAlert();
        existing.setId(9001L);
        existing.setAlertType("FIELD_DRIFT");
        existing.setAlertCode("product:1001:value");
        existing.setAlertStatus("OPEN");
        when(alertMapper.selectOne(any())).thenReturn(existing);

        GovernanceOpsAlertServiceImpl service = new GovernanceOpsAlertServiceImpl(alertMapper);
        service.raiseOrRefresh(new GovernanceOpsAlertCommand(
                "FIELD_DRIFT",
                "product:1001:value",
                "PRODUCT",
                1001L,
                1001L,
                null,
                null,
                null,
                null,
                null,
                "WARN",
                3L,
                "字段漂移告警",
                "value 已偏离正式合同",
                "product:1001:value",
                "产品1001/value",
                "PAYLOAD_APPLY",
                "{}",
                10001L
        ));

        verify(alertMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceOpsAlert>argThat(alert ->
                Long.valueOf(9001L).equals(alert.getId())
                        && Long.valueOf(3L).equals(alert.getAffectedCount())
                        && "OPEN".equals(alert.getAlertStatus())
                        && "WARN".equals(alert.getSeverityLevel())
        ));
    }

    @Test
    void raiseOrRefreshShouldPreserveSuppressedStatusWhenAlertStillExists() {
        GovernanceOpsAlert existing = new GovernanceOpsAlert();
        existing.setId(9002L);
        existing.setAlertType("CONTRACT_DIFF");
        existing.setAlertCode("product:1001:contract-diff");
        existing.setAlertStatus("SUPPRESSED");
        existing.setAssigneeUserId(20001L);
        existing.setAlertMessage("已人工抑制");
        when(alertMapper.selectOne(any())).thenReturn(existing);

        GovernanceOpsAlertServiceImpl service = new GovernanceOpsAlertServiceImpl(alertMapper);
        service.raiseOrRefresh(new GovernanceOpsAlertCommand(
                "CONTRACT_DIFF",
                "product:1001:contract-diff",
                "PRODUCT",
                1001L,
                1001L,
                null,
                7001L,
                "trace-1",
                "device-1",
                "product-key-1",
                "WARN",
                2L,
                "合同差异告警",
                "正式合同与当前字段不一致",
                "product:1001",
                "产品1001",
                "BATCH_RECONCILIATION",
                "{}",
                10001L
        ));

        verify(alertMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceOpsAlert>argThat(alert ->
                Long.valueOf(9002L).equals(alert.getId())
                        && "SUPPRESSED".equals(alert.getAlertStatus())
                        && Long.valueOf(20001L).equals(alert.getAssigneeUserId())
                        && "已人工抑制".equals(alert.getAlertMessage())
                        && Long.valueOf(7001L).equals(alert.getReleaseBatchId())
        ));
    }

    @Test
    void closeShouldKeepGeneratedAlertMessageWhenCommentMissing() {
        GovernanceOpsAlert existing = new GovernanceOpsAlert();
        existing.setId(9901L);
        existing.setAlertMessage("value 已偏离正式合同");
        when(alertMapper.selectById(9901L)).thenReturn(existing);

        GovernanceOpsAlertServiceImpl service = new GovernanceOpsAlertServiceImpl(alertMapper);

        service.close(9901L, 10001L, null);

        verify(alertMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceOpsAlert>argThat(alert ->
                Long.valueOf(9901L).equals(alert.getId())
                        && "CLOSED".equals(alert.getAlertStatus())
                        && "value 已偏离正式合同".equals(alert.getAlertMessage())
        ));
    }

    @Test
    void pageAlertsShouldReturnUnifiedRecommendationContract() {
        GovernanceOpsAlert row = new GovernanceOpsAlert();
        row.setId(9902L);
        row.setAlertType("FIELD_DRIFT");
        row.setAffectedCount(3L);
        row.setSnapshotJson("{\"recommendation\":{\"recommendationType\":\"PUBLISH\",\"confidence\":0.92,\"reasonCodes\":[\"FIELD_DRIFT\"],\"suggestedAction\":\"Publish contract update\",\"evidenceItems\":[{\"evidenceType\":\"RUNTIME_PAYLOAD\",\"title\":\"Latest payload\",\"summary\":\"value drift detected\"}]},\"impact\":{\"affectedCount\":3,\"affectedTypes\":[\"PRODUCT\",\"RISK_POINT\"],\"rollbackable\":true,\"rollbackPlanSummary\":\"Can re-publish previous contract\"},\"rollback\":{\"rollbackable\":true,\"rollbackPlanSummary\":\"Can re-publish previous contract\"}}");

        when(alertMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<GovernanceOpsAlert> page = invocation.getArgument(0);
            page.setRecords(List.of(row));
            page.setTotal(1L);
            return page;
        });

        GovernanceOpsAlertServiceImpl service = new GovernanceOpsAlertServiceImpl(alertMapper);

        PageResult<GovernanceOpsAlertVO> result = service.pageAlerts(new GovernanceOpsAlertPageQuery(), 10001L);

        GovernanceOpsAlertVO record = result.getRecords().get(0);
        Object recommendation = readFieldValue(record, "recommendation");
        Object impact = readFieldValue(record, "impact");
        List<?> evidenceItems = readListField(recommendation, "evidenceItems");

        assertEquals("0.92", String.valueOf(readFieldValue(recommendation, "confidence")));
        assertEquals("RUNTIME_PAYLOAD", String.valueOf(readFieldValue(evidenceItems.get(0), "evidenceType")));
        assertEquals("true", String.valueOf(readFieldValue(impact, "rollbackable")));
    }

    private static Object readFieldValue(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<?> readListField(Object target, String fieldName) {
        Object value = readFieldValue(target, fieldName);
        return value instanceof List<?> list ? list : Collections.emptyList();
    }
}
