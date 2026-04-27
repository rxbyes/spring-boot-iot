package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.vo.RuleDefinitionBatchAddResultVO;
import com.ghlzm.iot.common.response.PageResult;

import java.util.List;

/**
 * 阈值规则配置Service
 */
public interface RuleDefinitionService extends IService<RuleDefinition> {
      /**
       * 获取规则列表
       */
      List<RuleDefinition> getRuleList(String ruleName, String metricIdentifier, String alarmLevel, Integer status,
                                       String ruleScope, Long productId, String productType);

      default List<RuleDefinition> getRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                               Integer status) {
            return getRuleList(ruleName, metricIdentifier, alarmLevel, status, null, null, null);
      }

      /**
       * 分页获取规则列表
       */
      PageResult<RuleDefinition> pageRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                              Integer status, String ruleScope, Long productId, String productType,
                                              Long pageNum, Long pageSize);

      default PageResult<RuleDefinition> pageRuleList(String ruleName, String metricIdentifier, String alarmLevel,
                                                      Integer status, Long pageNum, Long pageSize) {
            return pageRuleList(ruleName, metricIdentifier, alarmLevel, status, null, null, null, pageNum, pageSize);
      }

      /**
       * 新增规则
       */
      void addRule(RuleDefinition rule);

      RuleDefinitionBatchAddResultVO batchAddRules(List<RuleDefinition> rules);

      /**
       * 更新规则
       */
      void updateRule(RuleDefinition rule);

      /**
       * 删除规则
       */
      void deleteRule(Long id);
}
