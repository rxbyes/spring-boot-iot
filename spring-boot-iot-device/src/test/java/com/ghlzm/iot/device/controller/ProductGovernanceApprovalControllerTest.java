package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.governance.ProductContractGovernanceApprovalPayloads;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalQueryService;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderDetailVO;
import com.ghlzm.iot.system.vo.GovernanceApprovalOrderVO;
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
class ProductGovernanceApprovalControllerTest {

    @Mock
    private GovernanceApprovalQueryService governanceApprovalQueryService;

    @Mock
    private GovernanceApprovalService governanceApprovalService;

    @Mock
    private GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private ProductGovernanceApprovalController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductGovernanceApprovalController(
                governanceApprovalQueryService,
                governanceApprovalService,
                governanceApprovalPolicyResolver,
                permissionGuard
        );
    }

    @Test
    void resubmitOrderShouldResolveFixedApproverForProductContractApplyAction() {
        GovernanceApprovalOrderVO order = new GovernanceApprovalOrderVO();
        order.setId(88001L);
        order.setActionCode(ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY);
        GovernanceApprovalOrderDetailVO detail = new GovernanceApprovalOrderDetailVO();
        detail.setOrder(order);
        detail.setTransitions(List.of());
        when(governanceApprovalQueryService.getOrderDetail(10001L, 88001L)).thenReturn(detail);
        when(governanceApprovalPolicyResolver.resolveApproverUserId(
                ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY,
                10001L
        )).thenReturn(99000001L);

        R<Void> response = controller.resubmitOrder(88001L, authentication(10001L));

        assertEquals(200, response.getCode());
        verify(permissionGuard).requireAnyPermission(
                10001L,
                "产品合同原单重提",
                GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE
        );
        verify(governanceApprovalService).resubmitOrder(88001L, 10001L, 99000001L, null);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
