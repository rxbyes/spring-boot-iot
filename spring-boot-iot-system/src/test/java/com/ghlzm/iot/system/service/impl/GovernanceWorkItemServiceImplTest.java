package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.GovernanceReplayFeedback;
import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceReplayFeedbackMapper;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.service.GovernancePriorityScorer;
import com.ghlzm.iot.system.service.GovernanceWorkItemContributor;
import com.ghlzm.iot.system.service.model.GovernanceReplayFeedbackCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceDecisionContextVO;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceWorkItemServiceImplTest {

    @Mock
    private GovernanceWorkItemMapper workItemMapper;

    @Mock
    private GovernanceWorkItemContributor contributor;

    @Mock
    private GovernanceReplayFeedbackMapper replayFeedbackMapper;

    @Test
    void springContextShouldInstantiateServiceWhenContributorBeanExists() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(GovernanceWorkItemMapper.class, () -> workItemMapper);
            context.registerBean(GovernanceReplayFeedbackMapper.class, () -> replayFeedbackMapper);
            context.registerBean(GovernanceWorkItemContributor.class, () -> contributor);
            context.registerBean(GovernancePriorityScorer.class, GovernancePriorityScorerImpl::new);
            context.registerBean("applicationTaskExecutor", Executor.class, () -> Runnable::run);
            context.register(GovernanceWorkItemServiceImpl.class);

            context.refresh();

            GovernanceWorkItemServiceImpl service = context.getBean(GovernanceWorkItemServiceImpl.class);
            assertNotNull(service);
        }
    }

    @Test
    void openOrRefreshShouldCreateOpenItemForSubject() {
        when(workItemMapper.selectOne(any())).thenReturn(null);
        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_CONTRACT_RELEASE",
                "PRODUCT",
                1001L,
                1001L,
                null,
                null,
                null,
                null,
                null,
                "phase1-crack",
                null,
                "MODEL_GOVERNANCE",
                "合同尚未发布",
                "{\"publishedRiskMetricCount\":0}",
                "P1",
                10001L
        ));

        verify(workItemMapper).insert(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                "PENDING_CONTRACT_RELEASE".equals(item.getWorkItemCode())
                        && "PRODUCT".equals(item.getSubjectType())
                        && Long.valueOf(1001L).equals(item.getSubjectId())
                        && "phase1-crack".equals(item.getProductKey())
                        && "OPEN".equals(item.getWorkStatus())
                        && "P1".equals(item.getPriorityLevel())
                        && Long.valueOf(10001L).equals(item.getCreateBy())
        ));
    }

    @Test
    void openOrRefreshShouldPreserveAckedStatusAndCommentOnSyncRefresh() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9001L);
        existing.setWorkStatus("ACKED");
        writeField(existing, "executionStatus", "IN_PROGRESS");
        existing.setAssigneeUserId(20001L);
        existing.setBlockingReason("已人工确认");
        when(workItemMapper.selectOne(any())).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_THRESHOLD_POLICY",
                "RISK_POINT_DEVICE",
                5101L,
                1001L,
                9102L,
                7001L,
                null,
                null,
                null,
                null,
                null,
                "RULE_DEFINITION",
                "待补阈值策略",
                "{\"riskPointDeviceId\":5101}",
                "P1",
                10001L
        ));

        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9001L).equals(item.getId())
                        && "ACKED".equals(item.getWorkStatus())
                        && "IN_PROGRESS".equals(readString(item, "executionStatus"))
                        && Long.valueOf(20001L).equals(item.getAssigneeUserId())
                        && "已人工确认".equals(item.getBlockingReason())
                        && Long.valueOf(9102L).equals(item.getRiskMetricId())
        ));
    }

    @Test
    void pageWorkItemsShouldSyncContributorCommandsAndResolveStaleRows() {
        GovernanceWorkItem stale = new GovernanceWorkItem();
        stale.setId(9101L);
        stale.setWorkItemCode("PENDING_PRODUCT_GOVERNANCE");
        stale.setSubjectType("PRODUCT");
        stale.setSubjectId(1002L);
        stale.setWorkStatus("OPEN");
        when(contributor.collectWorkItems()).thenReturn(List.of(
                new GovernanceWorkItemCommand(
                        "PENDING_PRODUCT_GOVERNANCE",
                        "PRODUCT",
                        1001L,
                        1001L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "phase1-crack",
                        null,
                        "MODEL_GOVERNANCE",
                        "待治理产品",
                        "{\"productId\":1001}",
                        "P2",
                        1L
                )
        ));
        when(workItemMapper.selectList(any())).thenReturn(List.of(stale));
        when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<GovernanceWorkItem> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of(contributor));

        service.pageWorkItems(new GovernanceWorkItemPageQuery(), 10001L);

        verify(contributor).collectWorkItems();
        verify(workItemMapper).insert(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                "PENDING_PRODUCT_GOVERNANCE".equals(item.getWorkItemCode())
                        && Long.valueOf(1001L).equals(item.getSubjectId())
                        && "phase1-crack".equals(item.getProductKey())
        ));
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9101L).equals(item.getId())
                        && "RESOLVED".equals(item.getWorkStatus())
                        && "RESOLVED".equals(readString(item, "executionStatus"))
        ));
    }

    @Test
    void pageWorkItemsShouldNotPerformPerCommandSelectOneDuringContributorSync() {
        when(contributor.collectWorkItems()).thenReturn(List.of(
                new GovernanceWorkItemCommand(
                        "PENDING_PRODUCT_GOVERNANCE",
                        "PRODUCT",
                        1001L,
                        1001L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "phase1-crack",
                        null,
                        "MODEL_GOVERNANCE",
                        "待治理产品",
                        "{\"productId\":1001}",
                        "P2",
                        1L
                ),
                new GovernanceWorkItemCommand(
                        "PENDING_CONTRACT_RELEASE",
                        "PRODUCT",
                        1001L,
                        1001L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "phase1-crack",
                        null,
                        "CONTRACT_RELEASE",
                        "待发布合同",
                        "{\"productId\":1001}",
                        "P1",
                        1L
                )
        ));
        when(workItemMapper.selectList(any())).thenReturn(List.of());
        when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<GovernanceWorkItem> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of(contributor));

        service.pageWorkItems(new GovernanceWorkItemPageQuery(), 10001L);

        verify(workItemMapper, never()).selectOne(any());
    }

    @Test
    void pageWorkItemsShouldScheduleContributorRefreshWithoutBlockingPageQuery() {
        AtomicReference<Runnable> scheduledRefresh = new AtomicReference<>();
        when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<GovernanceWorkItem> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(
                workItemMapper,
                List.of(contributor),
                scheduledRefresh::set,
                () -> 1_000L,
                300_000L
        );

        service.pageWorkItems(new GovernanceWorkItemPageQuery(), 10001L);

        verify(workItemMapper).selectPage(any(), any());
        verify(contributor, never()).collectWorkItems();
        assertNotNull(scheduledRefresh.get());
    }

    @Test
    void ackShouldKeepGeneratedReasonWhenCommentMissing() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9201L);
        existing.setBlockingReason("待补联动预案");
        when(workItemMapper.selectById(9201L)).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.ack(9201L, 10001L, null);

        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9201L).equals(item.getId())
                        && "ACKED".equals(item.getWorkStatus())
                        && "IN_PROGRESS".equals(readString(item, "executionStatus"))
                        && "待补联动预案".equals(item.getBlockingReason())
                        && Long.valueOf(10001L).equals(item.getAssigneeUserId())
        ));
    }

    @Test
    void blockShouldUpdateExecutionStatusToReplayRequired() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9202L);
        when(workItemMapper.selectById(9202L)).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.block(9202L, 10001L, "waiting replay");

        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9202L).equals(item.getId())
                        && "BLOCKED".equals(item.getWorkStatus())
                        && "REPLAY_REQUIRED".equals(readString(item, "executionStatus"))
                        && "waiting replay".equals(item.getBlockingReason())
                        && Long.valueOf(10001L).equals(item.getAssigneeUserId())
        ));
    }

    @Test
    void resolveShouldUpdateExecutionStatusToResolved() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9203L);
        when(workItemMapper.selectOne(any())).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.resolve("PENDING_LINKAGE_PLAN", "RISK_METRIC", 501L, 10001L, "done");

        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9203L).equals(item.getId())
                        && "RESOLVED".equals(item.getWorkStatus())
                        && "RESOLVED".equals(readString(item, "executionStatus"))
                        && "done".equals(item.getBlockingReason())
                        && item.getResolvedTime() != null
        ));
    }

    @Test
    void closeShouldUpdateExecutionStatusToClosed() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9204L);
        when(workItemMapper.selectById(9204L)).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.close(9204L, 10001L, "closed");

        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9204L).equals(item.getId())
                        && "CLOSED".equals(item.getWorkStatus())
                        && "CLOSED".equals(readString(item, "executionStatus"))
                        && "closed".equals(item.getBlockingReason())
                        && item.getClosedTime() != null
        ));
    }

    @Test
    void openOrRefreshShouldKeepExecutionStatusWhenExistingItemNotReopened() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9205L);
        existing.setWorkStatus("ACKED");
        writeField(existing, "executionStatus", "IN_PROGRESS");
        when(workItemMapper.selectOne(any())).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_THRESHOLD_POLICY",
                "RISK_POINT_DEVICE",
                5201L,
                1001L,
                9102L,
                7001L,
                null,
                null,
                null,
                null,
                null,
                "RULE_DEFINITION",
                "pending policy",
                "{\"riskPointDeviceId\":5201}",
                "P1",
                10001L
        ));

        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9205L).equals(item.getId())
                        && "ACKED".equals(item.getWorkStatus())
                        && "IN_PROGRESS".equals(readString(item, "executionStatus"))
        ));
    }

    @Test
    void openOrRefreshShouldPersistReplayContextFields() {
        when(workItemMapper.selectOne(any())).thenReturn(null);
        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_REPLAY",
                "REPLAY_CASE",
                5101L,
                1001L,
                9102L,
                7001L,
                null,
                "trace-001",
                "device-001",
                "phase2-gnss",
                null,
                "REPLAY",
                "待运营复盘",
                "{\"metricIdentifier\":\"gpsTotalX\"}",
                "P2",
                10001L
        ));

        verify(workItemMapper).insert(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                "PENDING_REPLAY".equals(item.getWorkItemCode())
                        && "trace-001".equals(item.getTraceId())
                        && "device-001".equals(item.getDeviceCode())
                        && "phase2-gnss".equals(item.getProductKey())
                        && Long.valueOf(7001L).equals(item.getReleaseBatchId())
        ));
    }

    @Test
    void openOrRefreshShouldBackfillLifecycleHubFieldsForPendingRiskBinding() {
        when(workItemMapper.selectOne(any())).thenReturn(null);
        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_RISK_BINDING",
                "RISK_POINT_DEVICE",
                5102L,
                1001L,
                9102L,
                7001L,
                null,
                null,
                "device-001",
                "phase1-crack",
                null,
                "RISK_GOVERNANCE",
                "待处理风险点治理任务",
                "{\"confidence\":0.97,\"evidenceItems\":[{\"source\":\"catalog\"}],\"affectedRiskPointCount\":3,\"rollbackable\":true}",
                "P1",
                10001L
        ));

        verify(workItemMapper).insert(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                "RISK_BINDING".equals(readString(item, "taskCategory"))
                        && "ALARM".equals(readString(item, "domainCode"))
                        && "RISK_POINT_PENDING_PROMOTION".equals(readString(item, "actionCode"))
                        && "PENDING_APPROVAL".equals(readString(item, "executionStatus"))
                        && readString(item, "recommendationSnapshotJson").contains("confidence")
                        && readString(item, "evidenceSnapshotJson").contains("evidenceItems")
                        && readString(item, "impactSnapshotJson").contains("affectedRiskPointCount")
                        && readString(item, "rollbackSnapshotJson").contains("rollbackable")
        ));
    }

    @Test
    void pageWorkItemsShouldReturnLifecycleHubFields() {
        GovernanceWorkItem row = new GovernanceWorkItem();
        row.setId(9301L);
        row.setWorkItemCode("PENDING_RISK_BINDING");
        writeField(row, "taskCategory", "RISK_BINDING");
        writeField(row, "domainCode", "ALARM");
        writeField(row, "actionCode", "RISK_POINT_PENDING_PROMOTION");
        writeField(row, "executionStatus", "PENDING_APPROVAL");
        writeField(row, "recommendationSnapshotJson", "{\"confidence\":0.97}");
        writeField(row, "evidenceSnapshotJson", "{\"evidenceItems\":[{\"source\":\"catalog\"}]}");
        writeField(row, "impactSnapshotJson", "{\"affectedRiskPointCount\":3}");
        writeField(row, "rollbackSnapshotJson", "{\"rollbackable\":true}");

        when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<GovernanceWorkItem> page = invocation.getArgument(0);
            page.setRecords(List.of(row));
            page.setTotal(1L);
            return page;
        });

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        PageResult<GovernanceWorkItemVO> result = service.pageWorkItems(new GovernanceWorkItemPageQuery(), 10001L);

        GovernanceWorkItemVO record = result.getRecords().get(0);
        assertEquals("RISK_BINDING", readString(record, "taskCategory"));
        assertEquals("ALARM", readString(record, "domainCode"));
        assertEquals("RISK_POINT_PENDING_PROMOTION", readString(record, "actionCode"));
        assertEquals("PENDING_APPROVAL", readString(record, "executionStatus"));
        assertTrue(readString(record, "recommendationSnapshotJson").contains("confidence"));
        assertTrue(readString(record, "evidenceSnapshotJson").contains("evidenceItems"));
        assertTrue(readString(record, "impactSnapshotJson").contains("affectedRiskPointCount"));
        assertTrue(readString(record, "rollbackSnapshotJson").contains("rollbackable"));
    }

    @Test
    void pageWorkItemsShouldReturnUnifiedRecommendationContract() {
        GovernanceWorkItem row = new GovernanceWorkItem();
        row.setId(9302L);
        row.setWorkItemCode("PENDING_RISK_BINDING");
        writeField(row, "recommendationSnapshotJson",
                "{\"recommendationType\":\"PROMOTE\",\"confidence\":0.92,\"reasonCodes\":[\"LOW_BINDING_COVERAGE\"],\"suggestedAction\":\"Promote pending binding\"}");
        writeField(row, "evidenceSnapshotJson",
                "{\"evidenceItems\":[{\"evidenceType\":\"RUNTIME_PAYLOAD\",\"title\":\"Latest payload\",\"summary\":\"gpsTotalX still pending\"}]}");
        writeField(row, "impactSnapshotJson",
                "{\"affectedCount\":3,\"affectedTypes\":[\"RISK_POINT\",\"DEVICE\"],\"rollbackable\":true,\"rollbackPlanSummary\":\"Can revert pending promotion\"}");
        writeField(row, "rollbackSnapshotJson",
                "{\"rollbackable\":true,\"rollbackPlanSummary\":\"Can revert pending promotion\"}");

        when(workItemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<GovernanceWorkItem> page = invocation.getArgument(0);
            page.setRecords(List.of(row));
            page.setTotal(1L);
            return page;
        });

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        PageResult<GovernanceWorkItemVO> result = service.pageWorkItems(new GovernanceWorkItemPageQuery(), 10001L);

        GovernanceWorkItemVO record = result.getRecords().get(0);
        Object recommendation = readFieldValue(record, "recommendation");
        Object impact = readFieldValue(record, "impact");
        List<?> evidenceItems = readListField(recommendation, "evidenceItems");

        assertEquals("0.92", String.valueOf(readFieldValue(recommendation, "confidence")));
        assertEquals("RUNTIME_PAYLOAD", String.valueOf(readFieldValue(evidenceItems.get(0), "evidenceType")));
        assertEquals("true", String.valueOf(readFieldValue(impact, "rollbackable")));
    }

    @Test
    void openOrRefreshShouldApplyDeterministicPriorityAndPersistReasonCodes() {
        when(workItemMapper.selectOne(any())).thenReturn(null);
        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        service.openOrRefresh(new GovernanceWorkItemCommand(
                "PENDING_CONTRACT_RELEASE",
                "PRODUCT",
                1001L,
                1001L,
                9102L,
                7001L,
                null,
                null,
                null,
                "phase2-gnss",
                null,
                "CONTRACT_RELEASE",
                "待发布合同影响多个下游模块",
                "{\"missingBindingCount\":2,\"missingPolicyCount\":1}",
                "CONTRACT_RELEASE",
                "DEVICE",
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "PENDING_APPROVAL",
                null,
                null,
                "{\"affectedCount\":5,\"affectedTypes\":[\"PRODUCT\",\"RISK_POINT\",\"RULE\"],\"rollbackable\":true,\"rollbackPlanSummary\":\"Can rollback contract release\"}",
                "{\"rollbackable\":true,\"rollbackPlanSummary\":\"Can rollback contract release\"}",
                null,
                10001L
        ));

        verify(workItemMapper).insert(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                "P1".equals(item.getPriorityLevel())
                        && readString(item, "recommendationSnapshotJson").contains("LOW_BINDING_COVERAGE")
                        && readString(item, "recommendationSnapshotJson").contains("HIGH_IMPACT_RELEASE")
        ));
    }

    @Test
    void getDecisionContextShouldExplainPriorityReasonsAndAffectedModules() {
        GovernanceWorkItem row = new GovernanceWorkItem();
        row.setId(9401L);
        row.setWorkItemCode("PENDING_CONTRACT_RELEASE");
        row.setPriorityLevel("P1");
        row.setProductId(1001L);
        row.setRiskMetricId(9102L);
        row.setBlockingReason("待发布合同影响多个下游模块");
        writeField(row, "recommendationSnapshotJson",
                "{\"recommendationType\":\"PUBLISH\",\"reasonCodes\":[\"LOW_BINDING_COVERAGE\",\"HIGH_IMPACT_RELEASE\"],\"suggestedAction\":\"Publish contract release\"}");
        writeField(row, "impactSnapshotJson",
                "{\"affectedCount\":5,\"affectedTypes\":[\"PRODUCT\",\"RISK_POINT\",\"RULE\"],\"rollbackable\":true,\"rollbackPlanSummary\":\"Can rollback contract release\"}");
        when(workItemMapper.selectById(9401L)).thenReturn(row);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());

        GovernanceDecisionContextVO context = service.getDecisionContext(9401L, 10001L);

        assertEquals("P1", context.getPriorityLevel());
        assertTrue(context.getReasonCodes().contains("LOW_BINDING_COVERAGE"));
        assertTrue(context.getReasonCodes().contains("HIGH_IMPACT_RELEASE"));
        assertTrue(context.getAffectedModules().contains("PRODUCT"));
        assertTrue(context.getAffectedModules().contains("RISK_POINT"));
        assertTrue(context.getAffectedModules().contains("RULE"));
        assertEquals("Publish contract release", context.getRecommendedAction());
    }

    @Test
    void closeReplayWithFeedbackShouldPersistStructuredReplayFeedbackAndCloseWorkItem() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9501L);
        existing.setTenantId(1L);
        existing.setWorkItemCode("PENDING_REPLAY");
        existing.setApprovalOrderId(8101L);
        existing.setReleaseBatchId(7001L);
        existing.setTraceId("trace-001");
        existing.setDeviceCode("device-001");
        existing.setProductKey("phase2-gnss");
        when(workItemMapper.selectById(9501L)).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());
        writeFieldValue(service, "replayFeedbackMapper", replayFeedbackMapper);

        service.closeReplayWithFeedback(new GovernanceReplayFeedbackCommand(
                9501L,
                8101L,
                7001L,
                "trace-001",
                "device-001",
                "phase2-gnss",
                "PROMOTE",
                "PROMOTE",
                "SUCCESS",
                "MISSING_POLICY",
                "复盘确认缺少阈值策略"
        ), 10001L);

        verify(replayFeedbackMapper).insert(org.mockito.ArgumentMatchers.<GovernanceReplayFeedback>argThat(feedback ->
                Long.valueOf(9501L).equals(feedback.getWorkItemId())
                        && Long.valueOf(8101L).equals(feedback.getApprovalOrderId())
                        && Long.valueOf(7001L).equals(feedback.getReleaseBatchId())
                        && "PROMOTE".equals(feedback.getAdoptedDecision())
                        && "SUCCESS".equals(feedback.getExecutionOutcome())
                        && "MISSING_POLICY".equals(feedback.getRootCauseCode())
                        && feedback.getFeedbackJson() != null
                        && feedback.getFeedbackJson().contains("recommendedDecision")
                        && feedback.getFeedbackJson().contains("operatorSummary")
        ));
        verify(workItemMapper).updateById(org.mockito.ArgumentMatchers.<GovernanceWorkItem>argThat(item ->
                Long.valueOf(9501L).equals(item.getId())
                        && "CLOSED".equals(item.getWorkStatus())
                        && "CLOSED".equals(readString(item, "executionStatus"))
                        && "复盘确认缺少阈值策略".equals(item.getBlockingReason())
                        && item.getClosedTime() != null
        ));
    }

    @Test
    void closeReplayWithFeedbackShouldResolveReplayWorkItemFromReplayContext() {
        GovernanceWorkItem existing = new GovernanceWorkItem();
        existing.setId(9502L);
        existing.setTenantId(1L);
        existing.setWorkItemCode("PENDING_REPLAY");
        existing.setReleaseBatchId(7002L);
        existing.setTraceId("trace-002");
        existing.setDeviceCode("device-002");
        existing.setProductKey("phase1-crack");
        when(workItemMapper.selectOne(any())).thenReturn(existing);

        GovernanceWorkItemServiceImpl service = new GovernanceWorkItemServiceImpl(workItemMapper, List.of());
        writeFieldValue(service, "replayFeedbackMapper", replayFeedbackMapper);

        service.closeReplayWithFeedback(new GovernanceReplayFeedbackCommand(
                null,
                null,
                7002L,
                "trace-002",
                "device-002",
                "phase1-crack",
                "IGNORE",
                "CREATE_POLICY",
                "SUCCESS",
                "MISSING_POLICY",
                "运维台复盘确认需要补齐阈值策略"
        ), 10001L);

        verify(replayFeedbackMapper).insert(org.mockito.ArgumentMatchers.<GovernanceReplayFeedback>argThat(feedback ->
                Long.valueOf(9502L).equals(feedback.getWorkItemId())
                        && Long.valueOf(7002L).equals(feedback.getReleaseBatchId())
                        && "CREATE_POLICY".equals(feedback.getAdoptedDecision())
                        && feedback.getFeedbackJson() != null
                        && feedback.getFeedbackJson().contains("overrideRecommendation")
        ));
    }

    private static void writeField(Object target, String fieldName, String value) {
        writeFieldValue(target, fieldName, value);
    }

    private static void writeFieldValue(Object target, String fieldName, Object value) {
        if (target == null) {
            return;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // compatibility for pre-hub classes
        }
    }

    private static String readString(Object target, String fieldName) {
        if (target == null) {
            return "";
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value == null ? "" : String.valueOf(value);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return "";
        }
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
