package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.auto.RiskPolicyResolver;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 阈值规则配置Service实现类
 */
@Service
public class RuleDefinitionServiceImpl extends ServiceImpl<RuleDefinitionMapper, RuleDefinition>
            implements RuleDefinitionService {

      private final RiskMetricCatalogService riskMetricCatalogService;

      public RuleDefinitionServiceImpl(RiskMetricCatalogService riskMetricCatalogService) {
            this.riskMetricCatalogService = riskMetricCatalogService;
      }

      @Override
      public PageResult<RuleDefinition> pageRuleList(String ruleName, String metricIdentifier, String alarmLevel, Integer status, Long pageNum, Long pageSize) {
            Page<RuleDefinition> page = new Page<>(pageNum, pageSize);
            Page<RuleDefinition> result = page(page, buildWrapper(ruleName, metricIdentifier, alarmLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
      }

      @Override
      public List<RuleDefinition> getRuleList(String ruleName, String metricIdentifier, String alarmLevel, Integer status) {
            return list(buildWrapper(ruleName, metricIdentifier, alarmLevel, status));
      }

      @Override
      public void addRule(RuleDefinition rule) {
            validateExecutableRule(rule);
            rule.setAlarmLevel(normalizeAlarmLevel(rule.getAlarmLevel()));
            rule.setDeleted(0);
            save(rule);
      }

      @Override
      public void updateRule(RuleDefinition rule) {
            validateExecutableRule(rule);
            rule.setAlarmLevel(normalizeAlarmLevel(rule.getAlarmLevel()));
            updateById(rule);
      }

      @Override
      public void deleteRule(Long id) {
            removeById(id);
      }

      private LambdaQueryWrapper<RuleDefinition> buildWrapper(String ruleName, String metricIdentifier, String alarmLevel, Integer status) {
            LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(ruleName)) {
                  wrapper.like(RuleDefinition::getRuleName, ruleName.trim());
            }
            if (metricIdentifier != null && !metricIdentifier.isEmpty()) {
                  wrapper.eq(RuleDefinition::getMetricIdentifier, metricIdentifier);
            }
            if (StringUtils.hasText(alarmLevel)) {
                  wrapper.in(RuleDefinition::getAlarmLevel, buildAlarmLevelQueryValues(alarmLevel));
            }
            if (status != null) {
                  wrapper.eq(RuleDefinition::getStatus, status);
            }
            wrapper.eq(RuleDefinition::getDeleted, 0);
            wrapper.orderByDesc(RuleDefinition::getCreateTime);
            return wrapper;
      }

      private List<String> buildAlarmLevelQueryValues(String alarmLevel) {
            String normalizedAlarmLevel = normalizeAlarmLevel(alarmLevel);
            if (!StringUtils.hasText(normalizedAlarmLevel)) {
                  return List.of();
            }
            return switch (normalizedAlarmLevel) {
                  case "red" -> List.of("red", "critical");
                  case "orange" -> List.of("orange", "warning", "warn", "high");
                  case "yellow" -> List.of("yellow", "medium");
                  case "blue" -> List.of("blue", "info", "low");
                  default -> List.of(normalizedAlarmLevel);
            };
      }

      private String normalizeAlarmLevel(String alarmLevel) {
            if (!StringUtils.hasText(alarmLevel)) {
                  return "";
            }
            return switch (alarmLevel.trim().toLowerCase(Locale.ROOT)) {
                  case "critical", "red" -> "red";
                  case "warning", "warn", "high", "orange" -> "orange";
                  case "medium", "yellow" -> "yellow";
                  case "info", "low", "blue" -> "blue";
                  default -> alarmLevel.trim().toLowerCase(Locale.ROOT);
            };
      }

      private void validateExecutableRule(RuleDefinition rule) {
            if (rule == null) {
                  throw new BizException("阈值策略不能为空");
            }
            normalizeAndValidateScope(rule);
            RiskMetricCatalog catalog = resolveRiskMetricCatalog(rule);
            if (catalog != null) {
                  bindCatalogIdentity(rule, catalog);
            } else if (StringUtils.hasText(rule.getMetricIdentifier())) {
                  rule.setMetricIdentifier(rule.getMetricIdentifier().trim());
            } else {
                  throw new BizException("阈值策略必须绑定目录指标或测点标识符");
            }
            if (Integer.valueOf(0).equals(rule.getStatus()) && !StringUtils.hasText(rule.getExpression())) {
                  throw new BizException("启用中的阈值策略必须提供可执行表达式");
            }
            if (StringUtils.hasText(rule.getExpression()) && !RiskPolicyResolver.isExecutableExpression(rule.getExpression())) {
                  throw new BizException("阈值策略表达式格式无效，仅支持 value >= 12 这类写法");
            }
      }

      private void normalizeAndValidateScope(RuleDefinition rule) {
            String scope = StringUtils.hasText(rule.getRuleScope())
                    ? rule.getRuleScope().trim().toUpperCase(Locale.ROOT)
                    : "METRIC";
            rule.setRuleScope(scope);
            switch (scope) {
                  case "METRIC" -> {
                  }
                  case "PRODUCT" -> {
                        if (rule.getProductId() == null) {
                              throw new BizException("PRODUCT threshold policy must provide productId");
                        }
                  }
                  case "DEVICE" -> {
                        if (rule.getDeviceId() == null) {
                              throw new BizException("DEVICE threshold policy must provide deviceId");
                        }
                  }
                  case "BINDING" -> {
                        if (rule.getRiskPointDeviceId() == null) {
                              throw new BizException("BINDING threshold policy must provide riskPointDeviceId");
                        }
                  }
                  default -> throw new BizException("Unsupported threshold policy scope: " + scope);
            }
      }

      private RiskMetricCatalog resolveRiskMetricCatalog(RuleDefinition rule) {
            if (rule.getRiskMetricId() == null) {
                  return null;
            }
            RiskMetricCatalog catalog = RiskMetricCatalogBindingSupport.resolveCatalog(
                    riskMetricCatalogService,
                    null,
                    rule.getRiskMetricId(),
                    rule.getMetricIdentifier()
            );
            if (catalog == null) {
                  throw new BizException("风险指标目录不存在或已停用: " + rule.getRiskMetricId());
            }
            return catalog;
      }

      private void bindCatalogIdentity(RuleDefinition rule, RiskMetricCatalog catalog) {
            RiskMetricCatalogBindingSupport.bindRuleDefinition(rule, catalog);
      }
}
