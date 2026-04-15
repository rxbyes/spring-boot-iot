package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.service.GovernancePermissionMatrixService;
import com.ghlzm.iot.system.vo.GovernancePermissionMatrixItemVO;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 治理权限矩阵固定读侧实现。
 */
@Service
public class GovernancePermissionMatrixServiceImpl implements GovernancePermissionMatrixService {

    private static final List<String> OPERATOR_ROLES = List.of("SUPER_ADMIN", "OPS_STAFF");
    private static final List<String> APPROVER_ROLES = List.of("SUPER_ADMIN", "MANAGEMENT_STAFF");
    private static final List<String> LIBRARY_OPERATOR_ROLES = List.of("SUPER_ADMIN", "MANAGEMENT_STAFF");

    @Override
    public List<GovernancePermissionMatrixItemVO> listMatrix() {
        return List.of(
                item("NORMATIVE_LIBRARY", "规范库治理", "NORMATIVE_LIBRARY_EDIT", "规范库维护",
                        GovernancePermissionCodes.NORMATIVE_LIBRARY_EDIT, GovernancePermissionCodes.NORMATIVE_LIBRARY_APPROVE,
                        LIBRARY_OPERATOR_ROLES, APPROVER_ROLES, true, "normative-library"),
                item("PRODUCT_CONTRACT", "产品合同治理", "PRODUCT_CONTRACT_RELEASE", "合同发布",
                        GovernancePermissionCodes.PRODUCT_CONTRACT_RELEASE, GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "product-contract-release"),
                item("PRODUCT_CONTRACT", "产品合同治理", "PRODUCT_CONTRACT_ROLLBACK", "合同回滚",
                        GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK, GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "product-contract-rollback"),
                item("PRODUCT_CONTRACT", "产品合同治理", "VENDOR_MAPPING_RULE_PUBLISH", "映射规则发布",
                        GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN, GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "vendor-mapping-rule-publish"),
                item("PRODUCT_CONTRACT", "产品合同治理", "VENDOR_MAPPING_RULE_ROLLBACK", "映射规则回滚",
                        GovernancePermissionCodes.PRODUCT_CONTRACT_ROLLBACK, GovernancePermissionCodes.PRODUCT_CONTRACT_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "vendor-mapping-rule-rollback"),
                item("RISK_METRIC", "风险指标治理", "RISK_METRIC_CATALOG_TAG", "风险指标标注",
                        GovernancePermissionCodes.RISK_METRIC_CATALOG_TAG, GovernancePermissionCodes.RISK_METRIC_CATALOG_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "risk-metric-catalog"),
                item("RISK_POLICY", "风险策略治理", "RULE_DEFINITION_EDIT", "阈值策略维护",
                        GovernancePermissionCodes.RULE_DEFINITION_EDIT, GovernancePermissionCodes.RULE_DEFINITION_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "risk-rule-definition"),
                item("RISK_POLICY", "风险策略治理", "LINKAGE_RULE_EDIT", "联动规则维护",
                        GovernancePermissionCodes.LINKAGE_RULE_EDIT, GovernancePermissionCodes.LINKAGE_RULE_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "risk-linkage-rule"),
                item("RISK_POLICY", "风险策略治理", "EMERGENCY_PLAN_EDIT", "应急预案维护",
                        GovernancePermissionCodes.EMERGENCY_PLAN_EDIT, GovernancePermissionCodes.EMERGENCY_PLAN_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "risk-emergency-plan"),
                item("SECRET_CUSTODY", "密钥托管治理", "DEVICE_SECRET_ROTATE", "设备密钥轮换",
                        GovernancePermissionCodes.SECRET_CUSTODY_ROTATE, GovernancePermissionCodes.SECRET_CUSTODY_APPROVE,
                        OPERATOR_ROLES, APPROVER_ROLES, true, "device-secret-custody")
        );
    }

    private GovernancePermissionMatrixItemVO item(String domainCode,
                                                  String domainName,
                                                  String actionCode,
                                                  String actionName,
                                                  String operatorPermissionCode,
                                                  String approverPermissionCode,
                                                  List<String> defaultRoleCodes,
                                                  List<String> defaultApproverRoleCodes,
                                                  boolean dualControlRequired,
                                                  String auditModule) {
        GovernancePermissionMatrixItemVO item = new GovernancePermissionMatrixItemVO();
        item.setDomainCode(domainCode);
        item.setDomainName(domainName);
        item.setActionCode(actionCode);
        item.setActionName(actionName);
        item.setOperatorPermissionCode(operatorPermissionCode);
        item.setApproverPermissionCode(approverPermissionCode);
        item.setDefaultRoleCodes(defaultRoleCodes);
        item.setDefaultApproverRoleCodes(defaultApproverRoleCodes);
        item.setDualControlRequired(dualControlRequired);
        item.setAuditModule(auditModule);
        return item;
    }
}
