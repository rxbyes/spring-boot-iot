package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductContractGovernanceApprovalExecutorTest {

    @Mock
    private ProductModelService productModelService;

    @Mock
    private ProductContractReleaseService productContractReleaseService;

    private ProductContractGovernanceApprovalExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ProductContractGovernanceApprovalExecutor(productModelService, productContractReleaseService);
    }

    @Test
    void executeShouldApplyGovernanceAndWriteBackPayload() {
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        ProductModelGovernanceApplyDTO.ApplyItem item = new ProductModelGovernanceApplyDTO.ApplyItem();
        item.setDecision("create");
        item.setModelType("property");
        item.setIdentifier("value");
        item.setModelName("crack value");
        dto.setItems(List.of(item));

        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(88001L);
        order.setActionCode(ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY);
        order.setOperatorUserId(10001L);
        order.setPayloadJson(ProductContractGovernanceApprovalPayloads.writeApplyPayload(1001L, dto));

        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        result.setCreatedCount(1);
        result.setReleaseBatchId(99001L);
        when(productModelService.applyGovernance(eq(1001L), any(ProductModelGovernanceApplyDTO.class), eq(10001L), eq(88001L)))
                .thenReturn(result);

        GovernanceApprovalActionExecutionResult executionResult = executor.execute(order);

        assertNotNull(executionResult);
        ProductContractGovernanceApprovalPayloads.ApplyPayload payload =
                ProductContractGovernanceApprovalPayloads.readApplyPayload(executionResult.payloadJson());
        assertEquals(1001L, payload.request().productId());
        assertEquals(99001L, payload.execution().result().getReleaseBatchId());
        ArgumentCaptor<ProductModelGovernanceApplyDTO> dtoCaptor = ArgumentCaptor.forClass(ProductModelGovernanceApplyDTO.class);
        verify(productModelService).applyGovernance(eq(1001L), dtoCaptor.capture(), eq(10001L), eq(88001L));
        assertEquals(1, dtoCaptor.getValue().getItems().size());
        assertEquals("value", dtoCaptor.getValue().getItems().get(0).getIdentifier());
    }

    @Test
    void executeShouldRollbackBatchAndWriteBackPayload() {
        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setActionCode(ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK);
        order.setOperatorUserId(10001L);
        order.setPayloadJson(ProductContractGovernanceApprovalPayloads.writeRollbackPayload(7001L));

        ProductContractReleaseRollbackResultVO result = new ProductContractReleaseRollbackResultVO();
        result.setTargetBatchId(7001L);
        result.setRolledBackBatchId(7001L);
        result.setProductId(1001L);
        when(productContractReleaseService.rollbackLatestBatch(7001L, 10001L)).thenReturn(result);

        GovernanceApprovalActionExecutionResult executionResult = executor.execute(order);

        assertNotNull(executionResult);
        ProductContractGovernanceApprovalPayloads.RollbackPayload payload =
                ProductContractGovernanceApprovalPayloads.readRollbackPayload(executionResult.payloadJson());
        assertEquals(7001L, payload.request().batchId());
        assertEquals(7001L, payload.execution().result().getRolledBackBatchId());
        verify(productContractReleaseService).rollbackLatestBatch(7001L, 10001L);
    }

    @Test
    void simulateShouldReturnDryRunSummaryForApply() {
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        ProductModelGovernanceApplyDTO.ApplyItem item = new ProductModelGovernanceApplyDTO.ApplyItem();
        item.setDecision("create");
        item.setModelType("property");
        item.setIdentifier("value");
        item.setModelName("crack value");
        dto.setItems(List.of(item));

        GovernanceApprovalOrder order = new GovernanceApprovalOrder();
        order.setId(88008L);
        order.setWorkItemId(73008L);
        order.setActionCode(ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY);
        order.setPayloadJson(ProductContractGovernanceApprovalPayloads.writeApplyPayload(1001L, dto));

        GovernanceSimulationResult result = executor.simulate(order);

        assertNotNull(result);
        assertTrue(result.executable());
        assertEquals(1L, result.affectedCount());
        assertTrue(result.rollbackable());
        assertTrue(result.affectedTypes().contains("RISK_METRIC"));
        assertTrue(result.affectedTypes().contains("RISK_POINT"));
        assertTrue(result.affectedTypes().contains("RULE"));
        verifyNoInteractions(productModelService, productContractReleaseService);
    }
}
