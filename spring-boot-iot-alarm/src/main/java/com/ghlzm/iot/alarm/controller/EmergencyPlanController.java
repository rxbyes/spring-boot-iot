package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.service.EmergencyPlanService;
import com.ghlzm.iot.common.response.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应急预案Controller
 */
@RestController
@RequestMapping("/api/emergency-plan")
public class EmergencyPlanController {

      @Autowired
      private EmergencyPlanService emergencyPlanService;

      /**
       * 获取预案列表
       */
      @GetMapping("/list")
      public R<List<EmergencyPlan>> getPlanList(
                  @RequestParam(required = false) String planName,
                  @RequestParam(required = false) String riskLevel,
                  @RequestParam(required = false) Integer status) {
            List<EmergencyPlan> list = emergencyPlanService.getPlanList(planName, riskLevel, status);
            return R.ok(list);
      }

      /**
       * 获取预案详情
       */
      @GetMapping("/get/{id}")
      public R<EmergencyPlan> getPlanById(@PathVariable Long id) {
            EmergencyPlan plan = emergencyPlanService.getById(id);
            return R.ok(plan);
      }

      /**
       * 新增预案
       */
      @PostMapping("/add")
      public R<EmergencyPlan> addPlan(@RequestBody EmergencyPlan plan) {
            emergencyPlanService.addPlan(plan);
            return R.ok(plan);
      }

      /**
       * 更新预案
       */
      @PostMapping("/update")
      public R<EmergencyPlan> updatePlan(@RequestBody EmergencyPlan plan) {
            emergencyPlanService.updatePlan(plan);
            return R.ok(plan);
      }

      /**
       * 删除预案
       */
      @PostMapping("/delete/{id}")
      public R<Void> deletePlan(@PathVariable Long id) {
            emergencyPlanService.deletePlan(id);
            return R.ok();
      }
}
