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
import com.ghlzm.iot.alarm.vo.RuleDefinitionBatchAddResultVO;
import com.ghlzm.iot.alarm.vo.RuleDefinitionEffectivePreviewVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
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
      public PageResult<RuleDefinition> pageRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                                     Integer status, String ruleScope, Long productId,
                                                     String productType,
                                                     Long pageNum, Long pageSize) {
            return pageRuleList(ruleName, metricIdentifier, alarmLevel, status, ruleScope, null, productId,
                    productType, pageNum, pageSize);
      }

      @Override
      public PageResult<RuleDefinition> pageRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                                     Integer status, String ruleScope, String scopeView,
                                                     Long productId, String productType,
                                                     Long pageNum, Long pageSize) {
            Page<RuleDefinition> page = new Page<>(pageNum, pageSize);
            Page<RuleDefinition> result = page(page, buildWrapper(ruleName, metricIdentifier, alarmLevel, status,
                    ruleScope, scopeView, productId, productType));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
      }

      @Override
      public List<RuleDefinition> getRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                              Integer status, String ruleScope, Long productId, String productType) {
            return getRuleList(ruleName, metricIdentifier, alarmLevel, status, ruleScope, null, productId, productType);
      }

      @Override
      public List<RuleDefinition> getRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                              Integer status, String ruleScope, String scopeView, Long productId,
                                              String productType) {
            return list(buildWrapper(ruleName, metricIdentifier, alarmLevel, status, ruleScope, scopeView, productId,
                    productType));
      }

      @Override
      public void addRule(RuleDefinition rule) {
            validateExecutableRule(rule);
            rule.setAlarmLevel(normalizeAlarmLevel(rule.getAlarmLevel()));
            ensureNoDuplicateActiveRule(rule);
            rule.setDeleted(0);
            save(rule);
      }

      @Override
      public RuleDefinitionBatchAddResultVO batchAddRules(List<RuleDefinition> rules) {
            RuleDefinitionBatchAddResultVO result = new RuleDefinitionBatchAddResultVO();
            if (rules == null || rules.isEmpty()) {
                  return result;
            }
            if (rules.size() > 100) {
                  throw new BizException("Batch add rule definitions cannot exceed 100 items");
            }
            result.setTotalCount(rules.size());
            for (int index = 0; index < rules.size(); index += 1) {
                  RuleDefinition rule = rules.get(index);
                  RuleDefinitionBatchAddResultVO.Item item = new RuleDefinitionBatchAddResultVO.Item();
                  item.setIndex(index);
                  item.setRuleName(rule == null ? null : rule.getRuleName());
                  item.setMetricIdentifier(rule == null ? null : rule.getMetricIdentifier());
                  try {
                        addRule(rule);
                        item.setRuleId(rule.getId());
                        item.setRuleName(rule.getRuleName());
                        item.setMetricIdentifier(rule.getMetricIdentifier());
                        item.setSuccess(true);
                        item.setMessage("OK");
                        result.setSuccessCount(result.getSuccessCount() + 1);
                  } catch (RuntimeException error) {
                        item.setSuccess(false);
                        item.setMessage(error.getMessage());
                        result.setFailedCount(result.getFailedCount() + 1);
                  }
                  result.getItems().add(item);
            }
            return result;
      }

      @Override
      public RuleDefinitionEffectivePreviewVO previewEffectiveRule(Long tenantId, Long riskMetricId,
                                                                   String metricIdentifier, Long productId,
                                                                   String productType, Long deviceId,
                                                                   Long riskPointDeviceId) {
            String normalizedMetricIdentifier = normalizeText(metricIdentifier);
            String normalizedProductType = StringUtils.hasText(productType)
                    ? productType.trim().toUpperCase(Locale.ROOT)
                    : null;
            if (riskMetricId == null && !StringUtils.hasText(normalizedMetricIdentifier)) {
                  throw new BizException("请提供风险指标ID或测点标识后再预览生效策略");
            }

            RuleDefinitionEffectivePreviewVO preview = new RuleDefinitionEffectivePreviewVO();
            preview.setTenantId(tenantId);
            preview.setRiskMetricId(riskMetricId);
            preview.setMetricIdentifier(normalizedMetricIdentifier);
            preview.setProductId(productId);
            preview.setProductType(normalizedProductType);
            preview.setDeviceId(deviceId);
            preview.setRiskPointDeviceId(riskPointDeviceId);

            List<RuleDefinition> rules = list(buildEffectivePreviewWrapper(
                    tenantId,
                    riskMetricId,
                    normalizedMetricIdentifier
            ));
            RuleDefinition matchedRule = rules.stream()
                    .filter(rule -> matchesPreviewMetric(rule, riskMetricId, normalizedMetricIdentifier))
                    .filter(rule -> matchesPreviewScope(rule, productId, normalizedProductType, deviceId, riskPointDeviceId))
                    .max(previewRuleComparator())
                    .orElse(null);

            List<RuleDefinitionEffectivePreviewVO.Candidate> candidates = rules.stream()
                    .filter(rule -> matchesPreviewMetric(rule, riskMetricId, normalizedMetricIdentifier))
                    .sorted(previewRuleComparator().reversed())
                    .map(rule -> buildPreviewCandidate(
                            rule,
                            matchedRule,
                            productId,
                            normalizedProductType,
                            deviceId,
                            riskPointDeviceId
                    ))
                    .toList();
            preview.setCandidates(candidates);
            if (matchedRule == null) {
                  preview.setHasMatchedRule(false);
                  preview.setDecision("当前上下文未命中启用阈值策略，将继续按运行时兜底策略处理。");
                  return preview;
            }
            preview.setHasMatchedRule(true);
            preview.setMatchedRule(matchedRule);
            preview.setMatchedScope(normalizeScope(matchedRule.getRuleScope()));
            preview.setMatchedScopeText(formatRuleScope(matchedRule.getRuleScope()));
            preview.setDecision("最终生效策略：" + matchedRule.getRuleName() + "（"
                    + formatRuleScope(matchedRule.getRuleScope()) + "），表达式："
                    + (StringUtils.hasText(matchedRule.getExpression()) ? matchedRule.getExpression() : "--"));
            return preview;
      }

      @Override
      public void updateRule(RuleDefinition rule) {
            validateExecutableRule(rule);
            rule.setAlarmLevel(normalizeAlarmLevel(rule.getAlarmLevel()));
            ensureNoDuplicateActiveRule(rule);
            updateById(rule);
      }

      @Override
      public void deleteRule(Long id) {
            removeById(id);
      }

      private LambdaQueryWrapper<RuleDefinition> buildEffectivePreviewWrapper(Long tenantId, Long riskMetricId,
                                                                              String metricIdentifier) {
            LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<RuleDefinition>()
                    .eq(RuleDefinition::getDeleted, 0)
                    .eq(RuleDefinition::getStatus, 0)
                    .eq(tenantId != null, RuleDefinition::getTenantId, tenantId);
            wrapper.and(metric -> {
                  if (riskMetricId != null && StringUtils.hasText(metricIdentifier)) {
                        metric.eq(RuleDefinition::getRiskMetricId, riskMetricId)
                                .or()
                                .eq(RuleDefinition::getMetricIdentifier, metricIdentifier);
                        return;
                  }
                  if (riskMetricId != null) {
                        metric.eq(RuleDefinition::getRiskMetricId, riskMetricId);
                        return;
                  }
                  metric.eq(RuleDefinition::getMetricIdentifier, metricIdentifier);
            });
            wrapper.orderByDesc(RuleDefinition::getCreateTime);
            return wrapper;
      }

      private Comparator<RuleDefinition> previewRuleComparator() {
            return Comparator
                    .comparingInt((RuleDefinition rule) -> previewScopePriority(rule.getRuleScope()))
                    .thenComparingInt(rule -> com.ghlzm.iot.alarm.auto.RiskPolicyDecision.fromRule(rule).getPriority())
                    .thenComparing(RuleDefinition::getId, Comparator.nullsLast(Long::compareTo));
      }

      private RuleDefinitionEffectivePreviewVO.Candidate buildPreviewCandidate(
              RuleDefinition rule,
              RuleDefinition matchedRule,
              Long productId,
              String productType,
              Long deviceId,
              Long riskPointDeviceId
      ) {
            RuleDefinitionEffectivePreviewVO.Candidate candidate = new RuleDefinitionEffectivePreviewVO.Candidate();
            String scope = normalizeScope(rule.getRuleScope());
            boolean matchedContext = matchesPreviewScope(rule, productId, productType, deviceId, riskPointDeviceId);
            candidate.setRuleId(rule.getId());
            candidate.setRuleName(rule.getRuleName());
            candidate.setRuleScope(scope);
            candidate.setRuleScopeText(formatRuleScope(scope));
            candidate.setScopeTarget(formatPreviewScopeTarget(rule));
            candidate.setMetricIdentifier(rule.getMetricIdentifier());
            candidate.setMetricName(rule.getMetricName());
            candidate.setExpression(rule.getExpression());
            candidate.setAlarmLevel(normalizeAlarmLevel(rule.getAlarmLevel()));
            candidate.setStatus(rule.getStatus());
            candidate.setPriority(previewScopePriority(scope));
            candidate.setMatchedContext(matchedContext);
            candidate.setSelected(matchedRule != null && sameLong(rule.getId(), matchedRule.getId()));
            candidate.setReason(matchedContext ? "上下文匹配，可参与最终生效优先级比较" : buildPreviewMismatchReason(rule));
            return candidate;
      }

      private boolean matchesPreviewMetric(RuleDefinition rule, Long riskMetricId, String metricIdentifier) {
            if (rule == null) {
                  return false;
            }
            if (rule.getRiskMetricId() != null && riskMetricId != null && rule.getRiskMetricId().equals(riskMetricId)) {
                  return true;
            }
            return StringUtils.hasText(rule.getMetricIdentifier())
                    && StringUtils.hasText(metricIdentifier)
                    && rule.getMetricIdentifier().trim().equals(metricIdentifier.trim());
      }

      private boolean matchesPreviewScope(RuleDefinition rule, Long productId, String productType, Long deviceId,
                                          Long riskPointDeviceId) {
            String scope = normalizeScope(rule == null ? null : rule.getRuleScope());
            return switch (scope) {
                  case "BINDING" -> riskPointDeviceId != null && riskPointDeviceId.equals(rule.getRiskPointDeviceId());
                  case "DEVICE" -> deviceId != null && deviceId.equals(rule.getDeviceId());
                  case "PRODUCT" -> productId != null && productId.equals(rule.getProductId());
                  case "PRODUCT_TYPE" -> StringUtils.hasText(productType)
                          && StringUtils.hasText(rule.getProductType())
                          && productType.equalsIgnoreCase(rule.getProductType().trim());
                  default -> true;
            };
      }

      private int previewScopePriority(String scope) {
            return switch (normalizeScope(scope)) {
                  case "BINDING" -> 5;
                  case "DEVICE" -> 4;
                  case "PRODUCT" -> 3;
                  case "PRODUCT_TYPE" -> 2;
                  default -> 1;
            };
      }

      private String normalizeScope(String scope) {
            return StringUtils.hasText(scope) ? scope.trim().toUpperCase(Locale.ROOT) : "METRIC";
      }

      private String normalizeText(String text) {
            return StringUtils.hasText(text) ? text.trim() : null;
      }

      private String formatPreviewScopeTarget(RuleDefinition rule) {
            String scope = normalizeScope(rule.getRuleScope());
            return switch (scope) {
                  case "BINDING" -> rule.getRiskPointDeviceId() == null ? "--" : "绑定 " + rule.getRiskPointDeviceId();
                  case "DEVICE" -> rule.getDeviceId() == null ? "--" : "设备 " + rule.getDeviceId();
                  case "PRODUCT" -> rule.getProductId() == null ? "--" : "产品 " + rule.getProductId();
                  case "PRODUCT_TYPE" -> StringUtils.hasText(rule.getProductType()) ? rule.getProductType() : "--";
                  default -> "通用";
            };
      }

      private String buildPreviewMismatchReason(RuleDefinition rule) {
            return "当前预览上下文未命中" + formatRuleScope(rule.getRuleScope()) + "适用对象";
      }

      private LambdaQueryWrapper<RuleDefinition> buildWrapper(String ruleName, String metricIdentifier,
                                                              String alarmLevel, Integer status,
                                                              String ruleScope, String scopeView, Long productId,
                                                              String productType) {
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
            if (StringUtils.hasText(ruleScope)) {
                  wrapper.eq(RuleDefinition::getRuleScope, ruleScope.trim().toUpperCase(Locale.ROOT));
            } else if (StringUtils.hasText(scopeView)) {
                  applyScopeView(wrapper, scopeView);
            }
            if (productId != null) {
                  wrapper.eq(RuleDefinition::getProductId, productId);
            }
            if (StringUtils.hasText(productType)) {
                  wrapper.eq(RuleDefinition::getProductType, productType.trim().toUpperCase(Locale.ROOT));
            }
            wrapper.eq(RuleDefinition::getDeleted, 0);
            wrapper.orderByDesc(RuleDefinition::getCreateTime);
            return wrapper;
      }

      private void applyScopeView(LambdaQueryWrapper<RuleDefinition> wrapper, String scopeView) {
            String normalizedScopeView = scopeView.trim().toUpperCase(Locale.ROOT);
            switch (normalizedScopeView) {
                  case "BUSINESS" -> wrapper.in(RuleDefinition::getRuleScope, "PRODUCT", "DEVICE", "BINDING");
                  case "SYSTEM" -> wrapper.and(scope -> scope
                          .in(RuleDefinition::getRuleScope, "METRIC", "PRODUCT_TYPE")
                          .or()
                          .isNull(RuleDefinition::getRuleScope));
                  case "ALL" -> {
                        // no scope restriction
                  }
                  default -> {
                        // keep backward-compatible behavior for unknown view values
                  }
            }
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
                        rule.setProductType(null);
                  }
                  case "PRODUCT_TYPE" -> {
                        if (!StringUtils.hasText(rule.getProductType())) {
                              throw new BizException("PRODUCT_TYPE threshold policy must provide productType");
                        }
                        rule.setProductType(rule.getProductType().trim().toUpperCase(Locale.ROOT));
                        rule.setProductId(null);
                  }
                  case "DEVICE" -> {
                        if (rule.getDeviceId() == null) {
                              throw new BizException("DEVICE threshold policy must provide deviceId");
                        }
                        rule.setProductType(null);
                  }
                  case "BINDING" -> {
                        if (rule.getRiskPointDeviceId() == null) {
                              throw new BizException("BINDING threshold policy must provide riskPointDeviceId");
                        }
                        rule.setProductType(null);
                  }
                  default -> throw new BizException("Unsupported threshold policy scope: " + scope);
            }
      }

      private void ensureNoDuplicateActiveRule(RuleDefinition rule) {
            if (!Integer.valueOf(0).equals(rule.getStatus()) || isSamePolicyIdentity(rule, getExistingRule(rule))) {
                  return;
            }
            LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<RuleDefinition>()
                    .eq(RuleDefinition::getDeleted, 0)
                    .eq(RuleDefinition::getStatus, 0)
                    .eq(StringUtils.hasText(rule.getRuleScope()), RuleDefinition::getRuleScope, rule.getRuleScope())
                    .eq(rule.getTenantId() != null, RuleDefinition::getTenantId, rule.getTenantId())
                    .eq(StringUtils.hasText(rule.getMetricIdentifier()), RuleDefinition::getMetricIdentifier,
                            rule.getMetricIdentifier())
                    .ne(rule.getId() != null, RuleDefinition::getId, rule.getId());
            switch (rule.getRuleScope()) {
                  case "PRODUCT_TYPE" -> wrapper.eq(RuleDefinition::getProductType, rule.getProductType());
                  case "PRODUCT" -> wrapper.eq(RuleDefinition::getProductId, rule.getProductId());
                  case "DEVICE" -> wrapper.eq(RuleDefinition::getDeviceId, rule.getDeviceId());
                  case "BINDING" -> wrapper.eq(RuleDefinition::getRiskPointDeviceId, rule.getRiskPointDeviceId());
                  default -> {
                  }
            }
            if (count(wrapper) > 0) {
                  throw new BizException("已存在相同范围和测点的启用阈值策略，请先停用或修改原策略后再保存。范围："
                          + formatRuleScope(rule.getRuleScope()) + "，测点：" + rule.getMetricIdentifier());
            }
      }

      private RuleDefinition getExistingRule(RuleDefinition rule) {
            if (rule == null || rule.getId() == null) {
                  return null;
            }
            return getById(rule.getId());
      }

      private boolean isSamePolicyIdentity(RuleDefinition rule, RuleDefinition existing) {
            if (rule == null || existing == null) {
                  return false;
            }
            return sameText(rule.getRuleScope(), existing.getRuleScope())
                    && sameInteger(rule.getStatus(), existing.getStatus())
                    && sameText(rule.getMetricIdentifier(), existing.getMetricIdentifier())
                    && sameLong(rule.getTenantId(), existing.getTenantId())
                    && sameText(rule.getProductType(), existing.getProductType())
                    && sameLong(rule.getProductId(), existing.getProductId())
                    && sameLong(rule.getDeviceId(), existing.getDeviceId())
                    && sameLong(rule.getRiskPointDeviceId(), existing.getRiskPointDeviceId());
      }

      private boolean sameLong(Long left, Long right) {
            return left == null ? right == null : left.equals(right);
      }

      private boolean sameInteger(Integer left, Integer right) {
            return left == null ? right == null : left.equals(right);
      }

      private boolean sameText(String left, String right) {
            String normalizedLeft = StringUtils.hasText(left) ? left.trim().toUpperCase(Locale.ROOT) : "";
            String normalizedRight = StringUtils.hasText(right) ? right.trim().toUpperCase(Locale.ROOT) : "";
            return normalizedLeft.equals(normalizedRight);
      }

      private String formatRuleScope(String scope) {
            if (!StringUtils.hasText(scope)) {
                  return "测点通用";
            }
            return switch (scope.trim().toUpperCase(Locale.ROOT)) {
                  case "PRODUCT" -> "产品默认";
                  case "DEVICE" -> "设备个性";
                  case "BINDING" -> "绑定个性";
                  case "PRODUCT_TYPE" -> "产品类型模板";
                  case "METRIC" -> "测点通用";
                  default -> scope.trim();
            };
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
