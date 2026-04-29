package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceWorkItemTransitionDTO;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemPageQuery;
import com.ghlzm.iot.system.vo.GovernanceDecisionContextVO;
import com.ghlzm.iot.system.vo.GovernanceWorkItemVO;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceWorkItemControllerTest {

    @Mock
    private GovernanceWorkItemService governanceWorkItemService;
    @Mock
    private GovernancePermissionGuard permissionGuard;

    private GovernanceWorkItemController controller;

    @BeforeEach
    void setUp() {
        controller = new GovernanceWorkItemController(governanceWorkItemService, permissionGuard);
    }

    @Test
    void pageWorkItemsShouldDelegateToService() {
        GovernanceWorkItemPageQuery query = new GovernanceWorkItemPageQuery();
        query.setWorkStatus("OPEN");
        query.setWorkItemCode("PENDING_CONTRACT_RELEASE");
        query.setPageNum(1L);
        query.setPageSize(20L);

        GovernanceWorkItemVO row = new GovernanceWorkItemVO();
        row.setId(1L);
        row.setWorkItemCode("PENDING_CONTRACT_RELEASE");
        row.setWorkStatus("OPEN");
        writeField(row, "taskCategory", "RISK_BINDING");
        writeField(row, "domainCode", "ALARM");
        writeField(row, "actionCode", "RISK_POINT_PENDING_PROMOTION");
        writeField(row, "executionStatus", "PENDING_APPROVAL");
        writeField(row, "recommendationSnapshotJson", "{\"confidence\":0.97}");
        writeField(row, "evidenceSnapshotJson", "{\"evidenceItems\":[{\"source\":\"catalog\"}]}");
        writeField(row, "impactSnapshotJson", "{\"affectedRiskPointCount\":3}");
        writeField(row, "rollbackSnapshotJson", "{\"rollbackable\":true}");
        when(governanceWorkItemService.pageWorkItems(query, 10001L))
                .thenReturn(PageResult.of(1L, 1L, 20L, List.of(row)));

        R<PageResult<GovernanceWorkItemVO>> response = controller.pageWorkItems(query, authentication(10001L));

        assertEquals(1L, response.getData().getTotal());
        assertEquals("PENDING_CONTRACT_RELEASE", response.getData().getRecords().get(0).getWorkItemCode());
        assertEquals("RISK_BINDING", readField(response.getData().getRecords().get(0), "taskCategory"));
        assertEquals("ALARM", readField(response.getData().getRecords().get(0), "domainCode"));
        assertEquals("RISK_POINT_PENDING_PROMOTION", readField(response.getData().getRecords().get(0), "actionCode"));
        assertEquals("PENDING_APPROVAL", readField(response.getData().getRecords().get(0), "executionStatus"));
        assertEquals("{\"confidence\":0.97}", readField(response.getData().getRecords().get(0), "recommendationSnapshotJson"));
        assertEquals("{\"evidenceItems\":[{\"source\":\"catalog\"}]}", readField(response.getData().getRecords().get(0), "evidenceSnapshotJson"));
        assertEquals("{\"affectedRiskPointCount\":3}", readField(response.getData().getRecords().get(0), "impactSnapshotJson"));
        assertEquals("{\"rollbackable\":true}", readField(response.getData().getRecords().get(0), "rollbackSnapshotJson"));
        verify(governanceWorkItemService).pageWorkItems(query, 10001L);
    }

    @Test
    void pageWorkItemsShouldDelegateKeywordAndExecutionStatusFilters() {
        GovernanceWorkItemPageQuery query = new GovernanceWorkItemPageQuery();
        query.setKeyword("2043187508765708289");
        query.setExecutionStatus("PENDING_APPROVAL");
        query.setWorkStatus("OPEN");
        query.setPageNum(1L);
        query.setPageSize(20L);
        when(governanceWorkItemService.pageWorkItems(query, 10001L))
                .thenReturn(PageResult.of(0L, 1L, 20L, List.of()));

        controller.pageWorkItems(query, authentication(10001L));

        verify(governanceWorkItemService).pageWorkItems(org.mockito.ArgumentMatchers.argThat(actual ->
                "2043187508765708289".equals(actual.getKeyword())
                        && "PENDING_APPROVAL".equals(actual.getExecutionStatus())
                        && "OPEN".equals(actual.getWorkStatus())
        ), org.mockito.ArgumentMatchers.eq(10001L));
    }

    @Test
    void blockWorkItemShouldDelegateToService() {
        GovernanceWorkItemTransitionDTO dto = new GovernanceWorkItemTransitionDTO();
        dto.setComment("等待外部确认");

        R<Void> response = controller.blockWorkItem(99L, dto, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(governanceWorkItemService).block(99L, 10001L, "等待外部确认");
    }

    @Test
    void decisionContextShouldDelegateToService() {
        GovernanceDecisionContextVO context = new GovernanceDecisionContextVO();
        context.setWorkItemId(77L);
        context.setPriorityLevel("P1");
        context.setRecommendedAction("Publish contract release");
        when(governanceWorkItemService.getDecisionContext(77L, 10001L)).thenReturn(context);

        R<GovernanceDecisionContextVO> response = controller.getDecisionContext(77L, authentication(10001L));

        assertEquals("P1", response.getData().getPriorityLevel());
        assertEquals("Publish contract release", response.getData().getRecommendedAction());
        verify(governanceWorkItemService).getDecisionContext(77L, 10001L);
    }

    @Test
    void closeReplayWithFeedbackShouldDelegateToService() {
        GovernanceWorkItemTransitionDTO dto = new GovernanceWorkItemTransitionDTO();
        dto.setWorkItemId(88L);
        dto.setApprovalOrderId(8101L);
        dto.setReleaseBatchId(7001L);
        dto.setProductKey("phase2-gnss");
        dto.setRecommendedDecision("PROMOTE");
        dto.setAdoptedDecision("PROMOTE");
        dto.setExecutionOutcome("SUCCESS");
        dto.setRootCauseCode("MISSING_POLICY");
        dto.setOperatorSummary("复盘确认缺少阈值策略");

        R<Void> response = controller.closeReplayWithFeedback(dto, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(governanceWorkItemService).closeReplayWithFeedback(org.mockito.ArgumentMatchers.argThat(command ->
                Long.valueOf(88L).equals(command.workItemId())
                        && Long.valueOf(8101L).equals(command.approvalOrderId())
                        && Long.valueOf(7001L).equals(command.releaseBatchId())
                        && "phase2-gnss".equals(command.productKey())
                        && "PROMOTE".equals(command.recommendedDecision())
                        && "PROMOTE".equals(command.adoptedDecision())
                        && "SUCCESS".equals(command.executionOutcome())
                        && "MISSING_POLICY".equals(command.rootCauseCode())
                        && "复盘确认缺少阈值策略".equals(command.operatorSummary())
        ), org.mockito.ArgumentMatchers.eq(10001L));
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
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

    private static String readField(Object target, String fieldName) {
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
