package com.ghlzm.iot.alarm.auto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 运行期风险策略解析器。
 */
@Component
public class RiskPolicyResolver {

    private static final Logger log = LoggerFactory.getLogger(RiskPolicyResolver.class);
    private static final Pattern SIMPLE_EXPRESSION = Pattern.compile(
            "^\\s*(?:value\\s*)?(>=|<=|>|<|==|=)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private final RuleDefinitionMapper ruleDefinitionMapper;
    private final IotProperties iotProperties;
    private final DeviceMapper deviceMapper;

    public RiskPolicyResolver(RuleDefinitionMapper ruleDefinitionMapper, IotProperties iotProperties) {
        this(ruleDefinitionMapper, iotProperties, null);
    }

    @Autowired
    public RiskPolicyResolver(RuleDefinitionMapper ruleDefinitionMapper, IotProperties iotProperties, DeviceMapper deviceMapper) {
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.iotProperties = iotProperties;
        this.deviceMapper = deviceMapper;
    }

    public RiskPolicyDecision resolve(Long tenantId, RiskPointDevice binding, BigDecimal absoluteValue) {
        if (binding != null && (StringUtils.hasText(binding.getMetricIdentifier()) || binding.getRiskMetricId() != null)) {
            Long productId = resolveProductId(binding);
            RuleDefinition matchedRule = listEnabledRules(tenantId, binding.getRiskMetricId(), binding.getMetricIdentifier()).stream()
                    .filter(rule -> matchesMetric(rule, binding))
                    .filter(rule -> matchesScope(rule, binding, productId))
                    .filter(rule -> matches(rule == null ? null : rule.getExpression(), absoluteValue))
                    .max(rulePriorityComparator(binding, productId))
                    .orElse(null);
            if (matchedRule != null) {
                return RiskPolicyDecision.fromRule(matchedRule);
            }
        }
        IotProperties.Alarm.AutoClosure config = iotProperties == null || iotProperties.getAlarm() == null
                ? null
                : iotProperties.getAlarm().getAutoClosure();
        return RiskPolicyDecision.fromAutoClosure(absoluteValue, config);
    }

    private Comparator<RuleDefinition> rulePriorityComparator(RiskPointDevice binding, Long productId) {
        return Comparator
                .comparingInt((RuleDefinition rule) -> scopePriority(rule, binding, productId))
                .thenComparingInt(rule -> RiskPolicyDecision.fromRule(rule).getPriority())
                .thenComparing(RuleDefinition::getId, Comparator.nullsLast(Long::compareTo));
    }

    public static boolean isExecutableExpression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return false;
        }
        return SIMPLE_EXPRESSION.matcher(expression).matches();
    }

    private List<RuleDefinition> listEnabledRules(Long tenantId, Long riskMetricId, String metricIdentifier) {
        if (riskMetricId == null && !StringUtils.hasText(metricIdentifier)) {
            return List.of();
        }
        List<RuleDefinition> rules = ruleDefinitionMapper.selectList(
                new LambdaQueryWrapper<RuleDefinition>()
                        .eq(RuleDefinition::getDeleted, 0)
                        .eq(RuleDefinition::getStatus, 0)
                        .and(wrapper -> {
                            if (riskMetricId != null && StringUtils.hasText(metricIdentifier)) {
                                wrapper.eq(RuleDefinition::getRiskMetricId, riskMetricId)
                                        .or()
                                        .eq(RuleDefinition::getMetricIdentifier, metricIdentifier);
                                return;
                            }
                            if (riskMetricId != null) {
                                wrapper.eq(RuleDefinition::getRiskMetricId, riskMetricId);
                                return;
                            }
                            wrapper.eq(RuleDefinition::getMetricIdentifier, metricIdentifier);
                        })
                        .eq(tenantId != null, RuleDefinition::getTenantId, tenantId)
                        .orderByDesc(RuleDefinition::getCreateTime)
        );
        return rules == null ? Collections.emptyList() : rules;
    }

    private Long resolveProductId(RiskPointDevice binding) {
        if (binding == null || binding.getDeviceId() == null || deviceMapper == null) {
            return null;
        }
        Device device = deviceMapper.selectById(binding.getDeviceId());
        return device == null ? null : device.getProductId();
    }

    private boolean matchesMetric(RuleDefinition rule, RiskPointDevice binding) {
        if (rule == null || binding == null) {
            return false;
        }
        if (rule.getRiskMetricId() != null && binding.getRiskMetricId() != null
                && rule.getRiskMetricId().equals(binding.getRiskMetricId())) {
            return true;
        }
        return StringUtils.hasText(rule.getMetricIdentifier())
                && StringUtils.hasText(binding.getMetricIdentifier())
                && rule.getMetricIdentifier().trim().equals(binding.getMetricIdentifier().trim());
    }

    private boolean matchesScope(RuleDefinition rule, RiskPointDevice binding, Long productId) {
        String scope = normalizeScope(rule == null ? null : rule.getRuleScope());
        return switch (scope) {
            case "BINDING" -> binding != null
                    && binding.getId() != null
                    && binding.getId().equals(rule.getRiskPointDeviceId());
            case "DEVICE" -> binding != null
                    && binding.getDeviceId() != null
                    && binding.getDeviceId().equals(rule.getDeviceId());
            case "PRODUCT" -> productId != null && productId.equals(rule.getProductId());
            default -> true;
        };
    }

    private int scopePriority(RuleDefinition rule, RiskPointDevice binding, Long productId) {
        String scope = normalizeScope(rule == null ? null : rule.getRuleScope());
        return switch (scope) {
            case "BINDING" -> 4;
            case "DEVICE" -> 3;
            case "PRODUCT" -> 2;
            default -> 1;
        };
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "METRIC";
        }
        return scope.trim().toUpperCase();
    }

    private boolean matches(String expression, BigDecimal absoluteValue) {
        if (absoluteValue == null || !isExecutableExpression(expression)) {
            return false;
        }
        Matcher matcher = SIMPLE_EXPRESSION.matcher(expression);
        if (!matcher.matches()) {
            return false;
        }
        String operator = matcher.group(1);
        BigDecimal threshold = new BigDecimal(matcher.group(2));
        return compare(absoluteValue.abs(), threshold, operator);
    }

    private boolean compare(BigDecimal currentValue, BigDecimal threshold, String rawOperator) {
        String operator = rawOperator == null ? "" : rawOperator.trim().toLowerCase();
        return switch (operator) {
            case ">" -> currentValue.compareTo(threshold) > 0;
            case ">=" -> currentValue.compareTo(threshold) >= 0;
            case "<" -> currentValue.compareTo(threshold) < 0;
            case "<=" -> currentValue.compareTo(threshold) <= 0;
            case "=", "==" -> currentValue.compareTo(threshold) == 0;
            default -> {
                log.warn("阈值策略比较符不受支持, operator={}", rawOperator);
                yield false;
            }
        };
    }
}
