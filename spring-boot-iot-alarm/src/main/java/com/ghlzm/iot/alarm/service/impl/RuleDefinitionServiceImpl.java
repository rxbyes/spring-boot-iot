package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 阈值规则配置Service实现类
 */
@Service
public class RuleDefinitionServiceImpl extends ServiceImpl<RuleDefinitionMapper, RuleDefinition>
            implements RuleDefinitionService {

      @Override
      public List<RuleDefinition> getRuleList(String metricIdentifier, String alarmLevel, Integer status) {
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
            return list(wrapper);
      }

      @Override
      public void addRule(RuleDefinition rule) {
            rule.setDeleted(0);
            save(rule);
      }

      @Override
      public void updateRule(RuleDefinition rule) {
            updateById(rule);
      }

      @Override
      public void deleteRule(Long id) {
            removeById(id);
      }
}
