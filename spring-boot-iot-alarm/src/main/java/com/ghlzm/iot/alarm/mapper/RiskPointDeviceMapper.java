package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险点与设备绑定Mapper
 */
@Mapper
public interface RiskPointDeviceMapper extends BaseMapper<RiskPointDevice> {
}
