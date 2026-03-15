package com.ghlzm.iot.alarm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;

import java.util.List;

/**
 * 应急预案Service
 */
public interface EmergencyPlanService extends IService<EmergencyPlan> {
      /**
       * 获取预案列表
       */
      List<EmergencyPlan> getPlanList(String planName, String riskLevel, Integer status);

      /**
       * 新增预案
       */
      void addPlan(EmergencyPlan plan);

      /**
       * 更新预案
       */
      void updatePlan(EmergencyPlan plan);

      /**
       * 删除预案
       */
      void deletePlan(Long id);
}
