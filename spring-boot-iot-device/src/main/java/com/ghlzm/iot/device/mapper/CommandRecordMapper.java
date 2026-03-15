package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.CommandRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指令记录 Mapper。
 */
@Mapper
public interface CommandRecordMapper extends BaseMapper<CommandRecord> {
}
