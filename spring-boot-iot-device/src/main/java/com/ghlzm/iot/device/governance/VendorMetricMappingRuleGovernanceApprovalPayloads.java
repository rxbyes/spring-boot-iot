package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 厂商字段映射规则治理审批载荷。
 */
public final class VendorMetricMappingRuleGovernanceApprovalPayloads {

    public static final String ACTION_VENDOR_MAPPING_RULE_PUBLISH = "VENDOR_MAPPING_RULE_PUBLISH";
    public static final String ACTION_VENDOR_MAPPING_RULE_ROLLBACK = "VENDOR_MAPPING_RULE_ROLLBACK";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private VendorMetricMappingRuleGovernanceApprovalPayloads() {
    }

    public static GovernanceApprovalActionCommand buildPublishCommand(VendorMetricMappingRule rule,
                                                                      Long operatorUserId,
                                                                      Long approverUserId,
                                                                      String submitReason) {
        return new GovernanceApprovalActionCommand(
                ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                "厂商字段映射规则发布",
                "VENDOR_MAPPING_RULE",
                rule == null ? null : rule.getId(),
                operatorUserId,
                approverUserId,
                writePayload(rule, submitReason),
                normalizeText(submitReason)
        );
    }

    public static GovernanceApprovalActionCommand buildRollbackCommand(VendorMetricMappingRule rule,
                                                                       Long operatorUserId,
                                                                       Long approverUserId,
                                                                       String submitReason) {
        return new GovernanceApprovalActionCommand(
                ACTION_VENDOR_MAPPING_RULE_ROLLBACK,
                "厂商字段映射规则回滚",
                "VENDOR_MAPPING_RULE",
                rule == null ? null : rule.getId(),
                operatorUserId,
                approverUserId,
                writePayload(rule, submitReason),
                normalizeText(submitReason)
        );
    }

    private static String writePayload(VendorMetricMappingRule rule, String submitReason) {
        if (rule == null || rule.getId() == null) {
            throw new BizException("厂商字段映射规则不存在");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleId", rule.getId());
        payload.put("productId", rule.getProductId());
        payload.put("expectedVersionNo", rule.getVersionNo());
        payload.put("rawIdentifier", normalizeText(rule.getRawIdentifier()));
        payload.put("logicalChannelCode", normalizeText(rule.getLogicalChannelCode()));
        payload.put("targetNormativeIdentifier", normalizeText(rule.getTargetNormativeIdentifier()));
        payload.put("scopeType", normalizeText(rule.getScopeType()));
        payload.put("submitReason", normalizeText(submitReason));
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BizException("厂商字段映射规则审批载荷序列化失败");
        }
    }

    private static String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
