package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.service.RuleDefinitionService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 阈值规则配置Controller
 */
@RestController
@RequestMapping("/api/rule-definition")
public class RuleDefinitionController {

      @Autowired
      private RuleDefinitionService ruleDefinitionService;

      /**
       * 获取规则列表
       */
      @GetMapping("/list")
      public R<List<RuleDefinition>> getRuleList(
                  @RequestParam(required = false) String metricIdentifier,
                  @RequestParam(required = false) String alarmLevel,
                  @RequestParam(required = false) Integer status) {
            List<RuleDefinition> list = ruleDefinitionService.getRuleList(metricIdentifier, alarmLevel, status);
            return R.ok(list);
      }

      /**
       * 分页获取规则列表
       */
      @GetMapping("/page")
      public R<PageResult<RuleDefinition>> pageRuleList(
                  @RequestParam(required = false) String metricIdentifier,
                  @RequestParam(required = false) String alarmLevel,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize) {
            PageResult<RuleDefinition> page = ruleDefinitionService.pageRuleList(metricIdentifier, alarmLevel, status, pageNum, pageSize);
            return R.ok(page);
      }

      /**
       * 获取规则详情
       */
      @GetMapping("/get/{id}")
      public R<RuleDefinition> getRuleById(@PathVariable Long id) {
            RuleDefinition rule = ruleDefinitionService.getById(id);
            return R.ok(rule);
      }

      /**
       * 新增规则
       */
      @PostMapping("/add")
      public R<RuleDefinition> addRule(@RequestBody RuleDefinition rule) {
            ruleDefinitionService.addRule(rule);
            return R.ok(rule);
      }

      /**
       * 更新规则
       */
      @PostMapping("/update")
      public R<RuleDefinition> updateRule(@RequestBody RuleDefinition rule) {
            ruleDefinitionService.updateRule(rule);
            return R.ok(rule);
      }

      /**
       * 删除规则
       */
      @PostMapping("/delete/{id}")
      public R<Void> deleteRule(@PathVariable Long id) {
            ruleDefinitionService.deleteRule(id);
            return R.ok();
      }
}
