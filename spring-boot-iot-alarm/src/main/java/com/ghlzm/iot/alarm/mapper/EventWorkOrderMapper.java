package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.EventWorkOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件工单Mapper
 */
@Mapper
public interface EventWorkOrderMapper extends BaseMapper<EventWorkOrder> {
}
