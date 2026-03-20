package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.service.EmergencyPlanService;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应急预案Service实现类
 */
@Service
public class EmergencyPlanServiceImpl extends ServiceImpl<EmergencyPlanMapper, EmergencyPlan>
            implements EmergencyPlanService {

      @Override
      public PageResult<EmergencyPlan> pagePlanList(String planName, String riskLevel, Integer status, Long pageNum, Long pageSize) {
            Page<EmergencyPlan> page = new Page<>(pageNum, pageSize);
            Page<EmergencyPlan> result = page(page, buildWrapper(planName, riskLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
      }

      @Override
      public List<EmergencyPlan> getPlanList(String planName, String riskLevel, Integer status) {
            return list(buildWrapper(planName, riskLevel, status));
      }

      @Override
      public void addPlan(EmergencyPlan plan) {
            plan.setDeleted(0);
            save(plan);
      }

      @Override
      public void updatePlan(EmergencyPlan plan) {
            updateById(plan);
      }

      @Override
      public void deletePlan(Long id) {
            removeById(id);
      }

      private LambdaQueryWrapper<EmergencyPlan> buildWrapper(String planName, String riskLevel, Integer status) {
            LambdaQueryWrapper<EmergencyPlan> wrapper = new LambdaQueryWrapper<>();
            if (planName != null && !planName.isEmpty()) {
                  wrapper.eq(EmergencyPlan::getPlanName, planName);
            }
            if (riskLevel != null && !riskLevel.isEmpty()) {
                  wrapper.eq(EmergencyPlan::getRiskLevel, riskLevel);
            }
            if (status != null) {
                  wrapper.eq(EmergencyPlan::getStatus, status);
            }
            wrapper.eq(EmergencyPlan::getDeleted, 0);
            wrapper.orderByDesc(EmergencyPlan::getCreateTime);
            return wrapper;
      }
}
