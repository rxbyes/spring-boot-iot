package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录Mapper
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
