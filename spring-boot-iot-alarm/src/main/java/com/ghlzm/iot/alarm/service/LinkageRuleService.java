package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.LinkageRule;

import java.util.List;

/**
 * 联动规则Service
 */
public interface LinkageRuleService extends IService<LinkageRule> {
      /**
       * 获取规则列表
       */
      List<LinkageRule> getRuleList(String ruleName, Integer status);

      /**
       * 新增规则
       */
      void addRule(LinkageRule rule);

      /**
       * 更新规则
       */
      void updateRule(LinkageRule rule);

      /**
       * 删除规则
       */
      void deleteRule(Long id);
}
