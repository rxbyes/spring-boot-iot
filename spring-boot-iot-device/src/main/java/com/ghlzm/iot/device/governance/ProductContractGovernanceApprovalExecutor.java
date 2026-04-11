package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import com.ghlzm.iot.system.service.model.GovernanceImpactSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRollbackSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Executes product contract governance approval actions inside the device domain.
 */
@Service
public class ProductContractGovernanceApprovalExecutor implements GovernanceApprovalActionExecutor {

    private static final List<String> APPLY_AFFECTED_TYPES = List.of("PRODUCT", "RISK_METRIC", "RISK_POINT", "RULE");
    private static final List<String> ROLLBACK_AFFECTED_TYPES = List.of("PRODUCT", "RISK_METRIC", "RISK_POINT", "RULE");

    private final ProductModelService productModelService;
    private final ProductContractReleaseService productContractReleaseService;

    public ProductContractGovernanceApprovalExecutor(ProductModelService productModelService,
                                                    ProductContractReleaseService productContractReleaseService) {
        this.productModelService = productModelService;
        this.productContractReleaseService = productContractReleaseService;
    }

    @Override
    public boolean supports(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return false;
        }
        return ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY.equals(actionCode.trim())
                || ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK.equals(actionCode.trim());
    }

    @Override
    public GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order) {
        if (order == null || !StringUtils.hasText(order.getActionCode())) {
            throw new BizException("审批动作不存在");
        }
        String actionCode = order.getActionCode().trim();
        return switch (actionCode) {
            case ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY -> executeApply(order);
            case ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK -> executeRollback(order);
            default -> throw new BizException("审批动作不支持执行: " + actionCode);
        };
    }

    @Override
    public GovernanceSimulationResult simulate(GovernanceApprovalOrder order) {
        if (order == null || !StringUtils.hasText(order.getActionCode())) {
            throw new BizException("审批动作不存在");
        }
        String actionCode = order.getActionCode().trim();
        return switch (actionCode) {
            case ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_RELEASE_APPLY -> simulateApply(order);
            case ProductContractGovernanceApprovalPayloads.ACTION_PRODUCT_CONTRACT_ROLLBACK -> simulateRollback(order);
            default -> throw new BizException("审批动作不支持预演: " + actionCode);
        };
    }

    private GovernanceApprovalActionExecutionResult executeApply(GovernanceApprovalOrder order) {
        ProductContractGovernanceApprovalPayloads.ApplyPayload payload =
                ProductContractGovernanceApprovalPayloads.readApplyPayload(order.getPayloadJson());
        if (payload.request() == null || payload.request().productId() == null) {
            throw new BizException("产品合同生效审批载荷缺少产品标识");
        }
        ProductModelGovernanceApplyDTO request = new ProductModelGovernanceApplyDTO();
        request.setItems(payload.request().items());
        ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(
                payload.request().productId(),
                request,
                order.getOperatorUserId(),
                order.getId()
        );
        String updatedPayloadJson = ProductContractGovernanceApprovalPayloads.writeApplyExecutionPayload(payload, result);
        return new GovernanceApprovalActionExecutionResult(updatedPayloadJson);
    }

    private GovernanceApprovalActionExecutionResult executeRollback(GovernanceApprovalOrder order) {
        ProductContractGovernanceApprovalPayloads.RollbackPayload payload =
                ProductContractGovernanceApprovalPayloads.readRollbackPayload(order.getPayloadJson());
        if (payload.request() == null || payload.request().batchId() == null) {
            throw new BizException("产品合同回滚审批载荷缺少批次标识");
        }
        ProductContractReleaseRollbackResultVO result = productContractReleaseService.rollbackLatestBatch(
                payload.request().batchId(),
                order.getOperatorUserId()
        );
        String updatedPayloadJson = ProductContractGovernanceApprovalPayloads.writeRollbackExecutionPayload(payload, result);
        return new GovernanceApprovalActionExecutionResult(updatedPayloadJson);
    }

    private GovernanceSimulationResult simulateApply(GovernanceApprovalOrder order) {
        ProductContractGovernanceApprovalPayloads.ApplyPayload payload =
                ProductContractGovernanceApprovalPayloads.readApplyPayload(order.getPayloadJson());
        if (payload.request() == null || payload.request().productId() == null) {
            throw new BizException("产品合同生效审批载荷缺少产品标识");
        }
        long affectedCount = countAffectedApplyItems(payload);
        GovernanceImpactSnapshot impact = new GovernanceImpactSnapshot();
        impact.setAffectedCount(affectedCount);
        impact.setAffectedTypes(APPLY_AFFECTED_TYPES);
        impact.setRollbackable(Boolean.TRUE);
        impact.setRollbackPlanSummary("审批通过后可通过合同回滚恢复正式批次");

        GovernanceRollbackSnapshot rollback = new GovernanceRollbackSnapshot();
        rollback.setRollbackable(Boolean.TRUE);
        rollback.setRollbackPlanSummary("审批通过后可通过合同回滚恢复正式批次");
        return new GovernanceSimulationResult(
                order.getId(),
                order.getWorkItemId(),
                order.getActionCode(),
                true,
                affectedCount,
                impact.getAffectedTypes(),
                true,
                impact.getRollbackPlanSummary(),
                null,
                impact,
                rollback,
                false,
                null
        );
    }

    private GovernanceSimulationResult simulateRollback(GovernanceApprovalOrder order) {
        ProductContractGovernanceApprovalPayloads.RollbackPayload payload =
                ProductContractGovernanceApprovalPayloads.readRollbackPayload(order.getPayloadJson());
        if (payload.request() == null || payload.request().batchId() == null) {
            throw new BizException("产品合同回滚审批载荷缺少批次标识");
        }
        GovernanceImpactSnapshot impact = new GovernanceImpactSnapshot();
        impact.setAffectedCount(1L);
        impact.setAffectedTypes(ROLLBACK_AFFECTED_TYPES);
        impact.setRollbackable(Boolean.FALSE);
        impact.setRollbackPlanSummary("回滚执行后需重新发布新的正式批次恢复");

        GovernanceRollbackSnapshot rollback = new GovernanceRollbackSnapshot();
        rollback.setRollbackable(Boolean.FALSE);
        rollback.setRollbackPlanSummary("回滚执行后需重新发布新的正式批次恢复");
        return new GovernanceSimulationResult(
                order.getId(),
                order.getWorkItemId(),
                order.getActionCode(),
                true,
                impact.getAffectedCount(),
                impact.getAffectedTypes(),
                false,
                impact.getRollbackPlanSummary(),
                null,
                impact,
                rollback,
                false,
                null
        );
    }

    private long countAffectedApplyItems(ProductContractGovernanceApprovalPayloads.ApplyPayload payload) {
        if (payload == null || payload.request() == null || payload.request().items() == null) {
            return 0L;
        }
        return payload.request().items().stream()
                .filter(item -> item != null && StringUtils.hasText(item.getDecision()))
                .filter(item -> !"skip".equalsIgnoreCase(item.getDecision().trim()))
                .count();
    }
}
