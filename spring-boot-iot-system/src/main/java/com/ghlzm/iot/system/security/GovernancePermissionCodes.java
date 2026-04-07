package com.ghlzm.iot.system.security;

/**
 * 治理与运营关键写操作权限编码。
 */
public final class GovernancePermissionCodes {

    private GovernancePermissionCodes() {
    }

    public static final String PRODUCT_CONTRACT_WRITE = "iot:products:update";

    public static final String NORMATIVE_LIBRARY_WRITE = "iot:normative-library:write";

    public static final String PRODUCT_CONTRACT_GOVERN = "iot:product-contract:govern";

    public static final String PRODUCT_CONTRACT_RELEASE = "iot:product-contract:release";

    public static final String PRODUCT_CONTRACT_APPROVE = "iot:product-contract:approve";

    public static final String PRODUCT_CONTRACT_ROLLBACK = "iot:product-contract:rollback";

    public static final String RISK_METRIC_CATALOG_TAG = "risk:metric-catalog:tag";

    public static final String RULE_DEFINITION_WRITE = "risk:rule-definition:write";
    public static final String RULE_DEFINITION_EDIT = "risk:rule-definition:edit";
    public static final String RULE_DEFINITION_APPROVE = "risk:rule-definition:approve";

    public static final String LINKAGE_RULE_WRITE = "risk:linkage-rule:write";
    public static final String LINKAGE_RULE_EDIT = "risk:linkage-rule:edit";
    public static final String LINKAGE_RULE_APPROVE = "risk:linkage-rule:approve";

    public static final String EMERGENCY_PLAN_WRITE = "risk:emergency-plan:write";
    public static final String EMERGENCY_PLAN_EDIT = "risk:emergency-plan:edit";
    public static final String EMERGENCY_PLAN_APPROVE = "risk:emergency-plan:approve";

    public static final String SECRET_CUSTODY_VIEW = "iot:secret-custody:view";
    public static final String SECRET_CUSTODY_ROTATE = "iot:secret-custody:rotate";
    public static final String SECRET_CUSTODY_APPROVE = "iot:secret-custody:approve";
}
