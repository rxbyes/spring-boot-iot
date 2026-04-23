package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductContractGovernanceApprovalPayloadsTest {

    @Test
    void buildPendingApplyResultShouldExposeSubmissionLayerCounts() {
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(
                item("create", "value"),
                item("update", "sensor_state"),
                item("skip", "temp")
        ));

        ProductModelGovernanceApplyResultVO result =
                ProductContractGovernanceApprovalPayloads.buildPendingApplyResult(88001L, dto);

        assertEquals(3, result.getSubmittedItemCount());
        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(88001L, result.getApprovalOrderId());
        assertEquals("PENDING", result.getApprovalStatus());
        assertNull(result.getReleaseBatchId());
        assertTrue(Boolean.TRUE.equals(result.getExecutionPending()));
    }

    @Test
    void writeApplyExecutionPayloadShouldPreserveSubmissionAndExecutionFields() {
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        dto.setItems(List.of(item("create", "value")));
        ProductContractGovernanceApprovalPayloads.ApplyPayload payload =
                ProductContractGovernanceApprovalPayloads.readApplyPayload(
                        ProductContractGovernanceApprovalPayloads.writeApplyPayload(1001L, dto)
                );

        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        result.setSubmittedItemCount(1);
        result.setCreatedCount(1);
        result.setReleaseBatchId(99001L);
        result.setApprovalOrderId(88001L);
        result.setApprovalStatus("APPROVED");
        result.setExecutionPending(Boolean.FALSE);

        ProductContractGovernanceApprovalPayloads.ApplyPayload executionPayload =
                ProductContractGovernanceApprovalPayloads.readApplyPayload(
                        ProductContractGovernanceApprovalPayloads.writeApplyExecutionPayload(payload, result)
                );

        assertNotNull(executionPayload.execution());
        assertNotNull(executionPayload.execution().result());
        assertEquals(1, executionPayload.execution().result().getSubmittedItemCount());
        assertEquals(1, executionPayload.execution().result().getCreatedCount());
        assertEquals(99001L, executionPayload.execution().result().getReleaseBatchId());
    }

    private ProductModelGovernanceApplyDTO.ApplyItem item(String decision, String identifier) {
        ProductModelGovernanceApplyDTO.ApplyItem item = new ProductModelGovernanceApplyDTO.ApplyItem();
        item.setDecision(decision);
        item.setModelType("property");
        item.setIdentifier(identifier);
        item.setModelName(identifier);
        return item;
    }
}
