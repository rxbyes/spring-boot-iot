package com.ghlzm.iot.system.security;

/**
 * 治理与运营关键写操作权限编码。
 */
public final class GovernancePermissionCodes {

    private GovernancePermissionCodes() {
    }

    public static final String PRODUCT_CONTRACT_WRITE = "iot:products:update";

    public static final String RULE_DEFINITION_WRITE = "risk:rule-definition:write";

    public static final String LINKAGE_RULE_WRITE = "risk:linkage-rule:write";

    public static final String EMERGENCY_PLAN_WRITE = "risk:emergency-plan:write";
}
