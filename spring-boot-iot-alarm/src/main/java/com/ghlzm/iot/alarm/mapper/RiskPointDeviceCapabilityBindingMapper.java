package com.ghlzm.iot.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.alarm.entity.RiskPointDeviceCapabilityBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险点设备级正式绑定 Mapper。
 */
@Mapper
public interface RiskPointDeviceCapabilityBindingMapper extends BaseMapper<RiskPointDeviceCapabilityBinding> {

    @Override
    int insert(RiskPointDeviceCapabilityBinding entity);
}
