package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.EventRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件记录Mapper
 */
@Mapper
public interface EventRecordMapper extends BaseMapper<EventRecord> {
}
