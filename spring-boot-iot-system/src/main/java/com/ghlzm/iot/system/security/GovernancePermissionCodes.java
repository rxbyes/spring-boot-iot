package com.ghlzm.iot.system.security;

/**
 * 治理与运营关键写操作权限编码。
 */
public final class GovernancePermissionCodes {

    private GovernancePermissionCodes() {
    }

    // 规范库维护（执行/复核）
    public static final String NORMATIVE_LIBRARY_EDIT = "iot:normative-library:write";
    public static final String NORMATIVE_LIBRARY_APPROVE = "iot:normative-library:approve";
    // 兼容旧命名
    public static final String NORMATIVE_LIBRARY_WRITE = NORMATIVE_LIBRARY_EDIT;

    // 契约治理/发布/回滚
    public static final String PRODUCT_CONTRACT_GOVERN = "iot:product-contract:govern";

    public static final String PRODUCT_CONTRACT_RELEASE = "iot:product-contract:release";

    public static final String PRODUCT_CONTRACT_APPROVE = "iot:product-contract:approve";

    public static final String PRODUCT_CONTRACT_ROLLBACK = "iot:product-contract:rollback";

    // 风险指标标注（执行/复核）
    public static final String RISK_METRIC_CATALOG_TAG = "risk:metric-catalog:tag";
    public static final String RISK_METRIC_CATALOG_APPROVE = "risk:metric-catalog:approve";
    public static final String RISK_POINT_BIND_APPROVE = "risk:risk-point-binding:approve";
    public static final String RISK_POINT_BIND_EXECUTE = "risk:risk-point-binding:execute";
    public static final String RISK_POINT_PENDING_PROMOTION_APPROVE = "risk:risk-point-pending-promotion:approve";
    public static final String RISK_POINT_PENDING_PROMOTION_EXECUTE = "risk:risk-point-pending-promotion:execute";

    // 阈值策略：执行/复核
    public static final String RULE_DEFINITION_EDIT = "risk:rule-definition:edit";
    public static final String RULE_DEFINITION_APPROVE = "risk:rule-definition:approve";

    // 联动编排：执行/复核
    public static final String LINKAGE_RULE_EDIT = "risk:linkage-rule:edit";
    public static final String LINKAGE_RULE_APPROVE = "risk:linkage-rule:approve";
    public static final String LINKAGE_PLAN_APPROVE = "risk:linkage-plan:approve";

    // 应急预案：执行/复核
    public static final String EMERGENCY_PLAN_EDIT = "risk:emergency-plan:edit";
    public static final String EMERGENCY_PLAN_APPROVE = "risk:emergency-plan:approve";

    // 密钥托管：查看/执行/复核
    public static final String SECRET_CUSTODY_VIEW = "iot:secret-custody:view";
    public static final String SECRET_CUSTODY_ROTATE = "iot:secret-custody:rotate";
    public static final String SECRET_CUSTODY_APPROVE = "iot:secret-custody:approve";
}
