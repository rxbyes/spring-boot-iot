package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceAppliedItemVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared payload helpers for contract governance approval actions.
 */
public final class ProductContractGovernanceApprovalPayloads {

    public static final String ACTION_PRODUCT_CONTRACT_RELEASE_APPLY = "PRODUCT_CONTRACT_RELEASE_APPLY";
    public static final String ACTION_PRODUCT_CONTRACT_ROLLBACK = "PRODUCT_CONTRACT_ROLLBACK";
    public static final String APPROVAL_STATUS_PENDING = "PENDING";

    private static final Integer PAYLOAD_VERSION = 1;
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private ProductContractGovernanceApprovalPayloads() {
    }

    public static String writeApplyPayload(Long productId, ProductModelGovernanceApplyDTO dto) {
        return writeValue(new ApplyPayload(
                PAYLOAD_VERSION,
                new ApplyRequest(productId, safeItems(dto)),
                null
        ));
    }

    public static ApplyPayload readApplyPayload(String payloadJson) {
        return readValue(payloadJson, ApplyPayload.class, "产品合同生效审批载荷无效");
    }

    public static String writeApplyExecutionPayload(ApplyPayload payload,
                                                    ProductModelGovernanceApplyResultVO result) {
        return writeValue(new ApplyPayload(
                payload == null || payload.version() == null ? PAYLOAD_VERSION : payload.version(),
                payload == null ? null : payload.request(),
                new ApplyExecution(LocalDateTime.now(), result)
        ));
    }

    public static ProductModelGovernanceApplyResultVO buildPendingApplyResult(Long approvalOrderId,
                                                                              ProductModelGovernanceApplyDTO dto) {
        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        for (ProductModelGovernanceApplyDTO.ApplyItem item : safeItems(dto)) {
            String decision = normalizeDecision(item == null ? null : item.getDecision());
            if ("create".equals(decision)) {
                createdCount++;
            } else if ("update".equals(decision)) {
                updatedCount++;
            } else if ("skip".equals(decision)) {
                skippedCount++;
            }
        }
        result.setCreatedCount(createdCount);
        result.setUpdatedCount(updatedCount);
        result.setSkippedCount(skippedCount);
        result.setConflictCount(0);
        result.setReleaseBatchId(null);
        result.setApprovalOrderId(approvalOrderId);
        result.setApprovalStatus(APPROVAL_STATUS_PENDING);
        result.setExecutionPending(Boolean.TRUE);
        result.setAppliedItems(buildAppliedItems(dto));
        return result;
    }

    public static String writeRollbackPayload(Long batchId) {
        return writeValue(new RollbackPayload(
                PAYLOAD_VERSION,
                new RollbackRequest(batchId),
                null
        ));
    }

    public static RollbackPayload readRollbackPayload(String payloadJson) {
        return readValue(payloadJson, RollbackPayload.class, "产品合同回滚审批载荷无效");
    }

    public static String writeRollbackExecutionPayload(RollbackPayload payload,
                                                       ProductContractReleaseRollbackResultVO result) {
        return writeValue(new RollbackPayload(
                payload == null || payload.version() == null ? PAYLOAD_VERSION : payload.version(),
                payload == null ? null : payload.request(),
                new RollbackExecution(LocalDateTime.now(), result)
        ));
    }

    public static ProductContractReleaseRollbackResultVO buildPendingRollbackResult(Long approvalOrderId,
                                                                                    Long batchId,
                                                                                    ProductContractReleaseBatchVO batch) {
        ProductContractReleaseRollbackResultVO result = new ProductContractReleaseRollbackResultVO();
        result.setTargetBatchId(batchId);
        if (batch != null) {
            result.setProductId(batch.getProductId());
            result.setScenarioCode(batch.getScenarioCode());
            result.setReleaseSource(batch.getReleaseSource());
            result.setReleasedFieldCount(batch.getReleasedFieldCount());
        }
        result.setApprovalOrderId(approvalOrderId);
        result.setApprovalStatus(APPROVAL_STATUS_PENDING);
        result.setExecutionPending(Boolean.TRUE);
        return result;
    }

    private static List<ProductModelGovernanceApplyDTO.ApplyItem> safeItems(ProductModelGovernanceApplyDTO dto) {
        if (dto == null || CollectionUtils.isEmpty(dto.getItems())) {
            return List.of();
        }
        return dto.getItems().stream()
                .filter(item -> item != null)
                .toList();
    }

    private static List<ProductModelGovernanceAppliedItemVO> buildAppliedItems(ProductModelGovernanceApplyDTO dto) {
        List<ProductModelGovernanceAppliedItemVO> result = new ArrayList<>();
        for (ProductModelGovernanceApplyDTO.ApplyItem item : safeItems(dto)) {
            ProductModelGovernanceAppliedItemVO appliedItem = new ProductModelGovernanceAppliedItemVO();
            appliedItem.setModelType(item.getModelType());
            appliedItem.setIdentifier(item.getIdentifier());
            appliedItem.setDecision(normalizeDecision(item.getDecision()));
            result.add(appliedItem);
        }
        return result;
    }

    private static String normalizeDecision(String decision) {
        return StringUtils.hasText(decision) ? decision.trim().toLowerCase() : null;
    }

    private static String writeValue(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BizException("审批载荷序列化失败");
        }
    }

    private static <T> T readValue(String payloadJson, Class<T> type, String errorMessage) {
        if (!StringUtils.hasText(payloadJson)) {
            throw new BizException(errorMessage);
        }
        try {
            return OBJECT_MAPPER.readValue(payloadJson, type);
        } catch (Exception ex) {
            throw new BizException(errorMessage);
        }
    }

    public record ApplyPayload(Integer version,
                               ApplyRequest request,
                               ApplyExecution execution) {
    }

    public record ApplyRequest(Long productId,
                               List<ProductModelGovernanceApplyDTO.ApplyItem> items) {
    }

    public record ApplyExecution(LocalDateTime executedAt,
                                 ProductModelGovernanceApplyResultVO result) {
    }

    public record RollbackPayload(Integer version,
                                  RollbackRequest request,
                                  RollbackExecution execution) {
    }

    public record RollbackRequest(Long batchId) {
    }

    public record RollbackExecution(LocalDateTime executedAt,
                                    ProductContractReleaseRollbackResultVO result) {
    }
}
