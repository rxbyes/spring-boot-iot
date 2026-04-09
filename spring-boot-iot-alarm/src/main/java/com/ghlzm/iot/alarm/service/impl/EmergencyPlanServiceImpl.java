package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.service.EmergencyPlanService;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingSyncService;
import com.ghlzm.iot.common.response.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 应急预案Service实现类
 */
@Service
public class EmergencyPlanServiceImpl extends ServiceImpl<EmergencyPlanMapper, EmergencyPlan>
            implements EmergencyPlanService {

      private final RiskMetricActionBindingSyncService bindingSyncService;

      public EmergencyPlanServiceImpl(RiskMetricActionBindingSyncService bindingSyncService) {
            this.bindingSyncService = bindingSyncService;
      }

      @Override
      public PageResult<EmergencyPlan> pagePlanList(String planName, String alarmLevel, Integer status, Long pageNum, Long pageSize) {
            Page<EmergencyPlan> page = new Page<>(pageNum, pageSize);
            Page<EmergencyPlan> result = page(page, buildWrapper(planName, alarmLevel, status));
            return PageResult.of(result.getTotal(), pageNum, pageSize, result.getRecords());
      }

      @Override
      public List<EmergencyPlan> getPlanList(String planName, String alarmLevel, Integer status) {
            return list(buildWrapper(planName, alarmLevel, status));
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void addPlan(EmergencyPlan plan, Long operatorId) {
            String normalizedAlarmLevel = resolveAlarmLevel(plan);
            plan.setAlarmLevel(normalizedAlarmLevel);
            plan.setRiskLevel(normalizedAlarmLevel);
            plan.setDeleted(0);
            plan.setCreateBy(operatorId);
            plan.setUpdateBy(operatorId);
            save(plan);
            bindingSyncService.rebuildEmergencyPlanBindingsForPlan(plan, operatorId, "AUTO_INFERRED");
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updatePlan(EmergencyPlan plan, Long operatorId) {
            String normalizedAlarmLevel = resolveAlarmLevel(plan);
            plan.setAlarmLevel(normalizedAlarmLevel);
            plan.setRiskLevel(normalizedAlarmLevel);
            plan.setUpdateBy(operatorId);
            updateById(plan);
            bindingSyncService.rebuildEmergencyPlanBindingsForPlan(plan, operatorId, "AUTO_INFERRED");
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deletePlan(Long id, Long operatorId) {
            removeById(id);
            bindingSyncService.deactivateEmergencyPlanBindings(id, operatorId);
      }

      private LambdaQueryWrapper<EmergencyPlan> buildWrapper(String planName, String alarmLevel, Integer status) {
            LambdaQueryWrapper<EmergencyPlan> wrapper = new LambdaQueryWrapper<>();
            if (planName != null && !planName.isEmpty()) {
                  wrapper.eq(EmergencyPlan::getPlanName, planName);
            }
            if (StringUtils.hasText(alarmLevel)) {
                  wrapper.in(EmergencyPlan::getAlarmLevel, buildAlarmLevelQueryValues(alarmLevel));
            }
            if (status != null) {
                  wrapper.eq(EmergencyPlan::getStatus, status);
            }
            wrapper.eq(EmergencyPlan::getDeleted, 0);
            wrapper.orderByDesc(EmergencyPlan::getCreateTime);
            return wrapper;
      }

      private List<String> buildAlarmLevelQueryValues(String alarmLevel) {
            String normalizedAlarmLevel = normalizeAlarmLevel(alarmLevel);
            if (!StringUtils.hasText(normalizedAlarmLevel)) {
                  return List.of();
            }
            return switch (normalizedAlarmLevel) {
                  case "red" -> List.of("red", "critical");
                  case "orange" -> List.of("orange", "warning", "high");
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
                  case "warning", "high", "orange" -> "orange";
                  case "medium", "yellow" -> "yellow";
                  case "info", "low", "blue" -> "blue";
                  default -> alarmLevel.trim().toLowerCase(Locale.ROOT);
            };
      }

      private String resolveAlarmLevel(EmergencyPlan plan) {
            if (plan == null) {
                  return "";
            }
            if (StringUtils.hasText(plan.getAlarmLevel())) {
                  return normalizeAlarmLevel(plan.getAlarmLevel());
            }
            return normalizeAlarmLevel(plan.getRiskLevel());
      }
}
