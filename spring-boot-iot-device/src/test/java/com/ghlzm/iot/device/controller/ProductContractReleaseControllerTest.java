package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseImpactVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductContractReleaseControllerTest {

    @Mock
    private ProductContractReleaseService productContractReleaseService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    @Mock
    private GovernanceApprovalService governanceApprovalService;

    private ProductContractReleaseController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductContractReleaseController(productContractReleaseService, permissionGuard, governanceApprovalService);
    }

    @Test
    void pageBatchesShouldDelegateToService() {
        ProductContractReleaseBatchVO batch = batchVO(7001L, "phase1-crack", "manual_compare_apply", 3);
        when(productContractReleaseService.pageBatches(1001L, 1L, 10L))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(batch)));

        R<PageResult<ProductContractReleaseBatchVO>> response = controller.pageBatches(1001L, 1L, 10L);

        assertEquals(1L, response.getData().getTotal());
        assertEquals(7001L, response.getData().getRecords().get(0).getId());
        verify(productContractReleaseService).pageBatches(1001L, 1L, 10L);
    }

    @Test
    void getBatchShouldDelegateToService() {
        ProductContractReleaseBatchVO batch = batchVO(7001L, "phase1-crack", "manual_compare_apply", 3);
        batch.setApprovalOrderId(88001L);
        batch.setReleaseReason("首次合同发布");
        batch.setReleaseStatus("RELEASED");
        when(productContractReleaseService.getBatch(7001L)).thenReturn(batch);

        R<ProductContractReleaseBatchVO> response = controller.getBatch(7001L);

        assertEquals("phase1-crack", response.getData().getScenarioCode());
        assertEquals(88001L, response.getData().getApprovalOrderId());
        assertEquals("首次合同发布", response.getData().getReleaseReason());
        assertEquals("RELEASED", response.getData().getReleaseStatus());
        verify(productContractReleaseService).getBatch(7001L);
    }

    @Test
    void analyzeBatchImpactShouldDelegateToService() {
        ProductContractReleaseImpactVO impact = new ProductContractReleaseImpactVO();
        impact.setBatchId(7001L);
        impact.setAddedCount(1);
        impact.setRemovedCount(1);
        impact.setChangedCount(2);
        when(productContractReleaseService.analyzeBatchImpact(7001L)).thenReturn(impact);

        R<ProductContractReleaseImpactVO> response = controller.analyzeBatchImpact(7001L);

        assertEquals(7001L, response.getData().getBatchId());
        assertEquals(1, response.getData().getAddedCount());
        verify(productContractReleaseService).analyzeBatchImpact(7001L);
    }

    @Test
    void rollbackBatchShouldSubmitPendingApproval() {
        Authentication authentication = authentication(10001L);
        when(productContractReleaseService.getBatch(7001L))
                .thenReturn(batchVO(7001L, "phase1-crack", "manual_compare_apply", 3));
        when(governanceApprovalService.submitAction(any())).thenReturn(99001L);

        R<ProductContractReleaseRollbackResultVO> response = controller.rollbackBatch(7001L, 20002L, authentication);

        assertEquals(7001L, response.getData().getTargetBatchId());
        assertEquals(1001L, response.getData().getProductId());
        assertEquals("phase1-crack", response.getData().getScenarioCode());
        assertEquals("manual_compare_apply", response.getData().getReleaseSource());
        assertEquals(3, response.getData().getReleasedFieldCount());
        assertEquals(99001L, response.getData().getApprovalOrderId());
        assertEquals("PENDING", response.getData().getApprovalStatus());
        assertEquals(Boolean.TRUE, response.getData().getExecutionPending());
        verify(permissionGuard).requireDualControl(
                10001L,
                20002L,
                "合同发布回滚",
                "iot:product-contract:rollback",
                "iot:product-contract:approve"
        );
        verify(governanceApprovalService).submitAction(any());
        verify(productContractReleaseService).getBatch(7001L);
        verify(productContractReleaseService, never()).rollbackLatestBatch(7001L, 10001L);
    }

    private ProductContractReleaseBatchVO batchVO(Long id,
                                                  String scenarioCode,
                                                  String releaseSource,
                                                  Integer releasedFieldCount) {
        ProductContractReleaseBatchVO vo = new ProductContractReleaseBatchVO();
        vo.setId(id);
        vo.setProductId(1001L);
        vo.setScenarioCode(scenarioCode);
        vo.setReleaseSource(releaseSource);
        vo.setReleasedFieldCount(releasedFieldCount);
        vo.setCreateBy(10001L);
        vo.setCreateTime(LocalDateTime.of(2026, 4, 6, 9, 30));
        return vo;
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
