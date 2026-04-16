package com.ghlzm.iot.device.governance;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
        RuleApprovalPayload payload = buildPayload(rule, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_VENDOR_MAPPING_RULE_PUBLISH,
                "厂商字段映射规则发布",
                "VENDOR_MAPPING_RULE",
                rule == null ? null : rule.getId(),
                operatorUserId,
                approverUserId,
                writePayload(payload),
                normalizeText(submitReason)
        );
    }

    public static GovernanceApprovalActionCommand buildRollbackCommand(VendorMetricMappingRule rule,
                                                                       Long operatorUserId,
                                                                       Long approverUserId,
                                                                       String submitReason) {
        RuleApprovalPayload payload = buildPayload(rule, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_VENDOR_MAPPING_RULE_ROLLBACK,
                "厂商字段映射规则回滚",
                "VENDOR_MAPPING_RULE",
                rule == null ? null : rule.getId(),
                operatorUserId,
                approverUserId,
                writePayload(payload),
                normalizeText(submitReason)
        );
    }

    public static RuleApprovalPayload readPublishPayload(String payloadJson) {
        return readPayload(payloadJson, "厂商字段映射规则发布审批载荷解析失败");
    }

    public static RuleApprovalPayload readRollbackPayload(String payloadJson) {
        return readPayload(payloadJson, "厂商字段映射规则回滚审批载荷解析失败");
    }

    public static String writePublishExecutionPayload(RuleApprovalPayload payload,
                                                      Long approvalOrderId,
                                                      Integer publishedVersionNo) {
        return writePayload(copyWithExecution(payload, approvalOrderId, publishedVersionNo, "PUBLISHED"));
    }

    public static String writeRollbackExecutionPayload(RuleApprovalPayload payload,
                                                       Long approvalOrderId,
                                                       Integer publishedVersionNo) {
        return writePayload(copyWithExecution(payload, approvalOrderId, publishedVersionNo, "ROLLED_BACK"));
    }

    public static String writeSnapshotJson(VendorMetricMappingRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new BizException("厂商字段映射规则不存在");
        }
        return writePayload(new RuleApprovalPayload(
                rule.getId(),
                rule.getProductId(),
                rule.getVersionNo(),
                normalizeText(rule.getRawIdentifier()),
                normalizeText(rule.getLogicalChannelCode()),
                normalizeText(rule.getTargetNormativeIdentifier()),
                normalizeText(rule.getScopeType()),
                normalizeText(rule.getProtocolCode()),
                normalizeText(rule.getScenarioCode()),
                normalizeText(rule.getDeviceFamily()),
                normalizeText(rule.getRelationConditionJson()),
                normalizeText(rule.getNormalizationRuleJson()),
                null,
                null
        ));
    }

    private static RuleApprovalPayload buildPayload(VendorMetricMappingRule rule, String submitReason) {
        if (rule == null || rule.getId() == null) {
            throw new BizException("厂商字段映射规则不存在");
        }
        return new RuleApprovalPayload(
                rule.getId(),
                rule.getProductId(),
                rule.getVersionNo(),
                normalizeText(rule.getRawIdentifier()),
                normalizeText(rule.getLogicalChannelCode()),
                normalizeText(rule.getTargetNormativeIdentifier()),
                normalizeText(rule.getScopeType()),
                normalizeText(rule.getProtocolCode()),
                normalizeText(rule.getScenarioCode()),
                normalizeText(rule.getDeviceFamily()),
                normalizeText(rule.getRelationConditionJson()),
                normalizeText(rule.getNormalizationRuleJson()),
                normalizeText(submitReason),
                null
        );
    }

    private static RuleApprovalPayload copyWithExecution(RuleApprovalPayload payload,
                                                         Long approvalOrderId,
                                                         Integer publishedVersionNo,
                                                         String lifecycleStatus) {
        RuleApprovalPayload normalized = requirePayload(payload);
        return new RuleApprovalPayload(
                normalized.ruleId(),
                normalized.productId(),
                normalized.expectedVersionNo(),
                normalized.rawIdentifier(),
                normalized.logicalChannelCode(),
                normalized.targetNormativeIdentifier(),
                normalized.scopeType(),
                normalized.protocolCode(),
                normalized.scenarioCode(),
                normalized.deviceFamily(),
                normalized.relationConditionJson(),
                normalized.normalizationRuleJson(),
                normalized.submitReason(),
                new RuleApprovalExecution(
                        approvalOrderId,
                        publishedVersionNo,
                        normalizeText(lifecycleStatus)
                )
        );
    }

    private static RuleApprovalPayload readPayload(String payloadJson, String errorMessage) {
        try {
            return requirePayload(OBJECT_MAPPER.readValue(payloadJson, RuleApprovalPayload.class));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(errorMessage);
        }
    }

    private static RuleApprovalPayload requirePayload(RuleApprovalPayload payload) {
        if (payload == null || payload.ruleId() == null || payload.productId() == null) {
            throw new BizException("厂商字段映射规则审批载荷缺少规则标识");
        }
        return payload;
    }

    private static String writePayload(Object payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BizException("厂商字段映射规则审批载荷序列化失败");
        }
    }

    private static String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record RuleApprovalPayload(Long ruleId,
                                      Long productId,
                                      Integer expectedVersionNo,
                                      String rawIdentifier,
                                      String logicalChannelCode,
                                      String targetNormativeIdentifier,
                                      String scopeType,
                                      String protocolCode,
                                      String scenarioCode,
                                      String deviceFamily,
                                      String relationConditionJson,
                                      String normalizationRuleJson,
                                      String submitReason,
                                      RuleApprovalExecution execution) {
    }

    public record RuleApprovalExecution(Long approvalOrderId,
                                        Integer publishedVersionNo,
                                        String lifecycleStatus) {
    }
}
