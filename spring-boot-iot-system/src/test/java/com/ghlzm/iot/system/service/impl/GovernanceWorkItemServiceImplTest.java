package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.GovernanceWorkItem;
import com.ghlzm.iot.system.mapper.GovernanceWorkItemMapper;
import com.ghlzm.iot.system.service.GovernanceWorkItemContributor;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Field;
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

    @Test
    void springContextShouldInstantiateServiceWhenContributorBeanExists() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(GovernanceWorkItemMapper.class, () -> workItemMapper);
            context.registerBean(GovernanceWorkItemContributor.class, () -> contributor);
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
                        && "待补联动预案".equals(item.getBlockingReason())
                        && Long.valueOf(10001L).equals(item.getAssigneeUserId())
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

    private static void writeField(Object target, String fieldName, String value) {
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
}
