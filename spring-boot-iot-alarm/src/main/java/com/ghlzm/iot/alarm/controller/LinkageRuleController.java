package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.service.LinkageRuleService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联动规则Controller
 */
@RestController
@RequestMapping("/api/linkage-rule")
public class LinkageRuleController {

      @Autowired
      private LinkageRuleService linkageRuleService;

      /**
       * 获取规则列表
       */
      @GetMapping("/list")
      public R<List<LinkageRule>> getRuleList(
                  @RequestParam(required = false) String ruleName,
                  @RequestParam(required = false) Integer status) {
            List<LinkageRule> list = linkageRuleService.getRuleList(ruleName, status);
            return R.ok(list);
      }

      /**
       * 分页获取规则列表
       */
      @GetMapping("/page")
      public R<PageResult<LinkageRule>> pageRuleList(
                  @RequestParam(required = false) String ruleName,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(defaultValue = "1") Long pageNum,
                  @RequestParam(defaultValue = "10") Long pageSize) {
            PageResult<LinkageRule> page = linkageRuleService.pageRuleList(ruleName, status, pageNum, pageSize);
            return R.ok(page);
      }

      /**
       * 获取规则详情
       */
      @GetMapping("/get/{id}")
      public R<LinkageRule> getRuleById(@PathVariable Long id) {
            LinkageRule rule = linkageRuleService.getById(id);
            return R.ok(rule);
      }

      /**
       * 新增规则
       */
      @PostMapping("/add")
      public R<LinkageRule> addRule(@RequestBody LinkageRule rule) {
            linkageRuleService.addRule(rule);
            return R.ok(rule);
      }

      /**
       * 更新规则
       */
      @PostMapping("/update")
      public R<LinkageRule> updateRule(@RequestBody LinkageRule rule) {
            linkageRuleService.updateRule(rule);
            return R.ok(rule);
      }

      /**
       * 删除规则
       */
      @PostMapping("/delete/{id}")
      public R<Void> deleteRule(@PathVariable Long id) {
            linkageRuleService.deleteRule(id);
            return R.ok();
      }
}
