package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.DeviceSecretRotationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Device secret rotation log mapper.
 */
@Mapper
public interface DeviceSecretRotationLogMapper extends BaseMapper<DeviceSecretRotationLog> {
}
