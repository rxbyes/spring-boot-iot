package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应急预案Mapper
 */
@Mapper
public interface EmergencyPlanMapper extends BaseMapper<EmergencyPlan> {
}
