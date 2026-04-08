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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Executes product contract governance approval actions inside the device domain.
 */
@Service
public class ProductContractGovernanceApprovalExecutor implements GovernanceApprovalActionExecutor {

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
}
