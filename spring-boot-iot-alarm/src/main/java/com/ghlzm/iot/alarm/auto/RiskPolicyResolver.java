package com.ghlzm.iot.alarm.auto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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

    public RiskPolicyResolver(RuleDefinitionMapper ruleDefinitionMapper, IotProperties iotProperties) {
        this.ruleDefinitionMapper = ruleDefinitionMapper;
        this.iotProperties = iotProperties;
    }

    public RiskPolicyDecision resolve(Long tenantId, RiskPointDevice binding, BigDecimal absoluteValue) {
        if (binding != null && StringUtils.hasText(binding.getMetricIdentifier())) {
            for (RuleDefinition rule : listEnabledRules(tenantId, binding.getMetricIdentifier())) {
                if (matches(rule == null ? null : rule.getExpression(), absoluteValue)) {
                    return RiskPolicyDecision.fromRule(rule);
                }
            }
        }
        IotProperties.Alarm.AutoClosure config = iotProperties == null || iotProperties.getAlarm() == null
                ? null
                : iotProperties.getAlarm().getAutoClosure();
        return RiskPolicyDecision.fromAutoClosure(absoluteValue, config);
    }

    public static boolean isExecutableExpression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return false;
        }
        return SIMPLE_EXPRESSION.matcher(expression).matches();
    }

    private List<RuleDefinition> listEnabledRules(Long tenantId, String metricIdentifier) {
        if (!StringUtils.hasText(metricIdentifier)) {
            return List.of();
        }
        List<RuleDefinition> rules = ruleDefinitionMapper.selectList(
                new LambdaQueryWrapper<RuleDefinition>()
                        .eq(RuleDefinition::getDeleted, 0)
                        .eq(RuleDefinition::getStatus, 0)
                        .eq(RuleDefinition::getMetricIdentifier, metricIdentifier)
                        .eq(tenantId != null, RuleDefinition::getTenantId, tenantId)
                        .orderByDesc(RuleDefinition::getCreateTime)
        );
        return rules == null ? Collections.emptyList() : rules;
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
