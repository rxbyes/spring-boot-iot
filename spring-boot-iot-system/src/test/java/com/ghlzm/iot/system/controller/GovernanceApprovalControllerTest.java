package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.GovernanceApprovalDecisionDTO;
import com.ghlzm.iot.system.dto.GovernanceApprovalResubmitDTO;
import com.ghlzm.iot.system.service.GovernanceApprovalQueryService;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalTransitionVO;
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
class GovernanceApprovalControllerTest {

    @Mock
    private GovernanceApprovalQueryService governanceApprovalQueryService;

    @Mock
    private GovernanceApprovalService governanceApprovalService;

    private GovernanceApprovalController controller;

    @BeforeEach
    void setUp() {
        controller = new GovernanceApprovalController(governanceApprovalQueryService, governanceApprovalService);
    }

    @Test
    void pageOrdersShouldDelegateToService() {
        GovernanceApprovalOrderVO order = new GovernanceApprovalOrderVO();
        order.setId(99001L);
        order.setActionCode("PRODUCT_CONTRACT_RELEASE_APPLY");
        when(governanceApprovalQueryService.pageOrders(10001L,
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "PRODUCT",
                1001L,
                "APPROVED",
                10001L,
                20002L,
                1L,
                10L)).thenReturn(PageResult.of(1L, 1L, 10L, List.of(order)));

        R<PageResult<GovernanceApprovalOrderVO>> response = controller.pageOrders(
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "PRODUCT",
                1001L,
                "APPROVED",
                10001L,
                20002L,
                1L,
                10L,
                authentication(10001L)
        );

        assertEquals(1L, response.getData().getTotal());
        assertEquals(99001L, response.getData().getRecords().get(0).getId());
        verify(governanceApprovalQueryService).pageOrders(10001L,
                "PRODUCT_CONTRACT_RELEASE_APPLY",
                "PRODUCT",
                1001L,
                "APPROVED",
                10001L,
                20002L,
                1L,
                10L);
    }

    @Test
    void getOrderDetailShouldDelegateToService() {
        GovernanceApprovalOrderVO order = new GovernanceApprovalOrderVO();
        order.setId(99001L);
        GovernanceApprovalTransitionVO transition = new GovernanceApprovalTransitionVO();
        transition.setId(99011L);
        GovernanceApprovalOrderDetailVO detail = new GovernanceApprovalOrderDetailVO();
        detail.setOrder(order);
        detail.setTransitions(List.of(transition));
        when(governanceApprovalQueryService.getOrderDetail(10001L, 99001L)).thenReturn(detail);

        R<GovernanceApprovalOrderDetailVO> response = controller.getOrderDetail(99001L, authentication(10001L));

        assertEquals(99001L, response.getData().getOrder().getId());
        assertEquals(1, response.getData().getTransitions().size());
        verify(governanceApprovalQueryService).getOrderDetail(10001L, 99001L);
    }

    @Test
    void approveOrderShouldDelegateToService() {
        GovernanceApprovalDecisionDTO dto = new GovernanceApprovalDecisionDTO();
        dto.setComment("approved");

        R<Void> response = controller.approveOrder(99001L, dto, authentication(20002L));

        assertEquals(200, response.getCode());
        verify(governanceApprovalService).approveOrder(99001L, 20002L, "approved");
    }

    @Test
    void rejectOrderShouldDelegateToService() {
        GovernanceApprovalDecisionDTO dto = new GovernanceApprovalDecisionDTO();
        dto.setComment("reject reason");

        R<Void> response = controller.rejectOrder(99001L, dto, authentication(20002L));

        assertEquals(200, response.getCode());
        verify(governanceApprovalService).rejectOrder(99001L, 20002L, "reject reason");
    }

    @Test
    void cancelOrderShouldDelegateToService() {
        GovernanceApprovalDecisionDTO dto = new GovernanceApprovalDecisionDTO();
        dto.setComment("cancel current apply");

        R<Void> response = controller.cancelOrder(99001L, dto, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(governanceApprovalService).cancelOrder(99001L, 10001L, "cancel current apply");
    }

    @Test
    void resubmitOrderShouldDelegateToService() {
        GovernanceApprovalResubmitDTO dto = new GovernanceApprovalResubmitDTO();
        dto.setApproverUserId(30003L);
        dto.setComment("resubmit with new approver");

        R<Void> response = controller.resubmitOrder(99001L, dto, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(governanceApprovalService).resubmitOrder(99001L, 10001L, 30003L, "resubmit with new approver");
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
