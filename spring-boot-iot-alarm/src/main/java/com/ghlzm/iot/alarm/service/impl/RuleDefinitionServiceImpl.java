package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.auto.RiskPolicyResolver;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 阈值规则配置Service实现类
 */
@Service
public class RuleDefinitionServiceImpl extends ServiceImpl<RuleDefinitionMapper, RuleDefinition>
            implements RuleDefinitionService {

      @Override
      public PageResult<RuleDefinition> pageRuleList(String metricIdentifier, String alarmLevel, Integer status, Long pageNum, Long pageSize) {
            Page<RuleDefinition> page = new Page<>(pageNum, pageSize);
            Page<RuleDefinition> result = page(page, buildWrapper(metricIdentifier, alarmLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
      }

      @Override
      public List<RuleDefinition> getRuleList(String metricIdentifier, String alarmLevel, Integer status) {
            return list(buildWrapper(metricIdentifier, alarmLevel, status));
      }

      @Override
      public void addRule(RuleDefinition rule) {
            validateExecutableRule(rule);
            rule.setDeleted(0);
            save(rule);
      }

      @Override
      public void updateRule(RuleDefinition rule) {
            validateExecutableRule(rule);
            updateById(rule);
      }

      @Override
      public void deleteRule(Long id) {
            removeById(id);
      }

      private LambdaQueryWrapper<RuleDefinition> buildWrapper(String metricIdentifier, String alarmLevel, Integer status) {
            LambdaQueryWrapper<RuleDefinition> wrapper = new LambdaQueryWrapper<>();
            if (metricIdentifier != null && !metricIdentifier.isEmpty()) {
                  wrapper.eq(RuleDefinition::getMetricIdentifier, metricIdentifier);
            }
            if (alarmLevel != null && !alarmLevel.isEmpty()) {
                  wrapper.eq(RuleDefinition::getAlarmLevel, alarmLevel);
            }
            if (status != null) {
                  wrapper.eq(RuleDefinition::getStatus, status);
            }
            wrapper.eq(RuleDefinition::getDeleted, 0);
            wrapper.orderByDesc(RuleDefinition::getCreateTime);
            return wrapper;
      }

      private void validateExecutableRule(RuleDefinition rule) {
            if (rule == null) {
                  throw new BizException("阈值策略不能为空");
            }
            if (Integer.valueOf(0).equals(rule.getStatus()) && !StringUtils.hasText(rule.getExpression())) {
                  throw new BizException("启用中的阈值策略必须提供可执行表达式");
            }
            if (StringUtils.hasText(rule.getExpression()) && !RiskPolicyResolver.isExecutableExpression(rule.getExpression())) {
                  throw new BizException("阈值策略表达式格式无效，仅支持 value >= 12 这类写法");
            }
      }
}
