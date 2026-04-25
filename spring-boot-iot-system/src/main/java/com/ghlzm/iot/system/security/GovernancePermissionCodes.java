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

    // 协议治理：执行/复核
    public static final String PROTOCOL_GOVERNANCE_EDIT = "iot:protocol-governance:edit";
    public static final String PROTOCOL_GOVERNANCE_APPROVE = "iot:protocol-governance:approve";
    public static final String PROTOCOL_GOVERNANCE_FAMILY_DRAFT = "iot:protocol-governance:family-draft";
    public static final String PROTOCOL_GOVERNANCE_FAMILY_PUBLISH = "iot:protocol-governance:family-publish";
    public static final String PROTOCOL_GOVERNANCE_FAMILY_ROLLBACK = "iot:protocol-governance:family-rollback";
    public static final String PROTOCOL_GOVERNANCE_DECRYPT_DRAFT = "iot:protocol-governance:decrypt-draft";
    public static final String PROTOCOL_GOVERNANCE_DECRYPT_PREVIEW = "iot:protocol-governance:decrypt-preview";
    public static final String PROTOCOL_GOVERNANCE_DECRYPT_REPLAY = "iot:protocol-governance:decrypt-replay";
    public static final String PROTOCOL_GOVERNANCE_DECRYPT_PUBLISH = "iot:protocol-governance:decrypt-publish";
    public static final String PROTOCOL_GOVERNANCE_DECRYPT_ROLLBACK = "iot:protocol-governance:decrypt-rollback";
    public static final String PROTOCOL_GOVERNANCE_TEMPLATE_DRAFT = "iot:protocol-governance:template-draft";
    public static final String PROTOCOL_GOVERNANCE_TEMPLATE_REPLAY = "iot:protocol-governance:template-replay";
    public static final String PROTOCOL_GOVERNANCE_TEMPLATE_PUBLISH = "iot:protocol-governance:template-publish";

    // 无代码接入台
    public static final String DEVICE_ONBOARDING_CREATE_CASE = "iot:device-onboarding:create-case";
    public static final String DEVICE_ONBOARDING_UPDATE_CASE = "iot:device-onboarding:update-case";
    public static final String DEVICE_ONBOARDING_TEMPLATE_PACK = "iot:device-onboarding:template-pack";
    public static final String DEVICE_ONBOARDING_BATCH_CREATE = "iot:device-onboarding:batch-create";
    public static final String DEVICE_ONBOARDING_BATCH_APPLY_TEMPLATE = "iot:device-onboarding:batch-apply-template";
    public static final String DEVICE_ONBOARDING_START_ACCEPTANCE = "iot:device-onboarding:start-acceptance";
    public static final String DEVICE_ONBOARDING_REFRESH_STATUS = "iot:device-onboarding:refresh-status";

    // 设备资产与设备能力
    public static final String PRODUCT_ADD = "iot:products:add";
    public static final String PRODUCT_UPDATE = "iot:products:update";
    public static final String PRODUCT_DELETE = "iot:products:delete";
    public static final String DEVICE_ADD = "iot:devices:add";
    public static final String DEVICE_UPDATE = "iot:devices:update";
    public static final String DEVICE_DELETE = "iot:devices:delete";
    public static final String DEVICE_IMPORT = "iot:devices:import";
    public static final String DEVICE_REPLACE = "iot:devices:replace";
    public static final String DEVICE_CAPABILITY_EXECUTE = "iot:device-capability:execute";

    // 系统基础资料维护
    public static final String CHANNEL_ADD = "system:channel:add";
    public static final String CHANNEL_UPDATE = "system:channel:update";
    public static final String CHANNEL_DELETE = "system:channel:delete";
    public static final String CHANNEL_TEST = "system:channel:test";
    public static final String DICT_ADD = "system:dict:add";
    public static final String DICT_UPDATE = "system:dict:update";
    public static final String DICT_DELETE = "system:dict:delete";
    public static final String DICT_ITEM_ADD = "system:dict-item:add";
    public static final String DICT_ITEM_UPDATE = "system:dict-item:update";
    public static final String DICT_ITEM_DELETE = "system:dict-item:delete";
    public static final String ORGANIZATION_ADD = "system:organization:add";
    public static final String ORGANIZATION_UPDATE = "system:organization:update";
    public static final String ORGANIZATION_DELETE = "system:organization:delete";
    public static final String REGION_ADD = "system:region:add";
    public static final String REGION_UPDATE = "system:region:update";
    public static final String REGION_DELETE = "system:region:delete";
    public static final String USER_ADD = "system:user:add";
    public static final String USER_UPDATE = "system:user:update";
    public static final String USER_DELETE = "system:user:delete";
    public static final String USER_RESET_PASSWORD = "system:user:reset-password";
    public static final String ROLE_ADD = "system:role:add";
    public static final String ROLE_UPDATE = "system:role:update";
    public static final String ROLE_DELETE = "system:role:delete";
    public static final String MENU_ADD = "system:menu:add";
    public static final String MENU_UPDATE = "system:menu:update";
    public static final String MENU_DELETE = "system:menu:delete";
    public static final String IN_APP_MESSAGE_ADD = "system:in-app-message:add";
    public static final String IN_APP_MESSAGE_UPDATE = "system:in-app-message:update";
    public static final String IN_APP_MESSAGE_DELETE = "system:in-app-message:delete";
    public static final String HELP_DOC_ADD = "system:help-doc:add";
    public static final String HELP_DOC_UPDATE = "system:help-doc:update";
    public static final String HELP_DOC_DELETE = "system:help-doc:delete";
    public static final String AUDIT_DELETE = "system:audit:delete";

    // 治理控制面
    public static final String GOVERNANCE_TASK_DECISION_CONTEXT = "system:governance-task:decision-context";
    public static final String GOVERNANCE_TASK_ACK = "system:governance-task:ack";
    public static final String GOVERNANCE_TASK_BLOCK = "system:governance-task:block";
    public static final String GOVERNANCE_TASK_CLOSE = "system:governance-task:close";
    public static final String GOVERNANCE_TASK_REPLAY_FEEDBACK = "system:governance-task:replay-feedback";
    public static final String GOVERNANCE_OPS_ACK = "system:governance-ops:ack";
    public static final String GOVERNANCE_OPS_SUPPRESS = "system:governance-ops:suppress";
    public static final String GOVERNANCE_OPS_CLOSE = "system:governance-ops:close";
    public static final String GOVERNANCE_OPS_REPLAY_FEEDBACK = "system:governance-ops:replay-feedback";

    // 风险指标标注（执行/复核）
    public static final String RISK_METRIC_CATALOG_TAG = "risk:metric-catalog:tag";
    public static final String RISK_METRIC_CATALOG_APPROVE = "risk:metric-catalog:approve";
    public static final String RISK_POINT_BIND_APPROVE = "risk:risk-point-binding:approve";
    public static final String RISK_POINT_BIND_EXECUTE = "risk:risk-point-binding:execute";
    public static final String RISK_POINT_PENDING_PROMOTION_APPROVE = "risk:risk-point-pending-promotion:approve";
    public static final String RISK_POINT_PENDING_PROMOTION_EXECUTE = "risk:risk-point-pending-promotion:execute";
    public static final String RISK_POINT_ADD = "risk:point:add";
    public static final String RISK_POINT_UPDATE = "risk:point:update";
    public static final String RISK_POINT_DELETE = "risk:point:delete";
    public static final String ALARM_CONFIRM = "risk:alarm:confirm";
    public static final String ALARM_SUPPRESS = "risk:alarm:suppress";
    public static final String ALARM_CLOSE = "risk:alarm:close";
    public static final String EVENT_DISPATCH = "risk:event:dispatch";
    public static final String EVENT_CLOSE = "risk:event:close";

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
