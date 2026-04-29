package com.ghlzm.iot.system.observability;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 入口业务事件字典解析器。
 */
public final class HttpBusinessEventDictionaryResolver {

    private static final List<Rule> RULES = List.of(
            rule("POST", "^/api/auth/login$", "auth.login", "账号登录", "auth", "login", "user", 0),

            rule("POST", "^/api/device/product/([^/]+)/model-governance/compare$", "product.contract.compare",
                    "契约字段识别对比", "product_contract", "compare", "product", 1),
            rule("POST", "^/api/device/product/([^/]+)/model-governance/apply$", "product.contract.apply",
                    "契约字段提交发布", "product_contract", "apply", "product", 1),
            rule("POST", "^/api/device/product/contract-release-batches/([^/]+)/rollback$", "product.contract.rollback",
                    "正式合同批次回滚", "product_contract", "rollback", "contract_release_batch", 1),
            rule("POST", "^/api/device/product/([^/]+)/models$", "product.contract.field_create",
                    "契约字段新增", "product_contract", "field_create", "product", 1),
            rule("PUT", "^/api/device/product/([^/]+)/models/([^/]+)$", "product.contract.field_update",
                    "契约字段更新", "product_contract", "field_update", "product_model", 2),
            rule("DELETE", "^/api/device/product/([^/]+)/models/([^/]+)$", "product.contract.field_delete",
                    "契约字段删除", "product_contract", "field_delete", "product_model", 2),

            rule("POST", "^/api/device/product/([^/]+)/vendor-mapping-rules$", "product.mapping_rule.create",
                    "映射规则创建", "product_mapping_rule", "create", "product", 1),
            rule("PUT", "^/api/device/product/([^/]+)/vendor-mapping-rules/([^/]+)$", "product.mapping_rule.update",
                    "映射规则更新", "product_mapping_rule", "update", "vendor_mapping_rule", 2),
            rule("POST", "^/api/device/product/([^/]+)/vendor-mapping-rules/batch-status$", "product.mapping_rule.batch_status",
                    "映射规则批量调状态", "product_mapping_rule", "batch_status", "product", 1),
            rule("POST", "^/api/device/product/([^/]+)/vendor-mapping-rules/([^/]+)/submit-publish$", "product.mapping_rule.publish_submit",
                    "映射规则提交发布", "product_mapping_rule", "publish_submit", "vendor_mapping_rule", 2),
            rule("POST", "^/api/device/product/([^/]+)/vendor-mapping-rules/([^/]+)/submit-rollback$", "product.mapping_rule.rollback_submit",
                    "映射规则提交回滚", "product_mapping_rule", "rollback_submit", "vendor_mapping_rule", 2),
            rule("POST", "^/api/device/product/([^/]+)/vendor-mapping-rules/preview-hit$", "product.mapping_rule.preview_hit",
                    "映射规则命中预览", "product_mapping_rule", "preview_hit", "product", 1),
            rule("POST", "^/api/device/product/([^/]+)/vendor-mapping-rules/replay$", "product.mapping_rule.replay",
                    "映射规则回放", "product_mapping_rule", "replay", "product", 1),
            rule("POST", "^/api/device/product/([^/]+)/runtime-display-rules$", "product.runtime_display_rule.create",
                    "运行态显示规则创建", "product_mapping_rule", "display_rule_create", "product", 1),
            rule("PUT", "^/api/device/product/([^/]+)/runtime-display-rules/([^/]+)$", "product.runtime_display_rule.update",
                    "运行态显示规则更新", "product_mapping_rule", "display_rule_update", "runtime_display_rule", 2),

            rule("POST", "^/api/governance/protocol/families$", "protocol.family.save",
                    "协议族定义维护", "protocol_governance", "save_family", "protocol_family", 0),
            rule("POST", "^/api/governance/protocol/families/([^/]+)/submit-publish$", "protocol.family.publish_submit",
                    "协议族提交发布", "protocol_governance", "publish_family", "protocol_family", 1),
            rule("POST", "^/api/governance/protocol/families/batch-submit-publish$", "protocol.family.batch_publish_submit",
                    "协议族批量提交发布", "protocol_governance", "batch_publish_family", "protocol_family", 0),
            rule("POST", "^/api/governance/protocol/families/([^/]+)/submit-rollback$", "protocol.family.rollback_submit",
                    "协议族提交回滚", "protocol_governance", "rollback_family", "protocol_family", 1),
            rule("POST", "^/api/governance/protocol/families/batch-submit-rollback$", "protocol.family.batch_rollback_submit",
                    "协议族批量提交回滚", "protocol_governance", "batch_rollback_family", "protocol_family", 0),
            rule("POST", "^/api/governance/protocol/decrypt-profiles$", "protocol.decrypt_profile.save",
                    "协议解密档案维护", "protocol_governance", "save_decrypt_profile", "protocol_decrypt_profile", 0),
            rule("POST", "^/api/governance/protocol/decrypt-profiles/([^/]+)/submit-publish$", "protocol.decrypt_profile.publish_submit",
                    "协议解密档案提交发布", "protocol_governance", "publish_decrypt_profile", "protocol_decrypt_profile", 1),
            rule("POST", "^/api/governance/protocol/decrypt-profiles/batch-submit-publish$", "protocol.decrypt_profile.batch_publish_submit",
                    "协议解密档案批量提交发布", "protocol_governance", "batch_publish_decrypt_profile", "protocol_decrypt_profile", 0),
            rule("POST", "^/api/governance/protocol/decrypt-profiles/([^/]+)/submit-rollback$", "protocol.decrypt_profile.rollback_submit",
                    "协议解密档案提交回滚", "protocol_governance", "rollback_decrypt_profile", "protocol_decrypt_profile", 1),
            rule("POST", "^/api/governance/protocol/decrypt-profiles/batch-submit-rollback$", "protocol.decrypt_profile.batch_rollback_submit",
                    "协议解密档案批量提交回滚", "protocol_governance", "batch_rollback_decrypt_profile", "protocol_decrypt_profile", 0),
            rule("POST", "^/api/governance/protocol/decrypt-profiles/preview$", "protocol.decrypt_profile.preview",
                    "协议解密试算", "protocol_governance", "preview_decrypt_profile", "protocol_decrypt_profile", 0),
            rule("POST", "^/api/governance/protocol/decrypt-profiles/replay$", "protocol.decrypt_profile.replay",
                    "协议解密回放", "protocol_governance", "replay_decrypt_profile", "protocol_decrypt_profile", 0),
            rule("POST", "^/api/governance/protocol/templates$", "protocol.template.save",
                    "协议模板维护", "protocol_governance", "save_template", "protocol_template", 0),
            rule("POST", "^/api/governance/protocol/templates/([^/]+)/publish$", "protocol.template.publish",
                    "协议模板发布", "protocol_governance", "publish_template", "protocol_template", 1),
            rule("POST", "^/api/governance/protocol/templates/replay$", "protocol.template.replay",
                    "协议模板回放", "protocol_governance", "replay_template", "protocol_template", 0),

            rule("POST", "^/api/device/([^/]+)/capabilities/([^/]+)/execute$", "device.command.issue",
                    "设备命令下发", "device_operation", "issue_command", "device", 1),
            rule("POST", "^/api/device/([^/]+)/secret-rotate$", "device.secret.rotate",
                    "设备密钥轮换", "device_operation", "rotate_secret", "device", 1),
            rule("POST", "^/api/device/onboarding/batch-activate$", "device.onboarding.batch_activate",
                    "接入建议批量转正式设备", "device_onboarding", "batch_activate", "device_onboarding_suggestion", 0),

            rule("POST", "^/api/device/onboarding/cases/([^/]+)/start-acceptance$", "acceptance.onboarding_case.start",
                    "接入案例触发验收", "acceptance", "start_onboarding_case", "onboarding_case", 1),
            rule("POST", "^/api/device/onboarding/cases/batch-start-acceptance$", "acceptance.onboarding_case.batch_start",
                    "接入案例批量触发验收", "acceptance", "batch_start_onboarding_case", "onboarding_case", 0),
            rule("POST", "^/api/report/business-acceptance/runs$", "acceptance.business_run.start",
                    "业务验收运行启动", "acceptance", "start_business_run", "business_acceptance_run", 0),
            rule("GET", "^/api/report/business-acceptance/results/([^/]+)$", "acceptance.business_result.view",
                    "业务验收结果查看", "acceptance", "view_business_result", "business_acceptance_run", 1),
            rule("GET", "^/api/report/automation-results/([^/]+)$", "acceptance.automation_result.view",
                    "自动化验收结果查看", "acceptance", "view_automation_result", "automation_run", 1)
    );

    public BusinessEventDefinition resolve(String method,
                                           String requestUri,
                                           String requestPattern,
                                           String operationModule,
                                           String fallbackAction) {
        String normalizedMethod = normalizeMethod(method);
        String normalizedUri = StringUtils.hasText(requestUri) ? requestUri : "";
        for (Rule rule : RULES) {
            if (!rule.matchesMethod(normalizedMethod)) {
                continue;
            }
            Matcher matcher = rule.pathPattern().matcher(normalizedUri);
            if (!matcher.matches()) {
                continue;
            }
            Map<String, Object> metadata = buildMatchedMetadata(rule, matcher, requestPattern);
            return new BusinessEventDefinition(
                    rule.eventCode(),
                    rule.eventName(),
                    rule.domainCode(),
                    rule.actionCode(),
                    rule.objectType(),
                    resolveObjectId(rule, matcher, requestPattern),
                    true,
                    metadata
            );
        }
        return fallback(normalizedMethod, requestUri, requestPattern, operationModule, fallbackAction);
    }

    private BusinessEventDefinition fallback(String method,
                                             String requestUri,
                                             String requestPattern,
                                             String operationModule,
                                             String fallbackAction) {
        String domain = StringUtils.hasText(operationModule) ? operationModule : "platform";
        String action = StringUtils.hasText(fallbackAction) ? fallbackAction : "unknown";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dictionaryMatched", false);
        metadata.put("requestPattern", StringUtils.hasText(requestPattern) ? requestPattern : requestUri);
        metadata.put("requestMethod", method);
        return new BusinessEventDefinition(
                domain + "." + action,
                domain + " " + action,
                domain,
                action,
                domain,
                StringUtils.hasText(requestPattern) ? requestPattern : requestUri,
                false,
                metadata
        );
    }

    private Map<String, Object> buildMatchedMetadata(Rule rule, Matcher matcher, String requestPattern) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dictionaryMatched", true);
        metadata.put("dictionaryRule", rule.eventCode());
        if (StringUtils.hasText(requestPattern)) {
            metadata.put("requestPattern", requestPattern);
        }
        for (int index = 1; index <= matcher.groupCount(); index++) {
            metadata.put("pathGroup" + index, matcher.group(index));
        }
        return metadata;
    }

    private String resolveObjectId(Rule rule, Matcher matcher, String requestPattern) {
        int group = rule.objectIdGroup();
        if (group > 0 && group <= matcher.groupCount()) {
            return matcher.group(group);
        }
        return requestPattern;
    }

    private String normalizeMethod(String method) {
        return StringUtils.hasText(method) ? method.toUpperCase(Locale.ROOT) : "";
    }

    private static Rule rule(String method,
                             String pathRegex,
                             String eventCode,
                             String eventName,
                             String domainCode,
                             String actionCode,
                             String objectType,
                             int objectIdGroup) {
        return new Rule(
                method.toUpperCase(Locale.ROOT),
                Pattern.compile(pathRegex),
                eventCode,
                eventName,
                domainCode,
                actionCode,
                objectType,
                objectIdGroup
        );
    }

    private record Rule(
            String method,
            Pattern pathPattern,
            String eventCode,
            String eventName,
            String domainCode,
            String actionCode,
            String objectType,
            int objectIdGroup
    ) {
        private boolean matchesMethod(String requestMethod) {
            return "*".equals(method) || method.equals(requestMethod);
        }
    }
}
